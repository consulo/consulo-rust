/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.flavors;

import consulo.annotation.component.ExtensionImpl;
import consulo.platform.Platform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@ExtensionImpl(id = "rust.unixToolchainFlavor")
public class RsUnixToolchainFlavor extends RsToolchainFlavor {
    @Override
    protected Stream<Path> getHomePathCandidates(Platform platform) {
        return Stream.of("/usr/local/bin", "/usr/bin")
            .map(path -> platform.fs().getPath(path))
            .filter(Files::isDirectory);
    }

    @Override
    protected boolean isApplicable(Platform platform) {
        return platform.os().isUnix();
    }
}
