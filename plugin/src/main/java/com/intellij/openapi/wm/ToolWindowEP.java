package com.intellij.openapi.wm;
/** IntelliJ-compat stub — extension-point descriptor for a ToolWindow. */
public final class ToolWindowEP {
    public String id;
    public String anchor;
    public String icon;
    public String factoryClass;
    public String conditionClass;
    public boolean canCloseContents;
    public boolean isSecondary;
    public boolean canWorkInDumbMode;
}
