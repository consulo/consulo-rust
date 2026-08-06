package com.intellij.structuralsearch;
import com.intellij.structuralsearch.plugin.ui.Configuration;
import consulo.virtualFileSystem.fileType.FileType;
/** IntelliJ-compat stub for the structural-search predefined-template factory. */
public final class PredefinedConfigurationUtil {
    private PredefinedConfigurationUtil() {}
    public static Configuration createConfiguration(String name, String refName, String pattern, String category, FileType fileType) {
        return new Configuration() { @Override public Configuration copy() { return this; } };
    }
    public static Configuration createSearchTemplateInfo(String name, String pattern, String category, FileType fileType) {
        return createConfiguration(name, name, pattern, category, fileType);
    }
}
