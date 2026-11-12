/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

import org.rust.cargo.toolchain.flavors.RsToolchainFlavor;
import consulo.process.cmd.GeneralCommandLine;
import consulo.util.io.FileUtil;
import org.rust.stdext.Utils;

import java.io.File;
import java.nio.file.Path;
import consulo.application.util.UserHomeFileUtil;
import consulo.platform.Platform;
import org.rust.stdext.PathUtil;

public class RsLocalToolchain extends RsToolchainBase {

    private final Platform myPlatform;

    public RsLocalToolchain(Path location) {
        this(location, Platform.current());
    }

    /**
     * A toolchain of {@code platform} - the local machine, or a remote one whose executables run there.
     */
    public RsLocalToolchain(Path location, Platform platform) {
        super(location);
        myPlatform = platform;
    }

    @Override
    public Platform getPlatform() {
        return myPlatform;
    }

    private boolean isLocal() {
        return myPlatform == Platform.current();
    }

    @Override
    public String getFileSeparator() {
        return isLocal() ? File.separator : myPlatform.fs().getSeparator();
    }

    @Override
    public int getExecutionTimeoutInMilliseconds() {
        return 1000;
    }

    @Override
    public GeneralCommandLine patchCommandLine(GeneralCommandLine commandLine, boolean withSudo) {
        // the tools of a remote toolchain run on its platform
        return isLocal() ? commandLine : commandLine.withPlatform(myPlatform);
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
        if (isLocal()) {
            return consulo.application.util.UserHomeFileUtil.expandUserHome(remotePath);
        }
        if (remotePath.equals("~") || remotePath.startsWith("~/")) {
            return myPlatform.user().homePath() + remotePath.substring(1);
        }
        return remotePath;
    }

    @Override
    public String getExecutableName(String toolName) {
        return myPlatform.os().isWindows() ? toolName + ".exe" : toolName;
    }

    @Override
    public Path pathToExecutable(String toolName) {
        Path inToolchain = getLocation().resolve(getExecutableName(toolName));
        if (org.rust.stdext.PathUtil.isExecutable(inToolchain)) {
            return inToolchain;
        }
        // a tool which is not part of the toolchain - rustup manages all toolchains and lives elsewhere - is looked up
        // where the flavors know of on the toolchain's machine
        Path found = RsToolchainFlavor.findExecutable(myPlatform, toolName);
        return found != null ? found : inToolchain;
    }

    @Override
    public boolean hasExecutable(String exec) {
        return org.rust.stdext.PathUtil.isExecutable(pathToExecutable(exec));
    }

    @Override
    public boolean hasCargoExecutable(String exec) {
        return org.rust.stdext.PathUtil.isExecutable(pathToCargoExecutable(exec));
    }
}
