/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.flavors;

import com.intellij.openapi.util.SystemInfo;
import org.rust.stdext.PathUtil;

import java.nio.file.Path;
import java.util.stream.Stream;

public class RsUnixToolchainFlavor extends RsToolchainFlavor {

    @Override
    protected Stream<Path> getHomePathCandidates() {
        return Stream.of("/usr/local/bin", "/usr/bin")
            .map(PathUtil::toPath)
            .filter(PathUtil::isDirectory);
    }

    @Override
    protected boolean isApplicable() {
        return SystemInfo.isUnix;
    }
}
