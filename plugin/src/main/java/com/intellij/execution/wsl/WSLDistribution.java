package com.intellij.execution.wsl;

import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import jakarta.annotation.Nullable;

/** WSL distribution handle. Path translation and command-line patching are no-ops. */
public class WSLDistribution {
    public WSLDistribution() {}
    public WSLDistribution(String id) {}
    public String getId() { return ""; }
    public String getMsId() { return ""; }
    public String getPresentableName() { return ""; }
    public String getUserHome() { return "/root"; }
    public String getMntRoot() { return "/mnt/"; }
    public String getUNCRootPath() { return "\\\\wsl$\\"; }
    @Nullable public String getWindowsPath(String linuxPath) { return null; }
    @Nullable public String getWslPath(String windowsPath) { return null; }
    public GeneralCommandLine patchCommandLine(GeneralCommandLine cl, Object project, WSLCommandLineOptions options) throws ExecutionException { return cl; }
}
