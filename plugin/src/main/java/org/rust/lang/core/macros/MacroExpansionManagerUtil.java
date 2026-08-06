/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.logging.Logger;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility methods for MacroExpansionManager.
 */
public final class MacroExpansionManagerUtil {
    private MacroExpansionManagerUtil() {
    }

    public static final Logger MACRO_LOG = Logger.getInstance(MacroExpansionManager.class);

    @Nonnull
    public static MacroExpansionManager getMacroExpansionManager(@Nonnull Project project) {
        return project.getService(MacroExpansionManager.class);
    }

    @Nonnull
    public static MacroExpansionManager getMacroExpansionManagerIfCreated(@Nonnull Project project) {
        MacroExpansionManager service = project.getInstance(MacroExpansionManager.class);
        if (service != null) return service;
        return getMacroExpansionManager(project);
    }

    @Nonnull
    public static Path getBaseMacroDir() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".intellij-rust", "macros");
    }

    @Nonnull
    public static String expansionNameToPath(@Nonnull String expansionName) {
        return expansionName.replace('.', '/') + ".rs";
    }
}
