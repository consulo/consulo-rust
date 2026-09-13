/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.module;

import com.intellij.openapi.module.ModuleType;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.icons.RsIcons;

import javax.swing.*;
import consulo.ui.image.Image;

public class RsModuleType extends ModuleType<RsModuleBuilder> {

    public static final String ID = "RUST_MODULE";

    public static final RsModuleType INSTANCE = new RsModuleType();

    public RsModuleType() {
        super(ID);
    }

    @Nonnull
    public consulo.ui.image.Image getNodeIcon(boolean isOpened) {
        return RsIcons.RUST;
    }

    @Nonnull
    public RsModuleBuilder createModuleBuilder() {
        return new RsModuleBuilder();
    }

    @Nonnull
    @Override
    public String getDescription() {
        return RsBundle.message("rust.module");
    }

    @Nonnull
    @Override
    public String getName() {
        return RsBundle.message("rust");
    }
}
