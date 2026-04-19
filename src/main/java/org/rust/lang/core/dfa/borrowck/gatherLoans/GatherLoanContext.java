/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa.borrowck.gatherLoans;

import org.jetbrains.annotations.NotNull;
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
    @NotNull
    private final BorrowCheckContext bccx;
    @NotNull
    private final MoveData moveData;
    @NotNull
    private final GatherMoveContext gmcx;

    public GatherLoanContext(@NotNull BorrowCheckContext bccx) {
        this(bccx, new MoveData());
    }

    public GatherLoanContext(@NotNull BorrowCheckContext bccx, @NotNull MoveData moveData) {
        this.bccx = bccx;
        this.moveData = moveData;
        this.gmcx = new GatherMoveContext(bccx, moveData);
    }

    @Override
    public void consume(@NotNull RsElement element, @NotNull Cmt cmt, @NotNull ConsumeMode mode) {
        if (mode instanceof ConsumeMode.Move) {
            gmcx.gatherMoveFromExpr(element, cmt, ((ConsumeMode.Move) mode).reason);
        }
    }

    @Override
    public void matchedPat(@NotNull RsPat pat, @NotNull Cmt cmt, @NotNull MatchMode mode) {
    }

    @Override
    public void consumePat(@NotNull RsPat pat, @NotNull Cmt cmt, @NotNull ConsumeMode mode) {
        if (mode instanceof ConsumeMode.Move) {
            gmcx.gatherMoveFromPat(pat, cmt);
        }
    }

    @Override
    public void declarationWithoutInit(@NotNull RsPatBinding binding) {
        gmcx.gatherDeclaration(binding, ExtensionsUtil.getType(binding));
    }

    @Override
    public void mutate(@NotNull RsElement assignmentElement, @NotNull Cmt assigneeCmt, @NotNull MutateMode mode) {
        guaranteeAssignmentValid(assignmentElement, assigneeCmt, mode);
    }

    @Override
    public void useElement(@NotNull RsElement element, @NotNull Cmt cmt) {
    }

    private void guaranteeAssignmentValid(@NotNull RsElement assignment, @NotNull Cmt cmt, @NotNull MutateMode mode) {
        LoanPath loanPath = LoanPath.computeFor(cmt);
        if (loanPath == null) return;
        gmcx.gatherAssignment(loanPath, assignment, cmt.element, mode);
    }

    @NotNull
    public MoveData check() {
        ExprUseWalker visitor = new ExprUseWalker(this, new MemoryCategorizationContext(bccx.getImplLookup(), bccx.getInference()));
        visitor.consumeBody(bccx.getBody());
        return moveData;
    }
}
