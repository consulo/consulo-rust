/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.container.plugin.PluginManager;
import com.intellij.util.system.CpuArch;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import consulo.container.boot.ContainerPathManager;
import consulo.platform.Platform;

public final class RsPathManager {
    public static final RsPathManager INSTANCE = new RsPathManager();
    public static final String INTELLIJ_RUST_NATIVE_HELPER = "intellij-rust-native-helper";

    private RsPathManager() {
    }

    @Nonnull
    public static Path prettyPrintersDir() {
        return pluginDir().resolve("prettyPrinters");
    }

    @Nonnull
    private static Path pluginDir() {
        File path = PluginManager.getPluginPath(RsPathManager.class);
        return Objects.requireNonNull(path, "Plugin must be never null").toPath();
    }

    @Nullable
    public static Path nativeHelper(boolean isWslToolchain) {
        String os;
        String binaryName;

        if (consulo.platform.Platform.current().os().isLinux() || isWslToolchain) {
            os = "linux";
            binaryName = INTELLIJ_RUST_NATIVE_HELPER;
        } else if (consulo.platform.Platform.current().os().isMac()) {
            os = "macos";
            binaryName = INTELLIJ_RUST_NATIVE_HELPER;
        } else if (consulo.platform.Platform.current().os().isWindows()) {
            os = "windows";
            binaryName = INTELLIJ_RUST_NATIVE_HELPER + ".exe";
        } else {
            return null;
        }

        String arch;
        if (CpuArch.isIntel64()) {
            arch = "x86-64";
        } else if (CpuArch.isArm64()) {
            arch = "arm64";
        } else {
            return null;
        }

        Path nativeHelperPath = pluginDir().resolve("bin/" + os + "/" + arch + "/" + binaryName);
        if (!Files.exists(nativeHelperPath)) return null;

        if (Files.isExecutable(nativeHelperPath) || nativeHelperPath.toFile().setExecutable(true)) {
            return nativeHelperPath;
        } else {
            return null;
        }
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
