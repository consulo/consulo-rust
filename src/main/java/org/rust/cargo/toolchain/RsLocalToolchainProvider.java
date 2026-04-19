/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

import com.intellij.execution.wsl.WslPath;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class RsLocalToolchainProvider implements RsToolchainProvider {
    @Override
    @Nullable
    public RsToolchainBase getToolchain(Path homePath) {
        if (SystemInfo.isWindows && WslPath.isWslUncPath(homePath.toString())) return null;
        return new RsLocalToolchain(homePath);
    }
}
