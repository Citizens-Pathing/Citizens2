package net.citizensnpcs;

import java.util.List;

import net.citizensnpcs.Settings.Setting;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.event.EntityTargetNPCEvent;
import net.citizensnpcs.api.event.NPCCombustByBlockEvent;
import net.citizensnpcs.api.event.NPCCombustByEntityEvent;
import net.citizensnpcs.api.event.NPCCombustEvent;
import net.citizensnpcs.api.event.NPCDamageByBlockEvent;
import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import net.citizensnpcs.api.event.NPCDamageEvent;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.event.PlayerCreateNPCEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.Owner;
import net.citizensnpcs.editor.Editor;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.trait.CurrentLocation;
import net.citizensnpcs.util.Messages;
import net.citizensnpcs.util.Messaging;
import net.citizensnpcs.util.NMS;
import net.minecraft.server.v1_4_6.EntityPlayer;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_4_6.CraftServer;
import org.bukkit.craftbukkit.v1_4_6.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class EventListen implements Listener {
    private final NPCRegistry npcRegistry = CitizensAPI.getNPCRegistry();
    private final ListMultimap<ChunkCoord, Integer> toRespawn = ArrayListMultimap.create();

    public EventListen() {
        instance = this; // TODO: remove singleton
    }

    /*
     * Chunk events
     */
    @EventHandler(ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        ChunkCoord coord = toCoord(event.getChunk());
        List<Integer> ids = toRespawn.get(coord);
        for (int i = 0; i < ids.size(); i++) {
            int id = ids.get(i);
            spawn(id);
            Messaging.debug("Spawned", id, "due to chunk load at [" + coord.x + "," + coord.z + "]");
        }
        toRespawn.removeAll(coord);
    }

    @EventHandler(ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        ChunkCoord coord = toCoord(event.getChunk());
        Location location = new Location(null, 0, 0, 0);
        for (NPC npc : npcRegistry) {
            if (!npc.isSpawned())
                continue;
            location = npc.getBukkitEntity().getLocation(location);
            boolean sameChunkCoordinates = coord.z == location.getBlockZ() >> 4
                    && coord.x == location.getBlockX() >> 4;
            if (sameChunkCoordinates && event.getWorld().equals(location.getWorld())) {
                npc.despawn(DespawnReason.CHUNK_UNLOAD);
                toRespawn.put(coord, npc.getId());
                Messaging.debug("Despawned", npc.getId(), "due to chunk unload at [" + coord.x + ","
                        + coord.z + "]");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangedWorld(EntityTeleportEvent event) {
        if (event.getFrom() == null || event.getTo() == null)
            return;
        if (event.getFrom().getWorld() == event.getTo().getWorld() || !npcRegistry.isNPC(event.getEntity()))
            return;
        NMS.updateNavigationWorld((LivingEntity) event.getEntity(), event.getTo().getWorld());
    }

    /*
     * Entity events
     */
    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {
        NPC npc = npcRegistry.getNPC(event.getEntity());
        if (npc == null)
            return;
        event.setCancelled(npc.data().get(NPC.DEFAULT_PROTECTED_METADATA, true));
        if (event instanceof EntityCombustByEntityEvent) {
            Bukkit.getPluginManager().callEvent(
                    new NPCCombustByEntityEvent((EntityCombustByEntityEvent) event, npc));
        } else if (event instanceof EntityCombustByBlockEvent) {
            Bukkit.getPluginManager().callEvent(
                    new NPCCombustByBlockEvent((EntityCombustByBlockEvent) event, npc));
        } else {
            Bukkit.getPluginManager().callEvent(new NPCCombustEvent(event, npc));
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        NPC npc = npcRegistry.getNPC(event.getEntity());
        if (npc == null)
            return;
        event.setCancelled(npc.data().get(NPC.DEFAULT_PROTECTED_METADATA, true));
        if (event instanceof EntityDamageByEntityEvent) {
            NPCDamageByEntityEvent damageEvent = new NPCDamageByEntityEvent(npc,
                    (EntityDamageByEntityEvent) event);
            Bukkit.getPluginManager().callEvent(damageEvent);

            if (!damageEvent.isCancelled() || !(damageEvent.getDamager() instanceof Player))
                return;
            Player damager = (Player) damageEvent.getDamager();

            // Call left-click event
            NPCLeftClickEvent leftClickEvent = new NPCLeftClickEvent(npc, damager);
            Bukkit.getPluginManager().callEvent(leftClickEvent);
        } else if (event instanceof EntityDamageByBlockEvent) {
            Bukkit.getPluginManager().callEvent(
                    new NPCDamageByBlockEvent(npc, (EntityDamageByBlockEvent) event));
        } else {
            Bukkit.getPluginManager().callEvent(new NPCDamageEvent(npc, event));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        NPC npc = npcRegistry.getNPC(event.getEntity());
        if (npc == null)
            return;
        Bukkit.getPluginManager().callEvent(new NPCDeathEvent(npc, event));
        npc.despawn(DespawnReason.DEATH);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntitySpawn(CreatureSpawnEvent event) {
        if (event.isCancelled() && npcRegistry.isNPC(event.getEntity()))
            event.setCancelled(false);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        NPC npc = npcRegistry.getNPC(event.getTarget());
        if (npc == null)
            return;
        event.setCancelled(npc.data().get(NPC.DEFAULT_PROTECTED_METADATA, true));
        Bukkit.getPluginManager().callEvent(new EntityTargetNPCEvent(event, npc));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        EntityPlayer handle = ((CraftPlayer) event.getPlayer()).getHandle();
        if (!(handle instanceof NPCHolder))
            return;
        ((CraftServer) Bukkit.getServer()).getHandle().players.remove(handle);
        // on teleport, player NPCs are added to the server player list. this is
        // undesirable as player NPCs are not real players and confuse plugins.
    }

    /*
     * Player events
     */

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCreateNPC(PlayerCreateNPCEvent event) {
        if (event.getCreator().hasPermission("citizens.admin.avoid-limits"))
            return;
        int limit = Setting.DEFAULT_NPC_LIMIT.asInt();
        int maxChecks = Setting.MAX_NPC_LIMIT_CHECKS.asInt();
        for (int i = maxChecks; i >= 0; i--) {
            if (!event.getCreator().hasPermission("citizens.npc.limit." + i))
                continue;
            limit = i;
            break;
        }
        if (limit < 0)
            return;
        int owned = 0;
        for (NPC npc : npcRegistry) {
            if (!event.getNPC().equals(npc) && npc.getTrait(Owner.class).isOwnedBy(event.getCreator()))
                owned++;
        }
        int wouldOwn = owned + 1;
        if (wouldOwn >= limit) {
            event.setCancelled(true);
            event.setCancelReason(Messaging.tr(Messages.OVER_NPC_LIMIT, limit));
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        NPC npc = npcRegistry.getNPC(event.getRightClicked());
        if (npc == null)
            return;

        Player player = event.getPlayer();

        // Call right-click event
        NPCRightClickEvent rightClickEvent = new NPCRightClickEvent(npc, player);
        Bukkit.getPluginManager().callEvent(rightClickEvent);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Editor.leave(event.getPlayer());
    }

    /*
     * World events
     */
    @EventHandler(ignoreCancelled = true)
    public void onWorldLoad(WorldLoadEvent event) {
        for (ChunkCoord chunk : toRespawn.keySet()) {
            if (!chunk.worldName.equals(event.getWorld().getName())
                    || !event.getWorld().isChunkLoaded(chunk.x, chunk.z))
                continue;
            List<Integer> ids = toRespawn.get(chunk);
            for (int i = 0; i < ids.size(); i++) {
                spawn(ids.get(i));
                Messaging
                        .debug("Spawned", ids.get(0), "due to world " + event.getWorld().getName() + " load");
            }
            toRespawn.removeAll(chunk);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        for (NPC npc : npcRegistry) {
            if (!npc.isSpawned() || !npc.getBukkitEntity().getWorld().equals(event.getWorld()))
                continue;
            storeForRespawn(npc);
            npc.despawn();
            Messaging.debug("Despawned", npc.getId() + "due to world unload at", event.getWorld().getName());
        }
    }

    private void spawn(int id) {
        NPC npc = npcRegistry.getById(id);
        if (npc == null)
            return;
        Location spawn = npc.getTrait(CurrentLocation.class).getLocation();
        if (spawn == null) {
            Messaging.debug("Couldn't find a spawn location for despawned NPC ID: " + id);
            return;
        }
        npc.spawn(spawn);
    }

    private void storeForRespawn(NPC npc) {
        toRespawn.put(toCoord(npc.getBukkitEntity().getLocation()), npc.getId());
    }

    private ChunkCoord toCoord(Chunk chunk) {
        return new ChunkCoord(chunk);
    }

    private ChunkCoord toCoord(Location loc) {
        return new ChunkCoord(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    private static class ChunkCoord {
        private final String worldName;
        private final int x;
        private final int z;

        private ChunkCoord(Chunk chunk) {
            this(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        }

        private ChunkCoord(String worldName, int x, int z) {
            this.x = x;
            this.z = z;
            this.worldName = worldName;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ChunkCoord other = (ChunkCoord) obj;
            if (worldName == null) {
                if (other.worldName != null) {
                    return false;
                }
            } else if (!worldName.equals(other.worldName)) {
                return false;
            }
            if (x != other.x || z != other.z) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = prime + ((worldName == null) ? 0 : worldName.hashCode());
            result = prime * result + x;
            result = prime * result + z;
            return result;
        }
    }

    private static EventListen instance;

    public static void addForRespawn(Location loc, int id) {
        if (instance == null)
            return;
        instance.toRespawn.put(instance.toCoord(loc), id);
    }
}