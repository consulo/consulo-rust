package com.intellij.openapi.application;
/** Registry of experimental feature flags; every feature reports as disabled. */
public final class Experiments {
    private static final Experiments INSTANCE = new Experiments();
    private Experiments() {}
    public static Experiments getInstance() { return INSTANCE; }
    public boolean isFeatureEnabled(String id) { return false; }
    public void setFeatureEnabled(String id, boolean enabled) {}
}
