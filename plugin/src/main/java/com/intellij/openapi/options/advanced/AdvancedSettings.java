package com.intellij.openapi.options.advanced;
/** IntelliJ-compat stub — source-compat only. */
public final class AdvancedSettings {
    private AdvancedSettings() {}
    public static AdvancedSettings getInstance() { return new AdvancedSettings(); }
    public static boolean getBoolean(String id) { return false; }
    public static boolean getBoolean(String id, boolean def) { return def; }
    public static int getInt(String id) { return 0; }
    public static int getInt(String id, int def) { return def; }
    public static String getString(String id) { return ""; }
    public static String getString(String id, String def) { return def; }
    public static void setBoolean(String id, boolean value) {}
    public static void setInt(String id, int value) {}
    public static void setString(String id, String value) {}
}
