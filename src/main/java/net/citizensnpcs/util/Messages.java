package net.citizensnpcs.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ListResourceBundle;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import com.google.common.io.Closeables;

public class Messages {
    public static final String AVAILABLE_WAYPOINT_PROVIDERS = "citizens.waypoints.available-providers-message";
    public static final String CITIZENS_DISABLED = "citizens.notifications.disabled";
    public static final String CITIZENS_ENABLED = "citizens.notifications.enabled";
    public static final String CITIZENS_IMPLEMENTATION_DISABLED = "citizens.changed-implementation";
    public static final String CITIZENS_INCOMPATIBLE = "citizens.notifications.incompatible-version";
    public static final String COMMAND_ID_NOT_FOUND = "citizens.commands.id-not-found";
    public static final String COMMAND_INVALID_MOB_TYPE = "citizens.commands.disallowed-mobtype";
    public static final String COMMAND_INVALID_NUMBER = "citizens.commands.invalid-number";
    public static final String COMMAND_MISSING_TRAIT = "citizens.commands.missing-required-trait";
    public static final String COMMAND_MUST_BE_INGAME = "citizens.commands.must-be-ingame";
    public static final String COMMAND_MUST_BE_OWNER = "citizens.commands.must-be-owner";
    public static final String COMMAND_MUST_HAVE_SELECTED = "citizens.commands.must-have-selected";
    public static final String COMMAND_REPORT_ERROR = "citizens.commands.console-error";
    public static final String CURRENT_WAYPOINT_PROVIDER = "citizens.waypoints.current-provider-message";
    public static final String DATABASE_CONNECTION_FAILED = "citizens.notifications.database-connection-failed";
    private static ResourceBundle defaultBundle;
    public static final String ERROR_INITALISING_SUB_PLUGIN = "citizens.sub-plugins.error-on-load";
    public static final String ERROR_LOADING_ECONOMY = "citizens.economy.error-loading";
    public static final String FAILED_LOAD_SAVES = "citizens.saves.load-failed";
    public static final String LOAD_NAME_NOT_FOUND = "citizens.notifications.npc-name-not-found";
    public static final String LOAD_TASK_NOT_SCHEDULED = "citizens.load-task-error";
    public static final String LOAD_UNKNOWN_NPC_TYPE = "citizens.notifications.unknown-npc-type";
    public static final String LOADING_SUB_PLUGIN = "citizens.sub-plugins.load";
    public static final String LOCALE_NOTIFICATION = "citizens.notifications.locale";
    public static final String METRICS_ERROR_NOTIFICATION = "citizens.notifications.metrics-load-error";
    public static final String METRICS_NOTIFICATION = "citizens.notifications.metrics-started";
    public static final String MINIMUM_COST_REQUIRED = "citizens.economy.minimum-cost-required-message";
    public static final String MONEY_WITHDRAWN = "citizens.economy.money-withdrawn";
    public static final String NUM_LOADED_NOTIFICATION = "citizens.notifications.npcs-loaded";
    public static final String OVER_NPC_LIMIT = "citizens.limits.over-npc-limit";
    public static final String SAVE_METHOD_SET_NOTIFICATION = "citizens.notifications.save-method-set";
    public static final String UNKNOWN_COMMAND = "citizens.commands.unknown-command";
    public static final String WAYPOINT_PROVIDER_SET = "citizens.waypoints.set-provider-message";
    public static final String WRITING_DEFAULT_SETTING = "citizens.settings.writing-default";
    public static final String EQUIPMENT_EDITOR_BEGIN = "citizens.editors.equipment.begin-message";
    public static final String EQUIPMENT_EDITOR_END = "citizens.editors.equipment.end-message";
    public static final String SKIPPING_BROKEN_TRAIT = "citizens.notifications.skipping-broken-trait";
    public static final String TRAIT_LOAD_FAILED = "citizens.notifications.trait-load-failed";
    public static final String EXCEPTION_UPDATING_NPC = "citizens.notifications.exception-updating-npc";
    public static final String EQUIPMENT_EDITOR_INVALID_BLOCK = "citizens.editors.equipment.invalid-block";
    public static final String EQUIPMENT_EDITOR_SHEEP_COLOURED = "citizens.editors.equipment.sheep-coloured";
    public static final String EQUIPMENT_EDITOR_ALL_ITEMS_REMOVED = "citizens.editors.equipment.all-items-removed";
    public static final String AGE_TRAIT_DESCRIPTION = "citizens.traits.age-description";
    public static final String SKIPPING_INVALID_POSE = "citizens.notifications.skipping-invalid-pose";
    public static final String TEXT_EDITOR_END = "citizens.editors.text.end-message";
    public static final String TEXT_EDITOR_BEGIN = "citizens.editors.text.begin-message";
    public static final String TEXT_EDITOR_ADDED_ENTRY = "citizens.editors.text.added-entry";
    public static final String TEXT_EDITOR_ADD_PROMPT = "citizens.editors.text.add-prompt";
    public static final String TEXT_EDITOR_EDIT_PROMPT = "citizens.editors.text.edit-prompt";
    public static final String TEXT_EDITOR_EDITED_TEXT = "citizens.editors.text.edited-text";
    public static final String TEXT_EDITOR_INVALID_INDEX = "citizens.editors.text.invalid-index";
    public static final String TEXT_EDITOR_INVALID_INPUT = "citizens.editors.text.invalid-input";
    public static final String TEXT_EDITOR_EDIT_BEGIN_PROMPT = "citizens.editors.text.edit-begin-prompt";

    private static Properties getDefaultBundleProperties() {
        Properties defaults = new Properties();
        InputStream in = null;
        try {
            in = Messages.class.getResourceAsStream("/" + Translator.PREFIX + "_en.properties");
            defaults.load(in);
        } catch (IOException e) {
        } finally {
            Closeables.closeQuietly(in);
        }
        return defaults;
    }

    public static ResourceBundle getDefaultResourceBundle(File resourceDirectory, String fileName) {
        if (defaultBundle == null) {
            resourceDirectory.mkdirs();

            File bundleFile = new File(resourceDirectory, fileName);
            if (!bundleFile.exists()) {
                try {
                    bundleFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            populateDefaults(bundleFile);
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(bundleFile);
                defaultBundle = new PropertyResourceBundle(stream);
            } catch (Exception e) {
                e.printStackTrace();
                defaultBundle = getFallbackResourceBundle();
            } finally {
                Closeables.closeQuietly(stream);
            }
        }
        return defaultBundle;
    }

    private static ResourceBundle getFallbackResourceBundle() {
        return new ListResourceBundle() {
            @Override
            protected Object[][] getContents() {
                return new Object[0][0];
            }
        };
    }

    private static void populateDefaults(File bundleFile) {
        Properties properties = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(bundleFile);
            properties.load(in);
        } catch (IOException e) {
        } finally {
            Closeables.closeQuietly(in);
        }
        Properties defaults = getDefaultBundleProperties();
        for (Entry<Object, Object> entry : defaults.entrySet()) {
            if (!properties.containsKey(entry.getKey()))
                properties.put(entry.getKey(), entry.getValue());
        }
        OutputStream stream = null;
        try {
            stream = new FileOutputStream(bundleFile);
            properties.store(stream, "");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Closeables.closeQuietly(stream);
        }
    }
}