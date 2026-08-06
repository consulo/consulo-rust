/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.icons;

import consulo.application.AllIcons;
import consulo.ui.image.Image;

public final class CargoIcons {
    public static final Image ICON = load("/icons/cargo.svg");
    public static final Image LOCK_ICON = load("/icons/cargoLock.svg");
    public static final Image BUILD_RS_ICON = load("/icons/rustBuild.svg");
    public static final Image TEST = AllIcons.RunConfigurations.Junit;
    public static final Image TEST_GREEN = AllIcons.RunConfigurations.TestPassed;
    public static final Image TEST_RED = AllIcons.RunConfigurations.TestFailed;

    // Icons for target nodes in cargo toolwindow
    public static final Image TARGETS = load("/icons/targets.svg");
    public static final Image BIN_TARGET = load("/icons/targetBin.svg");
    public static final Image LIB_TARGET = load("/icons/targetLib.svg");
    public static final Image TEST_TARGET = load("/icons/targetTest.svg");
    public static final Image BENCH_TARGET = load("/icons/targetBench.svg");
    public static final Image EXAMPLE_TARGET = load("/icons/targetExample.svg");
    public static final Image CUSTOM_BUILD_TARGET = load("/icons/targetCustomBuild.svg");

    public static final Image RELOAD_ICON = load("/icons/rustReload.svg");

    private CargoIcons() {
    }

    private static Image load(String path) {
        return Image.empty(16);
    }
}
