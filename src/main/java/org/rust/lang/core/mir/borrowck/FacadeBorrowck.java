/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.borrowck;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.dataflow.framework.BorrowCheckResults;
import org.rust.lang.core.mir.dataflow.framework.BorrowSet;
import org.rust.lang.core.mir.dataflow.framework.Results;
import org.rust.lang.core.mir.dataflow.framework.Utils;
import org.rust.lang.core.mir.dataflow.impls.Borrows;
import org.rust.lang.core.mir.dataflow.impls.MaybeUninitializedPlaces;
import org.rust.lang.core.mir.dataflow.move.MoveData;
import org.rust.lang.core.mir.schemas.MirBasicBlock;
import org.rust.lang.core.mir.schemas.MirBody;
import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsFunctionOrLambda;
import org.rust.lang.core.psi.ext.RsItemElement;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;

public final class FacadeBorrowck {
    private FacadeBorrowck() {
    }

    @NotNull
    public static MirBorrowCheckResult doMirBorrowCheck(@NotNull MirBody body) {
        MoveData moveData = MoveData.gatherMoves(body);
        boolean localsAreInvalidatedAtExit =
            PsiElementUtil.ancestorOrSelf(body.getSourceElement(), RsItemElement.class) instanceof RsFunctionOrLambda;
        BorrowSet borrowSet = BorrowSet.build(body, localsAreInvalidatedAtExit, moveData);

        Results<BitSet> borrows = new Borrows(borrowSet, Collections.emptyMap())
            .intoEngine(body)
            .iterateToFixPoint();
        Results<BitSet> uninitializedPlaces = new MaybeUninitializedPlaces(moveData)
            .intoEngine(body)
            .iterateToFixPoint();

        MirBorrowCheckVisitor visitor = new MirBorrowCheckVisitor(body, moveData, borrowSet, localsAreInvalidatedAtExit);
        BorrowCheckResults results = new BorrowCheckResults(uninitializedPlaces, borrows);
        List<MirBasicBlock> postOrder = Utils.getBasicBlocksInPostOrder(body);
        BorrowCheckResults.visitResults(results, postOrder, visitor);
        return visitor.getResult();
    }
}
