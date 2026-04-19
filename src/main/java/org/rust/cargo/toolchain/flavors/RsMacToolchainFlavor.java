/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.flavors;

import com.intellij.openapi.util.SystemInfo;
import java.nio.file.Files;
import org.rust.stdext.PathUtil;

import java.nio.file.Path;
import java.util.stream.Stream;

public class RsMacToolchainFlavor extends RsToolchainFlavor {

    @Override
    protected Stream<Path> getHomePathCandidates() {
        Path path = PathUtil.toPath("/usr/local/Cellar/rust/bin");
        if (Files.isDirectory(path)) {
            return Stream.of(path);
        } else {
            return Stream.empty();
        }
    }

    @Override
    protected boolean isApplicable() {
        return SystemInfo.isMac;
    }
}
