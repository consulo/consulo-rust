/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.wsl;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.wsl.WSLCommandLineOptions;
import com.intellij.execution.wsl.WSLDistribution;
import com.intellij.execution.wsl.WSLUtil;
import com.intellij.execution.wsl.WslPath;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.stdext.StdextUtil;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RsWslToolchain extends RsToolchainBase {

    @NotNull
    private final WslPath wslPath;
    @NotNull
    private final WSLDistribution distribution;
    @NotNull
    private final Path linuxPath;

    public RsWslToolchain(@NotNull WslPath wslPath) {
        super(StdextUtil.toPath(getWindowsPathWithFix(wslPath.getDistribution(), wslPath.getLinuxPath())));
        this.wslPath = wslPath;
        this.distribution = wslPath.getDistribution();
        this.linuxPath = StdextUtil.toPath(wslPath.getLinuxPath());
    }

    @NotNull
    public WslPath getWslPath() {
        return wslPath;
    }

    @NotNull
    @Override
    public String getFileSeparator() {
        return "/";
    }

    @Override
    public int getExecutionTimeoutInMilliseconds() {
        return 5000;
    }

    @NotNull
    @Override
    public GeneralCommandLine patchCommandLine(@NotNull GeneralCommandLine commandLine, boolean withSudo) {
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
        } catch (com.intellij.execution.ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public String toLocalPath(@NotNull String remotePath) {
        return getWindowsPathWithFix(distribution, remotePath);
    }

    @NotNull
    @Override
    public String toRemotePath(@NotNull String localPath) {
        String wslPathStr = distribution.getWslPath(localPath);
        return wslPathStr != null ? wslPathStr : localPath;
    }

    @NotNull
    @Override
    public String expandUserHome(@NotNull String remotePath) {
        return WslUtilsUtil.expandUserHome(distribution, remotePath);
    }

    @NotNull
    @Override
    public String getExecutableName(@NotNull String toolName) {
        return toolName;
    }

    @NotNull
    @Override
    public Path pathToExecutable(@NotNull String toolName) {
        return WslUtilsUtil.pathToExecutableOnWsl(linuxPath, toolName);
    }

    @Override
    public boolean hasExecutable(@NotNull String exec) {
        Path execPath = pathToExecutable(exec);
        return getWindowsPath(distribution, execPath).toFile().isFile();
    }

    @Override
    public boolean hasCargoExecutable(@NotNull String exec) {
        Path execPath = pathToCargoExecutable(exec);
        return getWindowsPath(distribution, execPath).toFile().isFile();
    }

    @NotNull
    private static String getWindowsPathWithFix(@NotNull WSLDistribution distribution, @NotNull String wslPathStr) {
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

    @NotNull
    private static Path getWindowsPath(@NotNull WSLDistribution distribution, @NotNull Path wslPath) {
        return StdextUtil.toPath(getWindowsPathWithFix(distribution, wslPath.toString()));
    }
}
