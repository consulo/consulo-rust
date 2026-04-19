/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.system.CpuArch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class RsPathManager {
    public static final RsPathManager INSTANCE = new RsPathManager();
    public static final String INTELLIJ_RUST_NATIVE_HELPER = "intellij-rust-native-helper";

    private RsPathManager() {
    }

    @NotNull
    public static Path prettyPrintersDir() {
        return pluginDir().resolve("prettyPrinters");
    }

    @NotNull
    private static Path pluginDir() {
        return OpenApiUtil.plugin().getPluginPath();
    }

    @Nullable
    public static Path nativeHelper(boolean isWslToolchain) {
        String os;
        String binaryName;

        if (SystemInfo.isLinux || isWslToolchain) {
            os = "linux";
            binaryName = INTELLIJ_RUST_NATIVE_HELPER;
        } else if (SystemInfo.isMac) {
            os = "macos";
            binaryName = INTELLIJ_RUST_NATIVE_HELPER;
        } else if (SystemInfo.isWindows) {
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

    @NotNull
    public static Path pluginDirInSystem() {
        return Paths.get(PathManager.getSystemPath()).resolve("intellij-rust");
    }

    @NotNull
    public static Path stdlibDependenciesDir() {
        return pluginDirInSystem().resolve("stdlib");
    }

    @NotNull
    public static Path tempPluginDirInSystem() {
        return Paths.get(PathManager.getTempPath()).resolve("intellij-rust");
    }
}
