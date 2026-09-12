/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import consulo.util.lang.SemVer;
import org.rust.stdext.Utils;

import java.nio.file.Path;
import consulo.platform.Platform;
import org.rust.stdext.PathUtil;

public final class ToolchainUtil {

    private ToolchainUtil() {
    }

    public static SemVer parseSemVer(String version) {
        SemVer result = SemVer.parseFromText(version);
        if (result == null) {
            throw new IllegalStateException("Invalid version value: " + version);
        }
        return result;
    }

    public static boolean hasExecutable(Path path, String toolName) {
        return org.rust.stdext.PathUtil.isExecutable(pathToExecutable(path, toolName));
    }

    public static Path pathToExecutable(Path path, String toolName) {
        String exeName = consulo.platform.Platform.current().os().isWindows() ? toolName + ".exe" : toolName;
        return path.resolve(exeName).toAbsolutePath();
    }
}
