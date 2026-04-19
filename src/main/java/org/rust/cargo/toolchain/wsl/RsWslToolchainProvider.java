/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.wsl;

import com.intellij.execution.wsl.WslPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.RsToolchainProvider;
import org.rust.ide.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;

import java.nio.file.Path;

public class RsWslToolchainProvider implements RsToolchainProvider {

    @Nullable
    @Override
    public RsToolchainBase getToolchain(@NotNull Path homePath) {
        if (!OpenApiUtil.isFeatureEnabled(RsExperiments.WSL_TOOLCHAIN)) return null;
        WslPath wslPath = WslPath.parseWindowsUncPath(homePath.toString());
        if (wslPath == null) return null;
        return new RsWslToolchain(wslPath);
    }
}
