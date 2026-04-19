/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class MacroExpansionMode {

    private MacroExpansionMode() {}

    public static final Disabled DISABLED = new Disabled();
    public static final Old OLD = new Old();
    public static final New NEW_ALL = new New(MacroExpansionScope.ALL);

    public static final class Disabled extends MacroExpansionMode {
        private Disabled() {}

        @Override
        public boolean equals(Object o) {
            return o instanceof Disabled;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "MacroExpansionMode.Disabled";
        }
    }

    public static final class Old extends MacroExpansionMode {
        private Old() {}

        @Override
        public boolean equals(Object o) {
            return o instanceof Old;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public String toString() {
            return "MacroExpansionMode.Old";
        }
    }

    public static final class New extends MacroExpansionMode {
        @NotNull
        private final MacroExpansionScope myScope;

        public New(@NotNull MacroExpansionScope scope) {
            myScope = scope;
        }

        @NotNull
        public MacroExpansionScope getScope() {
            return myScope;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof New)) return false;
            return myScope == ((New) o).myScope;
        }

        @Override
        public int hashCode() {
            return Objects.hash(2, myScope);
        }

        @Override
        public String toString() {
            return "MacroExpansionMode.New(scope=" + myScope + ")";
        }
    }
}
