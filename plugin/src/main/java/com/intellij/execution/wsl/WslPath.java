package com.intellij.execution.wsl;
import jakarta.annotation.Nullable;
/** IntelliJ-compat stub. Consulo doesn't bundle WSL support. */
public final class WslPath {
    private final WSLDistribution distribution;
    private final String linuxPath;
    public WslPath(String distribution, String linuxPath) {
        this.distribution = new WSLDistribution(distribution);
        this.linuxPath = linuxPath;
    }
    public WslPath(WSLDistribution distribution, String linuxPath) {
        this.distribution = distribution;
        this.linuxPath = linuxPath;
    }
    public WSLDistribution getDistribution() { return distribution; }
    public String getLinuxPath() { return linuxPath; }
    @Nullable public static WslPath parseWindowsUncPath(String path) { return null; }
    public static boolean isWslUncPath(String path) { return false; }
}
