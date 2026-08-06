/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import com.intellij.ui.dsl.builder.Cell;
import com.intellij.ui.dsl.builder.Row;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * Delegates to methods in {@link UiUtil}.
 */
public final class UiDslUtil {
    private UiDslUtil() {
    }

    @Nonnull
    public static <T extends JComponent> Cell<T> fullWidthCell(@Nonnull Row row, @Nonnull T component) {
        return UiUtil.fullWidthCell(row, component);
    }

    @Nonnull
    public static TextFieldWithBrowseButton pathTextField(
        @Nonnull FileChooserDescriptor fileChooserDescriptor,
        @Nonnull Disposable disposable,
        @Nonnull String title
    ) {
        return UiUtil.pathTextField(fileChooserDescriptor, disposable, title);
    }

    @Nonnull
    public static TextFieldWithBrowseButton pathTextField(
        @Nonnull FileChooserDescriptor fileChooserDescriptor,
        @Nonnull Disposable disposable,
        @Nonnull String title,
        @Nullable Runnable onTextChanged
    ) {
        return UiUtil.pathTextField(fileChooserDescriptor, disposable, title, onTextChanged);
    }
}
