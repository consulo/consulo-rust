/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.container.plugin.PluginManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class RsPathManager {
    public static final RsPathManager INSTANCE = new RsPathManager();

    private RsPathManager() {
    }

    @Nullable
    public static Path prettyPrintersDir() {
        Path pluginDir = pluginDir();
        return pluginDir == null ? null : pluginDir.resolve("prettyPrinters");
    }

    /// Null whenever this class was not loaded from an installed plugin - from a test classpath, say.
    /// Everything shipped inside the plugin directory is optional, so callers degrade rather than fail.
    @Nullable
    private static Path pluginDir() {
        File path = PluginManager.getPluginPath(RsPathManager.class);
        return path == null ? null : path.toPath();
    }

    @Nonnull
    public static Path pluginDirInSystem() {
        return Paths.get(consulo.container.boot.ContainerPathManager.get().getSystemPath()).resolve("intellij-rust");
    }

    @Nonnull
    public static Path stdlibDependenciesDir() {
        return pluginDirInSystem().resolve("stdlib");
    }

    @Nonnull
    public static Path tempPluginDirInSystem() {
        return Paths.get(System.getProperty("java.io.tmpdir")).resolve("intellij-rust");
    }
}
