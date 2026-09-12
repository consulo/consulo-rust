/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import org.rust.cargo.icons.CargoIcons;

/** Holds the icon shown on the Cargo project reload action. */
public class CargoExternalSystemIconProvider {
    @Nonnull
    public Image getReloadIcon() {
        return CargoIcons.RELOAD_ICON;
    }
}
