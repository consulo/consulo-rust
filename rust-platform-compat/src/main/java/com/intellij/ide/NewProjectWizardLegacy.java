package com.intellij.ide;
/** Availability flags for the legacy new-project wizard; it is always available and enabled. */
public final class NewProjectWizardLegacy {
    private NewProjectWizardLegacy() {}
    public static boolean isAvailable() { return true; }
    public static boolean isEnabled() { return true; }
}
