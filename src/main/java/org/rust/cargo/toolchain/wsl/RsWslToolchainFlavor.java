/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.wsl;

import com.intellij.execution.wsl.WSLDistribution;
import com.intellij.execution.wsl.WSLUtil;
import com.intellij.execution.wsl.WslDistributionManager;
import com.intellij.execution.wsl.WslPath;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.toolchain.flavors.RsToolchainFlavor;
import org.rust.ide.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class RsWslToolchainFlavor extends RsToolchainFlavor {

    @NotNull
    @Override
    protected Stream<Path> getHomePathCandidates() {
        List<WSLDistribution> distributions = compute(
            RsBundle.message("progress.title.getting.installed.distributions"),
            () -> WslDistributionManager.getInstance().getInstalledDistributions()
        );

        List<Path> candidates = new ArrayList<>();
        for (WSLDistribution distro : distributions) {
            candidates.addAll(getDistributionHomePathCandidates(distro));
        }
        return candidates.stream();
    }

    @Override
    protected boolean isApplicable() {
        return WSLUtil.isSystemCompatible() && OpenApiUtil.isFeatureEnabled(RsExperiments.WSL_TOOLCHAIN);
    }

    @Override
    protected boolean isValidToolchainPath(@NotNull Path path) {
        return WslPath.isWslUncPath(path.toString()) && super.isValidToolchainPath(path);
    }

    @Override
    protected boolean hasExecutable(@NotNull Path path, @NotNull String toolName) {
        return WslUtilsUtil.hasExecutableOnWsl(path, toolName);
    }

    @NotNull
    @Override
    protected Path pathToExecutable(@NotNull Path path, @NotNull String toolName) {
        return WslUtilsUtil.pathToExecutableOnWsl(path, toolName);
    }

    @NotNull
    public static List<Path> getDistributionHomePathCandidates(@NotNull WSLDistribution distribution) {
        @SuppressWarnings("UnstableApiUsage")
        Path root = distribution.getUNCRootPath();
        Map<String, String> environment = compute(
            RsBundle.message("progress.title.getting.environment.variables"),
            () -> distribution.getEnvironment()
        );

        List<Path> candidates = new ArrayList<>();
        if (environment != null) {
            String home = environment.get("HOME");
            String remoteCargoPath = home != null ? home + "/.cargo/bin" : null;
            Path localCargoPath = remoteCargoPath != null ? root.resolve(remoteCargoPath) : null;
            if (localCargoPath != null && localCargoPath.toFile().isDirectory()) {
                candidates.add(localCargoPath);
            }

            String sysPath = environment.get("PATH");
            if (sysPath != null) {
                for (String remotePath : sysPath.split(":")) {
                    if (remotePath.isEmpty()) continue;
                    Path localPath;
                    try {
                        localPath = root.resolve(remotePath);
                    } catch (Exception e) {
                        continue;
                    }
                    if (!localPath.toFile().isDirectory()) continue;
                    candidates.add(localPath);
                }
            }
        }

        for (String remotePath : Arrays.asList("/usr/local/bin", "/usr/bin")) {
            Path localPath = root.resolve(remotePath);
            if (!localPath.toFile().isDirectory()) continue;
            candidates.add(localPath);
        }

        return candidates;
    }

    @NotNull
    private static <T> T compute(@NotNull String title, @NotNull java.util.function.Supplier<T> getter) {
        if (OpenApiUtil.isDispatchThread()) {
            return OpenApiUtil.computeWithCancelableProgress(
                ProjectManager.getInstance().getDefaultProject(), title, getter::get
            );
        } else {
            return getter.get();
        }
    }
}
