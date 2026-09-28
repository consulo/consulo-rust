/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.ui.ExternalSystemIconProvider;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import org.rust.cargo.icons.CargoIcons;

@ExtensionImpl
public class CargoExternalSystemIconProvider implements ExternalSystemIconProvider {
    @Nonnull
    @Override
    public ProjectSystemId getSystemId() {
        return CargoExternalSystemProjectAware.CARGO_SYSTEM_ID;
    }

    @Nonnull
    @Override
    public Image getReloadIcon() {
        return CargoIcons.RELOAD_ICON;
    }
}
