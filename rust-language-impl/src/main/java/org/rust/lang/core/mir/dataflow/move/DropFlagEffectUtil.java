/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.move;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.MirBody;
import org.rust.lang.core.mir.schemas.MirLocal;
import org.rust.lang.core.mir.schemas.MirLocation;
import org.rust.lang.core.mir.schemas.MirPlace;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.impl.RsStructKind;
import org.rust.lang.core.psi.ext.impl.RsStructItemUtil;
import org.rust.lang.core.dfa.borrowck.gatherLoans.HasDestructorUtil;
import org.rust.lang.core.types.ty.*;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class DropFlagEffectUtil {
    private DropFlagEffectUtil() {
    }

    public static void dropFlagEffectsForLocation(
        @Nonnull MoveData moveData,
        @Nonnull MirLocation loc,
        @Nonnull BiConsumer<MovePath, DropFlagState> callback
    ) {
        List<MoveOut> moveOuts = moveData.getLocMap().get(loc);
        if (moveOuts != null) {
            for (MoveOut mi : moveOuts) {
                onAllChildrenBits(mi.getPath(), mpi -> callback.accept(mpi, DropFlagState.Absent));
            }
        }

        // TODO TerminatorKind::Drop

        forLocationInits(moveData, loc, it -> callback.accept(it, DropFlagState.Present));
    }

    public static void forLocationInits(
        @Nonnull MoveData moveData,
        @Nonnull MirLocation loc,
        @Nonnull Consumer<MovePath> callback
    ) {
        List<Init> inits = moveData.getInitLocMap().get(loc);
        if (inits == null) return;
        for (Init init : inits) {
            switch (init.getKind()) {
                case Deep:
                    onAllChildrenBits(init.getPath(), callback);
                    break;
                case Shallow:
                    callback.accept(init.getPath());
                    break;
                case NonPanicPathOnly:
                    break;
            }
        }
    }

    public static void onAllChildrenBits(
        @Nonnull MovePath movePath,
        @Nonnull Consumer<MovePath> eachChild
    ) {
        eachChild.accept(movePath);

        if (isTerminalPath(movePath)) return;

        MovePath nextChild = movePath.getFirstChild();
        while (nextChild != null) {
            onAllChildrenBits(nextChild, eachChild);
            nextChild = nextChild.getNextSibling();
        }
    }

    public static void dropFlagEffectsForFunctionEntry(
        @Nonnull MirBody body,
        @Nonnull MoveData moveData,
        @Nonnull BiConsumer<MovePath, DropFlagState> callback
    ) {
        for (MirLocal arg : body.getArgs()) {
            LookupResult lookupResult = moveData.getRevLookup().find(new MirPlace(arg));
            onLookupResultBits(lookupResult, it -> callback.accept(it, DropFlagState.Present));
        }
    }

    private static void onLookupResultBits(
        @Nonnull LookupResult lookupResult,
        @Nonnull Consumer<MovePath> eachChild
    ) {
        if (lookupResult instanceof LookupResult.Exact) {
            onAllChildrenBits(((LookupResult.Exact) lookupResult).getMovePath(), eachChild);
        }
        // LookupResult.Parent: access to untracked value - do not touch children
    }

    private static boolean isTerminalPath(@Nonnull MovePath movePath) {
        MirPlace place = movePath.getPlace();
        Ty ty = place.ty().getTy();
        if (ty instanceof TyAdt) {
            TyAdt tyAdt = (TyAdt) ty;
            boolean isUnion = (tyAdt.getItem() instanceof RsStructItem)
                && RsStructItemUtil.getKind((RsStructItem) tyAdt.getItem()) == RsStructKind.UNION;
            return (HasDestructorUtil.getHasDestructor(tyAdt.getItem()) && !TyUtil.isBox(tyAdt)) || isUnion;
        } else if (ty instanceof TySlice || ty instanceof TyReference || ty instanceof TyPointer) {
            return true;
        }
        return false;
    }
}
