/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.icons.RsIcons;

import javax.swing.*;

public final class RsFileType extends LanguageFileType {
    public static final RsFileType INSTANCE = new RsFileType();

    private RsFileType() {
        super(RsLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "Rust";
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return RsIcons.RUST_FILE;
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "rs";
    }

    @Nullable
    @Override
    public String getCharset(@NotNull VirtualFile file, @NotNull byte[] content) {
        return "UTF-8";
    }

    @NotNull
    @Override
    public String getDescription() {
        return RsBundle.message("label.rust.files");
    }
}
