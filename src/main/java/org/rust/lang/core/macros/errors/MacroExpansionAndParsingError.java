/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.errors;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.macros.MacroExpansionContext;

/**
 * Sealed class hierarchy for macro expansion and parsing errors.
 */
public abstract class MacroExpansionAndParsingError<E> {
    private MacroExpansionAndParsingError() {}

    public static final class ExpansionError<E> extends MacroExpansionAndParsingError<E> {
        private final E myError;

        public ExpansionError(@NotNull E error) {
            myError = error;
        }

        @NotNull
        public E getError() {
            return myError;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExpansionError<?> that = (ExpansionError<?>) o;
            return myError.equals(that.myError);
        }

        @Override
        public int hashCode() {
            return myError.hashCode();
        }

        @Override
        public String toString() {
            return "MacroExpansionAndParsingError.ExpansionError(error=" + myError + ")";
        }
    }

    public static final class ParsingError<E> extends MacroExpansionAndParsingError<E> {
        private final CharSequence myExpansionText;
        private final MacroExpansionContext myContext;

        public ParsingError(@NotNull CharSequence expansionText, @NotNull MacroExpansionContext context) {
            myExpansionText = expansionText;
            myContext = context;
        }

        @NotNull
        public CharSequence getExpansionText() {
            return myExpansionText;
        }

        @NotNull
        public MacroExpansionContext getContext() {
            return myContext;
        }

        @Override
        public String toString() {
            return "MacroExpansionAndParsingError.ParsingError(expansionText=" + myExpansionText +
                ", context=" + myContext + ")";
        }
    }

    @SuppressWarnings("rawtypes")
    public static final MacroExpansionAndParsingError MacroCallSyntaxError = new MacroExpansionAndParsingError() {
        @Override
        public String toString() {
            return "MacroExpansionAndParsingError.MacroCallSyntaxError";
        }
    };
}
