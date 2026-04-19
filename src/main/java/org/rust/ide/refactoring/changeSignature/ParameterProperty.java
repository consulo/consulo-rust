/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.changeSignature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElement;

public abstract class ParameterProperty<T extends RsElement> {
    @NotNull
    public String getText() {
        return "";
    }

    @Nullable
    public T getItem() {
        return null;
    }

    @NotNull
    public static <T extends RsElement> ParameterProperty<T> fromItem(@Nullable T item) {
        if (item == null) {
            return new Empty<>();
        }
        return new Valid<>(item);
    }

    @NotNull
    public static <T extends RsElement> ParameterProperty<T> fromText(@Nullable T item, @NotNull String text) {
        if (text.isBlank()) {
            return new Empty<>();
        }
        if (item == null) {
            return new Invalid<>(text);
        }
        return new Valid<>(item);
    }

    public static class Empty<T extends RsElement> extends ParameterProperty<T> {
    }

    public static class Invalid<T extends RsElement> extends ParameterProperty<T> {
        @NotNull
        private final String myText;

        public Invalid(@NotNull String text) {
            myText = text;
        }

        @NotNull
        @Override
        public String getText() {
            return myText;
        }
    }

    public static class Valid<T extends RsElement> extends ParameterProperty<T> {
        @NotNull
        private final T myItem;

        public Valid(@NotNull T item) {
            myItem = item;
        }

        @NotNull
        @Override
        public String getText() {
            return myItem.getText();
        }

        @NotNull
        @Override
        public T getItem() {
            return myItem;
        }
    }
}
