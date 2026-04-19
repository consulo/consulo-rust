/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.module;

import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.icons.RsIcons;

import javax.swing.*;

public class RsModuleType extends ModuleType<RsModuleBuilder> {

    public static final String ID = "RUST_MODULE";

    public static final RsModuleType INSTANCE = (RsModuleType) ModuleTypeManager.getInstance().findByID(ID);

    public RsModuleType() {
        super(ID);
    }

    @NotNull
    @Override
    public Icon getNodeIcon(boolean isOpened) {
        return RsIcons.RUST;
    }

    @NotNull
    @Override
    public RsModuleBuilder createModuleBuilder() {
        return new RsModuleBuilder();
    }

    @NotNull
    @Override
    public String getDescription() {
        return RsBundle.message("rust.module");
    }

    @NotNull
    @Override
    public String getName() {
        return RsBundle.message("rust");
    }
}
