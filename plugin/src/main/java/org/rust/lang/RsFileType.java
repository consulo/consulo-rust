/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang;

import consulo.language.file.LanguageFileType;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;

public final class RsFileType extends LanguageFileType {
    public static final RsFileType INSTANCE = new RsFileType();

    private RsFileType() {
        super(RsLanguage.INSTANCE);
    }

    @Nonnull
    @Override
    public String getId() {
        return "Rust";
    }

    @Nonnull
    @Override
    public Image getIcon() {
        // TODO: wire to RsIcons.RUST_FILE once org.rust.ide.icons.RsIcons compiles on
        // Consulo (needs IconLoader + LayeredIcon + AnimatedIcon replacements).
        return Image.empty(Image.DEFAULT_ICON_SIZE);
    }

    @Nonnull
    @Override
    public String getDefaultExtension() {
        return "rs";
    }

    @Nullable
    public String getCharset(@Nonnull VirtualFile file, @Nonnull byte[] content) {
        return "UTF-8";
    }

    @Nonnull
    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.of(RsBundle.message("label.rust.files"));
    }
}
