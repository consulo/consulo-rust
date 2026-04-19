/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.decl;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the value matched by a meta variable in a macro pattern.
 */
public abstract class MetaVarValue {

    private MetaVarValue() {}

    /**
     * A single matched fragment.
     */
    public static final class Fragment extends MetaVarValue {
        @NotNull
        private final String myValue;
        @Nullable
        private final FragmentKind myKind;
        @Nullable
        private final IElementType myElementType;
        private final int myOffsetInCallBody;

        public Fragment(@NotNull String value, @Nullable FragmentKind kind, @Nullable IElementType elementType, int offsetInCallBody) {
            myValue = value;
            myKind = kind;
            myElementType = elementType;
            myOffsetInCallBody = offsetInCallBody;
        }

        @NotNull
        public String getValue() {
            return myValue;
        }

        @Nullable
        public FragmentKind getKind() {
            return myKind;
        }

        @Nullable
        public IElementType getElementType() {
            return myElementType;
        }

        public int getOffsetInCallBody() {
            return myOffsetInCallBody;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Fragment)) return false;
            Fragment f = (Fragment) o;
            return myOffsetInCallBody == f.myOffsetInCallBody
                && Objects.equals(myValue, f.myValue)
                && myKind == f.myKind
                && Objects.equals(myElementType, f.myElementType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myValue, myKind, myElementType, myOffsetInCallBody);
        }

        @Override
        public String toString() {
            return "Fragment(value=" + myValue + ", kind=" + myKind + ", offset=" + myOffsetInCallBody + ")";
        }
    }

    /**
     * A group of nested meta var values (for repetitions).
     */
    public static final class Group extends MetaVarValue {
        @NotNull
        private final List<MetaVarValue> myNested;

        public Group() {
            myNested = new ArrayList<>();
        }

        public Group(@NotNull List<MetaVarValue> nested) {
            myNested = nested;
        }

        @NotNull
        public List<MetaVarValue> getNested() {
            return myNested;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Group)) return false;
            return Objects.equals(myNested, ((Group) o).myNested);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myNested);
        }

        @Override
        public String toString() {
            return "Group(nested=" + myNested + ")";
        }
    }

    /**
     * Represents an empty group (matched zero times).
     */
    public static final class EmptyGroup extends MetaVarValue {
        public static final EmptyGroup INSTANCE = new EmptyGroup();

        private EmptyGroup() {}

        @Override
        public String toString() {
            return "EmptyGroup";
        }
    }
}
