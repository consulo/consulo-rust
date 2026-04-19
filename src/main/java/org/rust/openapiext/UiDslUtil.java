/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.dsl.builder.Cell;
import com.intellij.ui.dsl.builder.Row;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Delegates to methods in {@link UiUtil}.
 */
public final class UiDslUtil {
    private UiDslUtil() {
    }

    @NotNull
    public static <T extends JComponent> Cell<T> fullWidthCell(@NotNull Row row, @NotNull T component) {
        return UiUtil.fullWidthCell(row, component);
    }

    @NotNull
    public static TextFieldWithBrowseButton pathTextField(
        @NotNull FileChooserDescriptor fileChooserDescriptor,
        @NotNull Disposable disposable,
        @NotNull String title
    ) {
        return UiUtil.pathTextField(fileChooserDescriptor, disposable, title);
    }

    @NotNull
    public static TextFieldWithBrowseButton pathTextField(
        @NotNull FileChooserDescriptor fileChooserDescriptor,
        @NotNull Disposable disposable,
        @NotNull String title,
        @Nullable Runnable onTextChanged
    ) {
        return UiUtil.pathTextField(fileChooserDescriptor, disposable, title, onTextChanged);
    }
}
