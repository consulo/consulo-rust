/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineValue;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner;
import org.rust.lang.core.resolve.ref.RsReference;

public abstract class InlineValueContext {
    @Nonnull
    private final RsNameIdentifierOwner myElement;
    @Nonnull
    private final RsExpr myExpr;
    @Nullable
    private final RsReference myReference;

    protected InlineValueContext(
        @Nonnull RsNameIdentifierOwner element,
        @Nonnull RsExpr expr,
        @Nullable RsReference reference
    ) {
        myElement = element;
        myExpr = expr;
        myReference = reference;
    }

    @Nonnull
    public RsNameIdentifierOwner getElement() {
        return myElement;
    }

    @Nonnull
    public RsExpr getExpr() {
        return myExpr;
    }

    @Nullable
    public RsReference getReference() {
        return myReference;
    }

    @Nonnull
    public String getType() {
        if (this instanceof Variable) return "variable";
        if (this instanceof Constant) return "constant";
        return "";
    }

    @Nonnull
    public String getName() {
        return myElement.getName() != null ? myElement.getName() : "";
    }

    public abstract void delete();

    public static class Constant extends InlineValueContext {
        public Constant(@Nonnull RsConstant constant, @Nonnull RsExpr expr, @Nullable RsReference reference) {
            super(constant, expr, reference);
        }

        @Override
        public void delete() {
            getElement().delete();
        }
    }

    public static class Variable extends InlineValueContext {
        @Nonnull
        private final RsLetDecl myDecl;

        public Variable(@Nonnull RsPatBinding variable, @Nonnull RsLetDecl decl, @Nonnull RsExpr expr, @Nullable RsReference reference) {
            super(variable, expr, reference);
            myDecl = decl;
        }

        @Override
        public void delete() {
            myDecl.delete();
        }
    }
}
