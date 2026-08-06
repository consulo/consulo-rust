/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa.borrowck.gatherLoans;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.dfa.ExprUseWalker;
import org.rust.lang.core.dfa.ExprUseWalker.*;
import org.rust.lang.core.dfa.MemoryCategorization.Cmt;
import org.rust.lang.core.dfa.MemoryCategorization.MemoryCategorizationContext;
import org.rust.lang.core.dfa.borrowck.BorrowChecker.BorrowCheckContext;
import org.rust.lang.core.dfa.borrowck.LoanPath;
import org.rust.lang.core.dfa.borrowck.MoveData;
import org.rust.lang.core.psi.RsPat;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.ExtensionsUtil;

public class GatherLoanContext implements ExprUseWalker.Delegate {
    @Nonnull
    private final BorrowCheckContext bccx;
    @Nonnull
    private final MoveData moveData;
    @Nonnull
    private final GatherMoveContext gmcx;

    public GatherLoanContext(@Nonnull BorrowCheckContext bccx) {
        this(bccx, new MoveData());
    }

    public GatherLoanContext(@Nonnull BorrowCheckContext bccx, @Nonnull MoveData moveData) {
        this.bccx = bccx;
        this.moveData = moveData;
        this.gmcx = new GatherMoveContext(bccx, moveData);
    }

    @Override
    public void consume(@Nonnull RsElement element, @Nonnull Cmt cmt, @Nonnull ConsumeMode mode) {
        if (mode instanceof ConsumeMode.Move) {
            gmcx.gatherMoveFromExpr(element, cmt, ((ConsumeMode.Move) mode).reason);
        }
    }

    @Override
    public void matchedPat(@Nonnull RsPat pat, @Nonnull Cmt cmt, @Nonnull MatchMode mode) {
    }

    @Override
    public void consumePat(@Nonnull RsPat pat, @Nonnull Cmt cmt, @Nonnull ConsumeMode mode) {
        if (mode instanceof ConsumeMode.Move) {
            gmcx.gatherMoveFromPat(pat, cmt);
        }
    }

    @Override
    public void declarationWithoutInit(@Nonnull RsPatBinding binding) {
        gmcx.gatherDeclaration(binding, ExtensionsUtil.getType(binding));
    }

    @Override
    public void mutate(@Nonnull RsElement assignmentElement, @Nonnull Cmt assigneeCmt, @Nonnull MutateMode mode) {
        guaranteeAssignmentValid(assignmentElement, assigneeCmt, mode);
    }

    @Override
    public void useElement(@Nonnull RsElement element, @Nonnull Cmt cmt) {
    }

    private void guaranteeAssignmentValid(@Nonnull RsElement assignment, @Nonnull Cmt cmt, @Nonnull MutateMode mode) {
        LoanPath loanPath = LoanPath.computeFor(cmt);
        if (loanPath == null) return;
        gmcx.gatherAssignment(loanPath, assignment, cmt.element, mode);
    }

    @Nonnull
    public MoveData check() {
        ExprUseWalker visitor = new ExprUseWalker(this, new MemoryCategorizationContext(bccx.getImplLookup(), bccx.getInference()));
        visitor.consumeBody(bccx.getBody());
        return moveData;
    }
}
