/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa.borrowck.gatherLoans;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.dfa.ExprUseWalker.MoveReason;
import org.rust.lang.core.dfa.ExprUseWalker.MutateMode;
import org.rust.lang.core.dfa.MemoryCategorization.Categorization;
import org.rust.lang.core.dfa.MemoryCategorization.Cmt;
import org.rust.lang.core.dfa.borrowck.*;
import org.rust.lang.core.psi.RsPat;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsPatIdent;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.ty.TySlice;
import org.rust.lang.core.types.ty.TyUtil;

import java.util.Objects;

public class GatherMoves {
    private GatherMoves() {
    }

    // TODO: use ImplLookup
    public static boolean hasDestructor(@Nonnull RsStructOrEnumItemElement element) {
        return false;
    }

    public static boolean isAdtWithDestructor(@Nonnull Ty ty) {
        return ty instanceof TyAdt && hasDestructor(((TyAdt) ty).getItem());
    }
}

class GatherMoveContext {
    @Nonnull
    private final BorrowChecker.BorrowCheckContext bccx;
    @Nonnull
    private final MoveData moveData;

    GatherMoveContext(@Nonnull BorrowChecker.BorrowCheckContext bccx, @Nonnull MoveData moveData) {
        this.bccx = bccx;
        this.moveData = moveData;
    }

    public void gatherDeclaration(@Nonnull RsPatBinding binding, @Nonnull Ty variableType) {
        LoanPath loanPath = new LoanPath(new LoanPathKind.Var(binding), variableType, binding);
        moveData.addMove(loanPath, binding, MoveKind.Declared);
    }

    public void gatherMoveFromExpr(@Nonnull RsElement element, @Nonnull Cmt cmt, @Nonnull MoveReason moveReason) {
        MoveKind kind;
        switch (moveReason) {
            case DirectRefMove:
            case PatBindingMove:
                kind = MoveKind.MoveExpr;
                break;
            case CaptureMove:
                kind = MoveKind.Captured;
                break;
            default:
                kind = MoveKind.MoveExpr;
        }
        GatherMoveInfo moveInfo = new GatherMoveInfo(element, kind, cmt, null);
        gatherMove(moveInfo);
    }

    public void gatherMoveFromPat(@Nonnull RsPat movePat, @Nonnull Cmt cmt) {
        MovePlace patMovePlace = null;
        if (movePat instanceof RsPatIdent) {
            patMovePlace = new MovePlace(((RsPatIdent) movePat).getPatBinding());
        }
        GatherMoveInfo moveInfo = new GatherMoveInfo(movePat, MoveKind.MovePat, cmt, patMovePlace);
        gatherMove(moveInfo);
    }

    private void gatherMove(@Nonnull GatherMoveInfo moveInfo) {
        Cmt move = getIllegalMoveOrigin(moveInfo.cmt);
        if (move != null) {
            bccx.reportMoveError(move);
        } else {
            LoanPath loanPath = LoanPath.computeFor(moveInfo.cmt);
            if (loanPath == null) return;
            moveData.addMove(loanPath, moveInfo.element, moveInfo.kind);
        }
    }

    public void gatherAssignment(@Nonnull LoanPath loanPath, @Nonnull RsElement assign,
                                 @Nonnull RsElement assignee, @Nonnull MutateMode mode) {
        moveData.addAssignment(loanPath, assign, assignee, mode);
    }

    @Nullable
    private Cmt getIllegalMoveOrigin(@Nonnull Cmt cmt) {
        Categorization category = cmt.category;
        if (category instanceof Categorization.Rvalue || category instanceof Categorization.Local) return null;
        if (category instanceof Categorization.Deref) {
            if (TyUtil.isBox(((Categorization.Deref) category).cmt.ty)) return null;
            return cmt;
        }
        if (category == Categorization.StaticItem) return cmt;
        if (category instanceof Categorization.Interior.Field || category instanceof Categorization.Interior.Pattern) {
            Cmt base = ((Categorization.Interior) category).getCmt();
            if (base.ty instanceof TyAdt) {
                return GatherMoves.hasDestructor(((TyAdt) base.ty).getItem()) ? cmt : getIllegalMoveOrigin(base);
            }
            if (base.ty instanceof TySlice) return cmt;
            return getIllegalMoveOrigin(base);
        }
        if (category instanceof Categorization.Interior.Index) return cmt;
        if (category instanceof Categorization.Downcast) {
            Cmt base = ((Categorization.Downcast) category).cmt;
            if (base.ty instanceof TyAdt) {
                return GatherMoves.hasDestructor(((TyAdt) base.ty).getItem()) ? cmt : getIllegalMoveOrigin(base);
            }
            if (base.ty instanceof TySlice) return cmt;
            return getIllegalMoveOrigin(base);
        }
        return null;
    }

    // ---- GatherMoveInfo ----
    static class GatherMoveInfo {
        @Nonnull
        final RsElement element;
        @Nonnull
        final MoveKind kind;
        @Nonnull
        final Cmt cmt;
        @Nullable
        final MovePlace movePlace;

        GatherMoveInfo(@Nonnull RsElement element, @Nonnull MoveKind kind, @Nonnull Cmt cmt, @Nullable MovePlace movePlace) {
            this.element = element;
            this.kind = kind;
            this.cmt = cmt;
            this.movePlace = movePlace;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GatherMoveInfo)) return false;
            GatherMoveInfo that = (GatherMoveInfo) o;
            return element.equals(that.element) && kind == that.kind && cmt.equals(that.cmt) && Objects.equals(movePlace, that.movePlace);
        }

        @Override
        public int hashCode() {
            return Objects.hash(element, kind, cmt, movePlace);
        }
    }
}
