/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

import consulo.annotation.component.ExtensionImpl;
import com.intellij.execution.wsl.WslPath;
import jakarta.annotation.Nullable;

import java.nio.file.Path;
import consulo.platform.Platform;

@ExtensionImpl
public class RsLocalToolchainProvider implements RsToolchainProvider {
    @Override
    @Nullable
    public RsToolchainBase getToolchain(Path homePath) {
        if (consulo.platform.Platform.current().os().isWindows() && WslPath.isWslUncPath(homePath.toString())) return null;
        return new RsLocalToolchain(homePath);
    }
}
