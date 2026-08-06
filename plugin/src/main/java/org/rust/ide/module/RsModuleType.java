/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.module;

import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.icons.RsIcons;

import javax.swing.*;

public class RsModuleType extends ModuleType<RsModuleBuilder> {

    public static final String ID = "RUST_MODULE";

    public static final RsModuleType INSTANCE = (RsModuleType) ModuleTypeManager.getInstance().findByID(ID);

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
