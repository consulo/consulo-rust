/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineValue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner;
import org.rust.lang.core.resolve.ref.RsReference;

public abstract class InlineValueContext {
    @NotNull
    private final RsNameIdentifierOwner myElement;
    @NotNull
    private final RsExpr myExpr;
    @Nullable
    private final RsReference myReference;

    protected InlineValueContext(
        @NotNull RsNameIdentifierOwner element,
        @NotNull RsExpr expr,
        @Nullable RsReference reference
    ) {
        myElement = element;
        myExpr = expr;
        myReference = reference;
    }

    @NotNull
    public RsNameIdentifierOwner getElement() {
        return myElement;
    }

    @NotNull
    public RsExpr getExpr() {
        return myExpr;
    }

    @Nullable
    public RsReference getReference() {
        return myReference;
    }

    @NotNull
    public String getType() {
        if (this instanceof Variable) return "variable";
        if (this instanceof Constant) return "constant";
        return "";
    }

    @NotNull
    public String getName() {
        return myElement.getName() != null ? myElement.getName() : "";
    }

    public abstract void delete();

    public static class Constant extends InlineValueContext {
        public Constant(@NotNull RsConstant constant, @NotNull RsExpr expr, @Nullable RsReference reference) {
            super(constant, expr, reference);
        }

        @Override
        public void delete() {
            getElement().delete();
        }
    }

    public static class Variable extends InlineValueContext {
        @NotNull
        private final RsLetDecl myDecl;

        public Variable(@NotNull RsPatBinding variable, @NotNull RsLetDecl decl, @NotNull RsExpr expr, @Nullable RsReference reference) {
            super(variable, expr, reference);
            myDecl = decl;
        }

        @Override
        public void delete() {
            myDecl.delete();
        }
    }
}
