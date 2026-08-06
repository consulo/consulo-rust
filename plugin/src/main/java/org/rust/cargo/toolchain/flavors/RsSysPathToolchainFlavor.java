/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.flavors;

import org.rust.stdext.PathUtil;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public class RsSysPathToolchainFlavor extends RsToolchainFlavor {
    @Override
    protected Stream<Path> getHomePathCandidates() {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isEmpty()) {
            return Stream.empty();
        }
        return Arrays.stream(pathEnv.split(File.pathSeparator))
            .filter(s -> !s.isEmpty())
            .map(PathUtil::toPathOrNull)
            .filter(Objects::nonNull)
            .filter(PathUtil::isDirectory);
    }
}
