/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.errors;

import jakarta.annotation.Nonnull;

import java.util.List;

public abstract class DeclMacroExpansionError extends MacroExpansionError {
    DeclMacroExpansionError() {}

    public static final class Matching extends DeclMacroExpansionError {
        private final List<MacroMatchingError> myErrors;

        public Matching(@Nonnull List<MacroMatchingError> errors) {
            myErrors = errors;
        }

        @Nonnull
        public List<MacroMatchingError> getErrors() {
            return myErrors;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Matching matching = (Matching) o;
            return myErrors.equals(matching.myErrors);
        }

        @Override
        public int hashCode() {
            return myErrors.hashCode();
        }

        @Override
        public String toString() {
            return "DeclMacroExpansionError.Matching(errors=" + myErrors + ")";
        }
    }

    public static final DeclMacroExpansionError DefSyntax = new DeclMacroExpansionError() {
        @Override
        public String toString() {
            return "DeclMacroExpansionError.DefSyntax";
        }
    };

    public static final DeclMacroExpansionError TooLargeExpansion = new DeclMacroExpansionError() {
        @Override
        public String toString() {
            return "DeclMacroExpansionError.TooLargeExpansion";
        }
    };
}
