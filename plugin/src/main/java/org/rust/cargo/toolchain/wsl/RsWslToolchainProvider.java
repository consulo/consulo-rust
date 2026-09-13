/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.wsl;

import consulo.annotation.component.ExtensionImpl;
import com.intellij.execution.wsl.WslPath;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.RsToolchainProvider;
import org.rust.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;

import java.nio.file.Path;

@ExtensionImpl
public class RsWslToolchainProvider implements RsToolchainProvider {

    @Nullable
    @Override
    public RsToolchainBase getToolchain(@Nonnull Path homePath) {
        if (!OpenApiUtil.isFeatureEnabled(RsExperiments.WSL_TOOLCHAIN)) return null;
        WslPath wslPath = WslPath.parseWindowsUncPath(homePath.toString());
        if (wslPath == null) return null;
        return new RsWslToolchain(wslPath);
    }
}
