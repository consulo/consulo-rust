/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import jakarta.annotation.Nonnull;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import jakarta.annotation.Nullable;

public abstract class CargoProjectActionBase extends LegacyDumbAwareAction {

    protected CargoProjectActionBase() {
    }

    protected CargoProjectActionBase(@Nonnull LocalizeValue text,
                     @Nonnull LocalizeValue description,
                     @Nullable Image icon) {
        super(text, description, icon);
    }

}
