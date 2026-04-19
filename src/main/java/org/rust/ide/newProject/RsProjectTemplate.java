/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject;

import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class RsProjectTemplate {

    @SuppressWarnings("UnstableApiUsage")
    @NlsContexts.ListItem
    @NotNull
    private final String name;
    private final boolean isBinary;
    @NotNull
    private final Icon icon;

    protected RsProjectTemplate(@NotNull String name, boolean isBinary, @NotNull Icon icon) {
        this.name = name;
        this.isBinary = isBinary;
        this.icon = icon;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public boolean isBinary() {
        return isBinary;
    }

    @NotNull
    public Icon getIcon() {
        return icon;
    }

    @Nls
    @Nullable
    public String validateProjectName(@NotNull String crateName) {
        return RsPackageNameValidator.validate(crateName, isBinary);
    }
}
