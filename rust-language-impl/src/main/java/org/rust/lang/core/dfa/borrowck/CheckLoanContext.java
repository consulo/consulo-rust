/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa.borrowck;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.dfa.ExprUseWalker;
import org.rust.lang.core.dfa.ExprUseWalker.*;
import org.rust.lang.core.dfa.MemoryCategorization.Cmt;
import org.rust.lang.core.dfa.MemoryCategorization.MemoryCategorizationContext;
import org.rust.lang.core.dfa.borrowck.gatherLoans.GatherMoves;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsPat;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsPatSlice;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;

public class CheckLoanContext implements ExprUseWalker.Delegate {
    @Nonnull
    private final BorrowChecker.BorrowCheckContext bccx;
    @Nonnull
    private final FlowedMoveData moveData;

    public CheckLoanContext(@Nonnull BorrowChecker.BorrowCheckContext bccx, @Nonnull FlowedMoveData moveData) {
        this.bccx = bccx;
        this.moveData = moveData;
    }

    @Override
    public void consume(@Nonnull RsElement element, @Nonnull Cmt cmt, @Nonnull ConsumeMode mode) {
        consumeCommon(element, cmt);
    }

    @Override
    public void matchedPat(@Nonnull RsPat pat, @Nonnull Cmt cmt, @Nonnull MatchMode mode) {
    }

    @Override
    public void consumePat(@Nonnull RsPat pat, @Nonnull Cmt cmt, @Nonnull ConsumeMode mode) {
        consumeCommon(pat, cmt);
    }

    private void checkIfPathIsMoved(@Nonnull RsElement element, @Nonnull LoanPath loanPath) {
        moveData.eachMoveOf(element, loanPath, (move, lp) -> {
            if (isInsideSliceDestructing(loanPath, move)) {
                return true;
            } else {
                bccx.reportUseOfMovedValue(loanPath, move);
                return false;
            }
        });
    }

    private boolean isInsideSliceDestructing(@Nonnull LoanPath loanPath, @Nonnull Move move) {
        return loanPath.element instanceof RsPatSlice &&
            RsElementUtil.ancestorOrSelf(move.element, RsPatSlice.class) == loanPath.element;
    }

    @Override
    public void declarationWithoutInit(@Nonnull RsPatBinding binding) {
    }

    @Override
    public void mutate(@Nonnull RsElement assignmentElement, @Nonnull Cmt assigneeCmt, @Nonnull MutateMode mode) {
        LoanPath loanPath = LoanPath.computeFor(assigneeCmt);
        if (loanPath == null) return;
        if (mode == MutateMode.Init || mode == MutateMode.JustWrite) {
            checkIfAssignedPathIsMoved(assigneeCmt.element, loanPath);
        } else if (mode == MutateMode.WriteAndRead) {
            checkIfPathIsMoved(assigneeCmt.element, loanPath);
        }
    }

    @Override
    public void useElement(@Nonnull RsElement element, @Nonnull Cmt cmt) {
    }

    public void checkLoans(@Nonnull RsBlock body) {
        MemoryCategorizationContext mc = new MemoryCategorizationContext(bccx.getImplLookup(), bccx.getInference());
        new ExprUseWalker(this, mc).consumeBody(body);
    }

    private void checkIfAssignedPathIsMoved(@Nonnull RsElement element, @Nonnull LoanPath loanPath) {
        if (loanPath.kind instanceof LoanPathKind.Downcast) {
            checkIfAssignedPathIsMoved(element, ((LoanPathKind.Downcast) loanPath.kind).loanPath);
        }
        if (!(loanPath.kind instanceof LoanPathKind.Extend)) return;
        LoanPathKind.Extend extend = (LoanPathKind.Extend) loanPath.kind;
        LoanPath baseLoanPath = extend.loanPath;
        LoanPathElement lpElement = extend.lpElement;

        if (lpElement instanceof LoanPathElement.Interior.Field) {
            if (GatherMoves.isAdtWithDestructor(baseLoanPath.ty)) {
                moveData.eachMoveOf(element, baseLoanPath, (m, lp) -> false);
            } else {
                checkIfAssignedPathIsMoved(element, baseLoanPath);
            }
        } else {
            checkIfPathIsMoved(element, baseLoanPath);
        }
    }

    private void consumeCommon(@Nonnull RsElement element, @Nonnull Cmt cmt) {
        LoanPath loanPath = LoanPath.computeFor(cmt);
        if (loanPath == null) return;
        checkIfPathIsMoved(element, loanPath);
    }
}
