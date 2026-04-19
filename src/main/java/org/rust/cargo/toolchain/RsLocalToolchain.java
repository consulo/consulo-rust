/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import org.rust.cargo.util.ToolchainUtil;
import org.rust.stdext.Utils;

import java.io.File;
import java.nio.file.Path;

public class RsLocalToolchain extends RsToolchainBase {

    public RsLocalToolchain(Path location) {
        super(location);
    }

    @Override
    public String getFileSeparator() {
        return File.separator;
    }

    @Override
    public int getExecutionTimeoutInMilliseconds() {
        return 1000;
    }

    @Override
    public GeneralCommandLine patchCommandLine(GeneralCommandLine commandLine, boolean withSudo) {
        return commandLine;
    }

    @Override
    public String toLocalPath(String remotePath) {
        return remotePath;
    }

    @Override
    public String toRemotePath(String localPath) {
        return localPath;
    }

    @Override
    public String expandUserHome(String remotePath) {
        return FileUtil.expandUserHome(remotePath);
    }

    @Override
    public String getExecutableName(String toolName) {
        return SystemInfo.isWindows ? toolName + ".exe" : toolName;
    }

    @Override
    public Path pathToExecutable(String toolName) {
        return ToolchainUtil.pathToExecutable(getLocation(), toolName);
    }

    @Override
    public boolean hasExecutable(String exec) {
        return ToolchainUtil.hasExecutable(getLocation(), exec);
    }

    @Override
    public boolean hasCargoExecutable(String exec) {
        return org.rust.stdext.PathUtil.isExecutable(pathToCargoExecutable(exec));
    }
}
