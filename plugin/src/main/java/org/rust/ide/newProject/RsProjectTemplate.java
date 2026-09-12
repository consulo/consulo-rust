/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject;



import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import consulo.ui.image.Image;

public abstract class RsProjectTemplate {

    @SuppressWarnings("UnstableApiUsage")
    
    @Nonnull
    private final String name;
    private final boolean isBinary;
    @Nonnull
    private final consulo.ui.image.Image icon;

    protected RsProjectTemplate(@Nonnull String name, boolean isBinary, @Nonnull consulo.ui.image.Image icon) {
        this.name = name;
        this.isBinary = isBinary;
        this.icon = icon;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public boolean isBinary() {
        return isBinary;
    }

    @Nonnull
    public consulo.ui.image.Image getIcon() {
        return icon;
    }

    
    @Nullable
    public String validateProjectName(@Nonnull String crateName) {
        return RsPackageNameValidator.validate(crateName, isBinary);
    }
}
