/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.cargo.project.workspace.orderEntry;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.setting.module.CustomOrderEntryTypeEditor;
import consulo.module.content.layer.orderEntry.CustomOrderEntry;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import org.rust.cargo.project.workspace.CargoLibrary;

import java.util.function.Consumer;

/**
 * Draws a Cargo library entry in the module classpath table: the crate name with its version, under
 * the icon of the thing it came from.
 */
@ExtensionImpl
public class CargoLibraryOrderEntryTypeEditor implements CustomOrderEntryTypeEditor<CargoLibraryOrderEntryModel> {
    @Nonnull
    @Override
    public Consumer<ColoredTextContainer> getRenderer(
        @Nonnull CustomOrderEntry<CargoLibraryOrderEntryModel> orderEntry,
        @Nonnull CargoLibraryOrderEntryModel model
    ) {
        return container -> {
            container.setIcon(model.getKind().getIcon());
            container.append(model.getPresentableName(), SimpleTextAttributes.SYNTHETIC_ATTRIBUTES);
        };
    }

    @Nonnull
    @Override
    public String getOrderTypeId() {
        return CargoLibraryOrderEntryType.ID;
    }
}
