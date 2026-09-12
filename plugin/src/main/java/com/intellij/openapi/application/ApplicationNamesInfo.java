package com.intellij.openapi.application;
/** Product name metadata of the running application. */
public final class ApplicationNamesInfo {
    private static final ApplicationNamesInfo INSTANCE = new ApplicationNamesInfo();
    private ApplicationNamesInfo() {}
    public static ApplicationNamesInfo getInstance() { return INSTANCE; }
    public String getFullProductName() { return "Consulo"; }
    public String getProductName() { return "Consulo"; }
    public String getEditionName() { return ""; }
    public String getLowercaseProductName() { return "consulo"; }
}
