package com.intellij.util.system;

import consulo.platform.CpuArchitecture;
import consulo.platform.Platform;

/** Queries the CPU architecture of the running JVM. */
public final class CpuArch {
    private CpuArch() {}
    public static boolean isIntel64() { return Platform.current().jvm().arch() == CpuArchitecture.X86_64; }
    public static boolean isArm64()   { return Platform.current().jvm().arch() == CpuArchitecture.AARCH64; }
    public static boolean isX86()     { return Platform.current().jvm().arch() == CpuArchitecture.X86; }
}
