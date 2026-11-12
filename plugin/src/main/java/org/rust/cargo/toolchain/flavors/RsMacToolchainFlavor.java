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

@ExtensionImpl(id = "rust.macToolchainFlavor")
public class RsMacToolchainFlavor extends RsToolchainFlavor {
    @Override
    protected Stream<Path> getHomePathCandidates(Platform platform) {
        Path path = platform.fs().getPath("/usr/local/Cellar/rust/bin");
        return Files.isDirectory(path) ? Stream.of(path) : Stream.empty();
    }

    @Override
    protected boolean isApplicable(Platform platform) {
        return platform.os().isMac();
    }
}
