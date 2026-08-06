/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.wsl;

import consulo.process.cmd.GeneralCommandLine;
import com.intellij.execution.wsl.WSLCommandLineOptions;
import com.intellij.execution.wsl.WSLDistribution;
import com.intellij.execution.wsl.WSLUtil;
import com.intellij.execution.wsl.WslPath;
import consulo.util.io.FileUtil;
import jakarta.annotation.Nonnull;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.stdext.StdextUtil;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RsWslToolchain extends RsToolchainBase {

    @Nonnull
    private final WslPath wslPath;
    @Nonnull
    private final WSLDistribution distribution;
    @Nonnull
    private final Path linuxPath;

    public RsWslToolchain(@Nonnull WslPath wslPath) {
        super(StdextUtil.toPath(getWindowsPathWithFix(wslPath.getDistribution(), wslPath.getLinuxPath())));
        this.wslPath = wslPath;
        this.distribution = wslPath.getDistribution();
        this.linuxPath = StdextUtil.toPath(wslPath.getLinuxPath());
    }

    @Nonnull
    public WslPath getWslPath() {
        return wslPath;
    }

    @Nonnull
    @Override
    public String getFileSeparator() {
        return "/";
    }

    @Override
    public int getExecutionTimeoutInMilliseconds() {
        return 5000;
    }

    @Nonnull
    @Override
    public GeneralCommandLine patchCommandLine(@Nonnull GeneralCommandLine commandLine, boolean withSudo) {
        commandLine.setExePath(toRemotePath(commandLine.getExePath()));

        List<String> parameters = new ArrayList<>(commandLine.getParametersList().getList());
        List<String> remoteParams = new ArrayList<>();
        for (String param : parameters) {
            remoteParams.add(toRemotePath(param));
        }
        commandLine.getParametersList().clearAll();
        commandLine.getParametersList().addAll(remoteParams);

        Map<String, String> env = commandLine.getEnvironment();
        for (Map.Entry<String, String> entry : new ArrayList<>(env.entrySet())) {
            String[] paths = entry.getValue().split(File.pathSeparator);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paths.length; i++) {
                if (i > 0) sb.append(":");
                sb.append(toRemotePath(paths[i]));
            }
            env.put(entry.getKey(), sb.toString());
        }

        File workDir = commandLine.getWorkDirectory();
        if (workDir != null) {
            if (workDir.getPath().startsWith(getFileSeparator())) {
                commandLine.setWorkDirectory(new File(toLocalPath(workDir.getPath())));
            }
        }

        String remoteWorkDir = null;
        if (commandLine.getWorkDirectory() != null) {
            remoteWorkDir = toRemotePath(commandLine.getWorkDirectory().getAbsolutePath());
        }

        String linuxPathStr = linuxPath.toString().replace('\\', '/');
        WSLCommandLineOptions options = new WSLCommandLineOptions()
            .setSudo(withSudo)
            .setRemoteWorkingDirectory(remoteWorkDir)
            .addInitCommand("export PATH=\"" + linuxPathStr + ":$PATH\"");
        try {
            return distribution.patchCommandLine(commandLine, null, options);
        } catch (consulo.process.ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    @Override
    public String toLocalPath(@Nonnull String remotePath) {
        return getWindowsPathWithFix(distribution, remotePath);
    }

    @Nonnull
    @Override
    public String toRemotePath(@Nonnull String localPath) {
        String wslPathStr = distribution.getWslPath(localPath);
        return wslPathStr != null ? wslPathStr : localPath;
    }

    @Nonnull
    @Override
    public String expandUserHome(@Nonnull String remotePath) {
        return WslUtilsUtil.expandUserHome(distribution, remotePath);
    }

    @Nonnull
    @Override
    public String getExecutableName(@Nonnull String toolName) {
        return toolName;
    }

    @Nonnull
    @Override
    public Path pathToExecutable(@Nonnull String toolName) {
        return WslUtilsUtil.pathToExecutableOnWsl(linuxPath, toolName);
    }

    @Override
    public boolean hasExecutable(@Nonnull String exec) {
        Path execPath = pathToExecutable(exec);
        return getWindowsPath(distribution, execPath).toFile().isFile();
    }

    @Override
    public boolean hasCargoExecutable(@Nonnull String exec) {
        Path execPath = pathToCargoExecutable(exec);
        return getWindowsPath(distribution, execPath).toFile().isFile();
    }

    @Nonnull
    private static String getWindowsPathWithFix(@Nonnull WSLDistribution distribution, @Nonnull String wslPathStr) {
        String systemIndependentPath = FileUtil.toSystemIndependentName(wslPathStr);
        @SuppressWarnings("UnstableApiUsage")
        String uncRoot = distribution.getUNCRootPath().toString().replace('\\', '/');
        String result;
        if (systemIndependentPath.startsWith(uncRoot) || !systemIndependentPath.startsWith("/")) {
            result = systemIndependentPath;
        } else if (systemIndependentPath.startsWith(distribution.getMntRoot())) {
            result = WSLUtil.getWindowsPath(systemIndependentPath, distribution.getMntRoot());
        } else {
            result = distribution.getWindowsPath(systemIndependentPath);
        }
        return result != null ? result : systemIndependentPath;
    }

    @Nonnull
    private static Path getWindowsPath(@Nonnull WSLDistribution distribution, @Nonnull Path wslPath) {
        return StdextUtil.toPath(getWindowsPathWithFix(distribution, wslPath.toString()));
    }
}
