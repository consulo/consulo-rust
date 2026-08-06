/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.intellij.util;

/** IntelliJ-compat stub. Consulo uses {@code consulo.platform.Platform}; these ID constants aren't meaningful there. */
public final class PlatformUtils {
    private PlatformUtils() {}

    public static final String IDEA_PREFIX = "idea";
    public static final String IDEA_CE_PREFIX = "Idea";
    public static final String CLION_PREFIX = "CLion";
    public static final String APPCODE_PREFIX = "AppCode";
    public static final String RIDER_PREFIX = "Rider";
    public static final String GOIDE_PREFIX = "GoLand";
    public static final String PYCHARM_PREFIX = "PyCharm";
    public static final String PYCHARM_CE_PREFIX = "PyCharmCore";

    public static boolean isIntelliJ() { return false; }
    public static boolean isCLion() { return false; }
    public static boolean isAppCode() { return false; }
    public static boolean isRider() { return false; }
    public static boolean isGoIde() { return false; }
    public static boolean isPyCharm() { return false; }
    public static boolean isIdeaUltimate() { return false; }
    public static boolean isRubyMine() { return false; }
    public static boolean isPyCharmPro() { return false; }
    public static String getPlatformPrefix() { return "Consulo"; }
}
