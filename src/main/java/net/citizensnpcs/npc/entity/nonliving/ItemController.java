package net.citizensnpcs.npc.entity.nonliving;

import net.citizensnpcs.api.event.NPCPushEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.npc.AbstractEntityController;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.Util;
import net.minecraft.server.v1_7_R2.EntityHuman;
import net.minecraft.server.v1_7_R2.EntityItem;
import net.minecraft.server.v1_7_R2.ItemStack;
import net.minecraft.server.v1_7_R2.World;
import net.minecraft.server.v1_7_R2.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R2.CraftServer;
import org.bukkit.craftbukkit.v1_7_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_7_R2.entity.CraftItem;
import org.bukkit.craftbukkit.v1_7_R2.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;

public class ItemController extends AbstractEntityController {
    public ItemController() {
        super(EntityItemNPC.class);
    }

    @Override
    protected Entity createEntity(Location at, NPC npc) {
        WorldServer ws = ((CraftWorld) at.getWorld()).getHandle();
        final EntityItemNPC handle = new EntityItemNPC(ws, npc, at.getX(), at.getY(), at.getZ(),
                CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.STONE)));
        return handle.getBukkitEntity();
    }

    @Override
    public Item getBukkitEntity() {
        return (Item) super.getBukkitEntity();
    }

    public static class EntityItemNPC extends EntityItem implements NPCHolder {
        private final CitizensNPC npc;

        public EntityItemNPC(World world) {
            super(world);
            this.npc = null;
        }

        public EntityItemNPC(World world, NPC npc, double x, double y, double z, ItemStack stack) {
            super(world, x, y, z, stack);
            this.npc = (CitizensNPC) npc;
        }

        @Override
        public void b_(EntityHuman entityhuman) {
            if (npc == null) {
                super.b_(entityhuman);
            }
        }

        @Override
        public void collide(net.minecraft.server.v1_7_R2.Entity entity) {
            // this method is called by both the entities involved - cancelling
            // it will not stop the NPC from moving.
            super.collide(entity);
            if (npc != null) {
                Util.callCollisionEvent(npc, entity.getBukkitEntity());
            }
        }

        @Override
        public void g(double x, double y, double z) {
            if (npc == null) {
                super.g(x, y, z);
                return;
            }
            if (NPCPushEvent.getHandlerList().getRegisteredListeners().length == 0) {
                if (!npc.data().get(NPC.DEFAULT_PROTECTED_METADATA, true))
                    super.g(x, y, z);
                return;
            }
            Vector vector = new Vector(x, y, z);
            NPCPushEvent event = Util.callPushEvent(npc, vector);
            if (!event.isCancelled()) {
                vector = event.getCollisionVector();
                super.g(vector.getX(), vector.getY(), vector.getZ());
            }
            // when another entity collides, this method is called to push the
            // NPC so we prevent it from doing anything if the event is
            // cancelled.
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (bukkitEntity == null && npc != null) {
                bukkitEntity = new ItemNPC(this);
            }
            return super.getBukkitEntity();
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        public void h() {
            if (npc != null) {
                npc.update();
            } else {
                super.h();
            }
        }
    }

    public static class ItemNPC extends CraftItem implements NPCHolder {
        private final CitizensNPC npc;

        public ItemNPC(EntityItemNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
            this.npc = entity.npc;
        }

        @Override
        public NPC getNPC() {
            return npc;
        }
    }
}