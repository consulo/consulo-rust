/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.macros.RsExpandedElement;

import java.util.Collections;
import java.util.List;

public abstract class MacroExpansion {
    private final RsFile myFile;

    protected MacroExpansion(@NotNull RsFile file) {
        myFile = file;
    }

    @NotNull
    public RsFile getFile() {
        return myFile;
    }

    @NotNull
    public abstract List<RsExpandedElement> getElements();

    public static class Expr extends MacroExpansion {
        private final RsExpr myExpr;

        public Expr(@NotNull RsFile file, @NotNull RsExpr expr) {
            super(file);
            myExpr = expr;
        }

        @NotNull
        public RsExpr getExpr() {
            return myExpr;
        }

        @NotNull
        @Override
        public List<RsExpandedElement> getElements() {
            return Collections.singletonList(myExpr);
        }
    }

    public static class Pat extends MacroExpansion {
        private final RsPat myPat;

        public Pat(@NotNull RsFile file, @NotNull RsPat pat) {
            super(file);
            myPat = pat;
        }

        @NotNull
        public RsPat getPat() {
            return myPat;
        }

        @NotNull
        @Override
        public List<RsExpandedElement> getElements() {
            return Collections.singletonList(myPat);
        }
    }

    public static class Type extends MacroExpansion {
        private final RsTypeReference myType;

        public Type(@NotNull RsFile file, @NotNull RsTypeReference type) {
            super(file);
            myType = type;
        }

        @NotNull
        public RsTypeReference getType() {
            return myType;
        }

        @NotNull
        @Override
        public List<RsExpandedElement> getElements() {
            return Collections.singletonList(myType);
        }
    }

    public static class MetaItemValue extends MacroExpansion {
        private final RsExpandedElement myValue;

        public MetaItemValue(@NotNull RsFile file, @NotNull RsExpandedElement value) {
            super(file);
            myValue = value;
        }

        @NotNull
        public RsExpandedElement getValue() {
            return myValue;
        }

        @NotNull
        @Override
        public List<RsExpandedElement> getElements() {
            return Collections.singletonList(myValue);
        }
    }

    /** Can contain items, macros and macro calls */
    public static class Items extends MacroExpansion {
        private final List<RsExpandedElement> myElements;

        public Items(@NotNull RsFile file, @NotNull List<RsExpandedElement> elements) {
            super(file);
            myElements = elements;
        }

        @NotNull
        @Override
        public List<RsExpandedElement> getElements() {
            return myElements;
        }
    }

    /** Can contain items, statements and a tail expr */
    public static class Stmts extends MacroExpansion {
        private final List<RsExpandedElement> myElements;

        public Stmts(@NotNull RsFile file, @NotNull List<RsExpandedElement> elements) {
            super(file);
            myElements = elements;
        }

        @NotNull
        @Override
        public List<RsExpandedElement> getElements() {
            return myElements;
        }
    }
}
