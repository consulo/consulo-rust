package com.intellij.execution.wsl;

import jakarta.annotation.Nullable;

public final class WSLUtil {
    private WSLUtil() {}
    public static boolean isSystemCompatible() { return false; }
    @Nullable public static String getWindowsPath(String wslPath, String mntRoot) { return null; }
}
