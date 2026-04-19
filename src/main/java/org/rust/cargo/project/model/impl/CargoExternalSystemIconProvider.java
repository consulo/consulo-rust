/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import com.intellij.openapi.externalSystem.ui.ExternalSystemIconProvider;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.icons.CargoIcons;

import javax.swing.*;

public class CargoExternalSystemIconProvider implements ExternalSystemIconProvider {

    @NotNull
    @Override
    public Icon getReloadIcon() {
        return CargoIcons.RELOAD_ICON;
    }
}
