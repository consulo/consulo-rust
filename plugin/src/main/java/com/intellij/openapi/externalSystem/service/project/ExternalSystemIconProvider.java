package com.intellij.openapi.externalSystem.service.project;
import javax.swing.Icon;
/** IntelliJ-compat stub. */
public interface ExternalSystemIconProvider {
    default Icon getReloadIcon() { return null; }
    default Icon getProjectIcon() { return null; }
    default Icon getTaskIcon() { return null; }
}
