/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.icons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public final class CargoIcons {
    public static final Icon ICON = load("/icons/cargo.svg");
    public static final Icon LOCK_ICON = load("/icons/cargoLock.svg");
    public static final Icon BUILD_RS_ICON = load("/icons/rustBuild.svg");
    public static final Icon TEST = AllIcons.RunConfigurations.TestState.Run;
    public static final Icon TEST_GREEN = AllIcons.RunConfigurations.TestState.Green2;
    public static final Icon TEST_RED = AllIcons.RunConfigurations.TestState.Red2;

    // Icons for target nodes in cargo toolwindow
    public static final Icon TARGETS = load("/icons/targets.svg");
    public static final Icon BIN_TARGET = load("/icons/targetBin.svg");
    public static final Icon LIB_TARGET = load("/icons/targetLib.svg");
    public static final Icon TEST_TARGET = load("/icons/targetTest.svg");
    public static final Icon BENCH_TARGET = load("/icons/targetBench.svg");
    public static final Icon EXAMPLE_TARGET = load("/icons/targetExample.svg");
    public static final Icon CUSTOM_BUILD_TARGET = load("/icons/targetCustomBuild.svg");

    public static final Icon RELOAD_ICON = load("/icons/rustReload.svg");

    private CargoIcons() {
    }

    private static Icon load(String path) {
        return IconLoader.getIcon(path, CargoIcons.class);
    }
}
