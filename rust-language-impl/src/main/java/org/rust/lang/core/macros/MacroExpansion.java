/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.macros.RsExpandedElement;

import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.impl.*;

public abstract class MacroExpansion {
    private final RsFile myFile;

    protected MacroExpansion(@Nonnull RsFile file) {
        myFile = file;
    }

    @Nonnull
    public RsFile getFile() {
        return myFile;
    }

    @Nonnull
    public abstract List<RsExpandedElement> getElements();

    public static class Expr extends MacroExpansion {
        private final RsExpr myExpr;

        public Expr(@Nonnull RsFile file, @Nonnull RsExpr expr) {
            super(file);
            myExpr = expr;
        }

        @Nonnull
        public RsExpr getExpr() {
            return myExpr;
        }

        @Nonnull
        @Override
        public List<RsExpandedElement> getElements() {
            return Collections.singletonList(myExpr);
        }
    }

    public static class Pat extends MacroExpansion {
        private final RsPat myPat;

        public Pat(@Nonnull RsFile file, @Nonnull RsPat pat) {
            super(file);
            myPat = pat;
        }

        @Nonnull
        public RsPat getPat() {
            return myPat;
        }

        @Nonnull
        @Override
        public List<RsExpandedElement> getElements() {
            return Collections.singletonList(myPat);
        }
    }

    public static class Type extends MacroExpansion {
        private final RsTypeReference myType;

        public Type(@Nonnull RsFile file, @Nonnull RsTypeReference type) {
            super(file);
            myType = type;
        }

        @Nonnull
        public RsTypeReference getType() {
            return myType;
        }

        @Nonnull
        @Override
        public List<RsExpandedElement> getElements() {
            return Collections.singletonList(myType);
        }
    }

    public static class MetaItemValue extends MacroExpansion {
        private final RsExpandedElement myValue;

        public MetaItemValue(@Nonnull RsFile file, @Nonnull RsExpandedElement value) {
            super(file);
            myValue = value;
        }

        @Nonnull
        public RsExpandedElement getValue() {
            return myValue;
        }

        @Nonnull
        @Override
        public List<RsExpandedElement> getElements() {
            return Collections.singletonList(myValue);
        }
    }

    /** Can contain items, macros and macro calls */
    public static class Items extends MacroExpansion {
        private final List<RsExpandedElement> myElements;

        public Items(@Nonnull RsFile file, @Nonnull List<RsExpandedElement> elements) {
            super(file);
            myElements = elements;
        }

        @Nonnull
        @Override
        public List<RsExpandedElement> getElements() {
            return myElements;
        }
    }

    /** Can contain items, statements and a tail expr */
    public static class Stmts extends MacroExpansion {
        private final List<RsExpandedElement> myElements;

        public Stmts(@Nonnull RsFile file, @Nonnull List<RsExpandedElement> elements) {
            super(file);
            myElements = elements;
        }

        @Nonnull
        @Override
        public List<RsExpandedElement> getElements() {
            return myElements;
        }
    }
}
