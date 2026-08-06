/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import org.rust.lang.core.psi.RsProcMacroKind;

public abstract class MacroCallBody {

    public RsProcMacroKind getKind() {
        if (this instanceof Attribute) {
            return RsProcMacroKind.ATTRIBUTE;
        } else if (this instanceof Derive) {
            return RsProcMacroKind.DERIVE;
        } else if (this instanceof FunctionLike) {
            return RsProcMacroKind.FUNCTION_LIKE;
        }
        throw new IllegalStateException("Unknown MacroCallBody type: " + getClass());
    }

    public static class FunctionLike extends MacroCallBody {
        private final String myText;

        public FunctionLike(String text) {
            myText = text;
        }

        public String getText() {
            return myText;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FunctionLike that = (FunctionLike) o;
            return myText.equals(that.myText);
        }

        @Override
        public int hashCode() {
            return myText.hashCode();
        }

        @Override
        public String toString() {
            return "FunctionLike(text=" + myText + ")";
        }
    }

    public static class Derive extends MacroCallBody {
        private final MappedText myItem;

        public Derive(MappedText item) {
            myItem = item;
        }

        public MappedText getItem() {
            return myItem;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Derive derive = (Derive) o;
            return myItem.equals(derive.myItem);
        }

        @Override
        public int hashCode() {
            return myItem.hashCode();
        }

        @Override
        public String toString() {
            return "Derive(item=" + myItem + ")";
        }
    }

    /**
     * An attribute procedural macro body consists of two parts: an {@code item} part and an {@code attr} part.
     *
     * <pre>{@code
     * #[foo(bar)]
     * fn baz() {}
     * }</pre>
     *
     * In this example, {@code bar} is an {@code attr} part and {@code fn baz() {}} is an {@code item} part.
     */
    public static class Attribute extends MacroCallBody {
        private final MappedText myItem;
        private final MappedText myAttr;
        private final boolean myFixupRustSyntaxErrors;

        public Attribute(MappedText item, MappedText attr, boolean fixupRustSyntaxErrors) {
            myItem = item;
            myAttr = attr;
            myFixupRustSyntaxErrors = fixupRustSyntaxErrors;
        }

        public MappedText getItem() {
            return myItem;
        }

        public MappedText getAttr() {
            return myAttr;
        }

        public boolean isFixupRustSyntaxErrors() {
            return myFixupRustSyntaxErrors;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Attribute attribute = (Attribute) o;
            return myFixupRustSyntaxErrors == attribute.myFixupRustSyntaxErrors
                && myItem.equals(attribute.myItem)
                && myAttr.equals(attribute.myAttr);
        }

        @Override
        public int hashCode() {
            int result = myItem.hashCode();
            result = 31 * result + myAttr.hashCode();
            result = 31 * result + Boolean.hashCode(myFixupRustSyntaxErrors);
            return result;
        }

        @Override
        public String toString() {
            return "Attribute(item=" + myItem + ", attr=" + myAttr + ", fixupRustSyntaxErrors=" + myFixupRustSyntaxErrors + ")";
        }
    }
}
