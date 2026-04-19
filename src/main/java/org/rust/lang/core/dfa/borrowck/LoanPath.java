/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa.borrowck;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.dfa.MemoryCategorization;
import org.rust.lang.core.dfa.MemoryCategorization.*;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.regions.Scope;
import org.rust.lang.core.dfa.borrowck.BorrowChecker.BorrowCheckContext;
import org.rust.lang.core.types.ty.Ty;

import java.util.Objects;

public class LoanPath {
    @NotNull
    public final LoanPathKind kind;
    @NotNull
    public final Ty ty;
    @NotNull
    public final RsElement element;

    public LoanPath(@NotNull LoanPathKind kind, @NotNull Ty ty, @NotNull RsElement element) {
        this.kind = kind;
        this.ty = ty;
        this.element = element;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LoanPath)) return false;
        return Objects.equals(this.kind, ((LoanPath) other).kind);
    }

    @Override
    public int hashCode() {
        return kind.hashCode();
    }

    @NotNull
    public Scope killScope(@NotNull BorrowCheckContext bccx) {
        if (kind instanceof LoanPathKind.Var) {
            RsElement variable = ((LoanPathKind.Var) kind).declaration;
            if (variable instanceof RsPatBinding) {
                Scope scope = bccx.getCfg().regionScopeTree.getVariableScope((RsPatBinding) variable);
                return scope != null ? scope : new Scope.Node(variable);
            }
            return new Scope.Node(variable);
        }
        if (kind instanceof LoanPathKind.Downcast) {
            return ((LoanPathKind.Downcast) kind).loanPath.killScope(bccx);
        }
        if (kind instanceof LoanPathKind.Extend) {
            return ((LoanPathKind.Extend) kind).loanPath.killScope(bccx);
        }
        throw new IllegalStateException();
    }

    @Nullable
    public static LoanPath computeFor(@NotNull Cmt cmt) {
        Categorization category = cmt.category;
        if (category instanceof Categorization.Rvalue || category == Categorization.StaticItem) {
            return null;
        }
        if (category instanceof Categorization.Local) {
            return new LoanPath(new LoanPathKind.Var(((Categorization.Local) category).declaration), cmt.ty, cmt.element);
        }
        if (category instanceof Categorization.Deref) {
            Categorization.Deref deref = (Categorization.Deref) category;
            LoanPath baseLp = computeFor(deref.cmt);
            if (baseLp == null) return null;
            return new LoanPath(
                new LoanPathKind.Extend(baseLp, cmt.mutabilityCategory, new LoanPathElement.Deref(deref.pointerKind)),
                cmt.ty, cmt.element
            );
        }
        if (category instanceof Categorization.Interior) {
            Categorization.Interior interior = (Categorization.Interior) category;
            Cmt baseCmt = interior.getCmt();
            LoanPath baseLp = computeFor(baseCmt);
            if (baseLp == null) return null;
            RsElement interiorElement = baseCmt.category instanceof Categorization.Downcast
                ? ((Categorization.Downcast) baseCmt.category).element : null;
            LoanPathElement.Interior lpElement = LoanPathElement.Interior.fromCategory(interior, interiorElement);
            return new LoanPath(new LoanPathKind.Extend(baseLp, cmt.mutabilityCategory, lpElement), cmt.ty, cmt.element);
        }
        if (category instanceof Categorization.Downcast) {
            Categorization.Downcast downcast = (Categorization.Downcast) category;
            LoanPath baseLp = computeFor(downcast.cmt);
            if (baseLp == null) return null;
            return new LoanPath(new LoanPathKind.Downcast(baseLp, downcast.element), cmt.ty, cmt.element);
        }
        return null;
    }
}

// ---- LoanPathKind ----

// ---- LoanPathElement ----
