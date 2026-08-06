package com.intellij.openapi.application;
/** IntelliJ-compat stub — experiments feature flags. Consulo has no equivalent; always returns false. */
public final class Experiments {
    private static final Experiments INSTANCE = new Experiments();
    private Experiments() {}
    public static Experiments getInstance() { return INSTANCE; }
    public boolean isFeatureEnabled(String id) { return false; }
    public void setFeatureEnabled(String id, boolean enabled) {}
}
