/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import org.rust.cargo.icons.CargoIcons;

/** ExternalSystemIconProvider is IntelliJ-only — this class is kept as a stand-alone icon holder. */
public class CargoExternalSystemIconProvider {
    @Nonnull
    public Image getReloadIcon() {
        return CargoIcons.RELOAD_ICON;
    }
}
