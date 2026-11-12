/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.flavors;

import consulo.annotation.component.ExtensionImpl;
import consulo.platform.Platform;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

@ExtensionImpl(id = "rust.sysPathToolchainFlavor")
public class RsSysPathToolchainFlavor extends RsToolchainFlavor {
    @Override
    protected Stream<Path> getHomePathCandidates(Platform platform) {
        String pathEnv = platform.os().getEnvironmentVariable("PATH");
        if (pathEnv == null || pathEnv.isEmpty()) {
            return Stream.empty();
        }
        String separator = platform.os().isWindows() ? ";" : ":";
        return Arrays.stream(pathEnv.split(separator))
            .filter(s -> !s.isEmpty())
            .<Path>mapMulti((entry, consumer) -> {
                try {
                    consumer.accept(platform.fs().getPath(entry));
                }
                catch (InvalidPathException ignored) {
                }
            })
            .filter(Files::isDirectory);
    }
}
