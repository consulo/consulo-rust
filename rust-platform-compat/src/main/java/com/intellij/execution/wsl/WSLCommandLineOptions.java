package com.intellij.execution.wsl;

import java.util.ArrayList;
import java.util.List;

/** Options describing WSL-specific patching of a command line. */
public final class WSLCommandLineOptions {
    private boolean sudo;
    private String remoteWorkingDirectory;
    private final List<String> initCommands = new ArrayList<>();
    private boolean executeCommandInShell = true;
    private boolean launchWithWslExe = true;

    public WSLCommandLineOptions() {}
    public WSLCommandLineOptions setSudo(boolean sudo) { this.sudo = sudo; return this; }
    public WSLCommandLineOptions setRemoteWorkingDirectory(String dir) { this.remoteWorkingDirectory = dir; return this; }
    public WSLCommandLineOptions addInitCommand(String cmd) { this.initCommands.add(cmd); return this; }
    public WSLCommandLineOptions setExecuteCommandInShell(boolean v) { this.executeCommandInShell = v; return this; }
    public WSLCommandLineOptions setLaunchWithWslExe(boolean v) { this.launchWithWslExe = v; return this; }

    public boolean isSudo() { return sudo; }
    public String getRemoteWorkingDirectory() { return remoteWorkingDirectory; }
    public List<String> getInitCommands() { return initCommands; }
    public boolean isExecuteCommandInShell() { return executeCommandInShell; }
    public boolean isLaunchWithWslExe() { return launchWithWslExe; }
}
