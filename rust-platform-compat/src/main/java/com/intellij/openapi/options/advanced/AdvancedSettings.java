package com.intellij.openapi.options.advanced;

import consulo.application.ApplicationPropertiesComponent;

/**
 * Application-wide named settings, persisted in the shared application property store.
 * Every getter takes the value the setting falls back to when it has never been written.
 */
public final class AdvancedSettings {
    private static final AdvancedSettings INSTANCE = new AdvancedSettings();

    private AdvancedSettings() {
    }

    public static AdvancedSettings getInstance() {
        return INSTANCE;
    }

    private static ApplicationPropertiesComponent store() {
        return ApplicationPropertiesComponent.getInstance();
    }

    public static boolean getBoolean(String id, boolean defaultValue) {
        return store().getBoolean(id, defaultValue);
    }

    public static int getInt(String id, int defaultValue) {
        return store().getInt(id, defaultValue);
    }

    public static String getString(String id, String defaultValue) {
        return store().getValue(id, defaultValue);
    }

    public static void setBoolean(String id, boolean value) {
        store().setValue(id, String.valueOf(value));
    }

    public static void setInt(String id, int value) {
        store().setValue(id, String.valueOf(value));
    }

    public static void setString(String id, String value) {
        store().setValue(id, value);
    }
}
