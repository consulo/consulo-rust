/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.changeSignature;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsElement;

public abstract class ParameterProperty<T extends RsElement> {
    @Nonnull
    public String getText() {
        return "";
    }

    @Nullable
    public T getItem() {
        return null;
    }

    @Nonnull
    public static <T extends RsElement> ParameterProperty<T> fromItem(@Nullable T item) {
        if (item == null) {
            return new Empty<>();
        }
        return new Valid<>(item);
    }

    @Nonnull
    public static <T extends RsElement> ParameterProperty<T> fromText(@Nullable T item, @Nonnull String text) {
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
        @Nonnull
        private final String myText;

        public Invalid(@Nonnull String text) {
            myText = text;
        }

        @Nonnull
        @Override
        public String getText() {
            return myText;
        }
    }

    public static class Valid<T extends RsElement> extends ParameterProperty<T> {
        @Nonnull
        private final T myItem;

        public Valid(@Nonnull T item) {
            myItem = item;
        }

        @Nonnull
        @Override
        public String getText() {
            return myItem.getText();
        }

        @Nonnull
        @Override
        public T getItem() {
            return myItem;
        }
    }
}
