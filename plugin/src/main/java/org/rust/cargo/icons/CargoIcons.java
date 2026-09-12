/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.icons;

import consulo.application.AllIcons;
import consulo.rust.icon.RustIconGroup;
import consulo.ui.image.Image;

/**
 * @deprecated use {@link RustIconGroup} directly.
 */
@Deprecated
public final class CargoIcons {
    public static final Image ICON = RustIconGroup.cargo();
    public static final Image LOCK_ICON = RustIconGroup.cargolock();
    public static final Image BUILD_RS_ICON = RustIconGroup.rustbuild();
    public static final Image TEST = AllIcons.RunConfigurations.Junit;
    public static final Image TEST_GREEN = AllIcons.RunConfigurations.TestPassed;
    public static final Image TEST_RED = AllIcons.RunConfigurations.TestFailed;

    public static final Image TARGETS = RustIconGroup.targets();
    public static final Image BIN_TARGET = RustIconGroup.targetbin();
    public static final Image LIB_TARGET = RustIconGroup.targetlib();
    public static final Image TEST_TARGET = RustIconGroup.targettest();
    public static final Image BENCH_TARGET = RustIconGroup.targetbench();
    public static final Image EXAMPLE_TARGET = RustIconGroup.targetexample();
    public static final Image CUSTOM_BUILD_TARGET = RustIconGroup.targetcustombuild();

    public static final Image RELOAD_ICON = RustIconGroup.rustreload();

    private CargoIcons() {
    }
}
