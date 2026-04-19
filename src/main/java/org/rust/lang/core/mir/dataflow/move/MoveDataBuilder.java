/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.move;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.schemas.*;
import org.rust.lang.core.mir.util.IndexAlloc;
import org.rust.lang.core.mir.util.IndexKeyMap;
import org.rust.lang.core.mir.util.LocationMap;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.RsStructKind;
import org.rust.lang.core.psi.ext.RsStructItemUtil;
import org.rust.lang.core.dfa.borrowck.gatherLoans.HasDestructorUtil;
import org.rust.lang.core.types.ty.*;
import org.rust.stdext.RsResult;

import java.util.*;
import java.util.stream.Collectors;

class MoveDataBuilder {
    @NotNull
    private final MirBody body;
    @NotNull
    private final MoveDataImpl data;
    @Nullable
    private MirLocation loc;

    private MoveDataBuilder(@NotNull MirBody body, @NotNull MoveDataImpl data) {
        this.body = body;
        this.data = data;
    }

    private void createMovePath(@NotNull MirPlace place) {
        movePathFor(place);
    }

    // https://github.com/rust-lang/rust/blob/f7b831ac8a897273f78b9f47165cf8e54066ce4b/compiler/rustc_mir_dataflow/src/move_paths/builder.rs#L100
    @NotNull
    private RsResult<MovePath, MoveError> movePathFor(@NotNull MirPlace place) {
        MovePath base = data.revLookup.getLocals().get(place.getLocal());
        if (base == null) {
            throw new IllegalStateException("Local not found: " + place.getLocal());
        }
        List<MirProjectionElem<Ty>> projections = place.getProjections();
        for (int i = 0; i < projections.size(); i++) {
            MirProjectionElem<Ty> elem = projections.get(i);
            List<MirProjectionElem<Ty>> projectionBase = projections.subList(0, i);
            Ty placeTy = MirPlace.tyFrom(place.getLocal(), projectionBase).getTy();
            if (placeTy instanceof TyReference || placeTy instanceof TyPointer) {
                throw new UnsupportedOperationException("TODO");
            } else if (placeTy instanceof TyAdt) {
                TyAdt tyAdt = (TyAdt) placeTy;
                if ((tyAdt.getItem() instanceof RsStructItem) && RsStructItemUtil.getKind((RsStructItem) tyAdt.getItem()) == RsStructKind.UNION) {
                    throw new UnsupportedOperationException("TODO");
                } else if (HasDestructorUtil.getHasDestructor(tyAdt.getItem()) && !TyUtil.isBox(tyAdt)) {
                    throw new UnsupportedOperationException("TODO");
                }
            } else if (placeTy instanceof TySlice) {
                throw new UnsupportedOperationException("TODO");
            } else if (placeTy instanceof TyArray) {
                throw new UnsupportedOperationException("TODO");
            }

            final int idx = i;
            final MovePath currentBase = base;
            base = addMovePath(currentBase, elem, () ->
                new MirPlace(place.getLocal(), new ArrayList<MirProjectionElem<Ty>>(projections.subList(0, idx + 1)))
            );
        }
        return new RsResult.Ok<>(base);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private MovePath addMovePath(@NotNull MovePath base, @NotNull MirProjectionElem<Ty> element, @NotNull java.util.function.Supplier<MirPlace> makePlace) {
        Pair<MovePath, MirProjectionElem<?>> key = new Pair<MovePath, MirProjectionElem<?>>(base, element.lift());
        MovePath existing = data.revLookup.getProjections().get(key);
        if (existing != null) {
            return existing;
        }
        MovePath newPath = newMovePath(data.movePaths, data.pathMap, data.initPathMap, base, makePlace.get());
        data.revLookup.getProjections().put(key, newPath);
        return newPath;
    }

    void gatherStatement(@NotNull MirLocation loc, @NotNull MirStatement stmt) {
        this.loc = loc;
        if (stmt instanceof MirStatement.Assign) {
            MirStatement.Assign assign = (MirStatement.Assign) stmt;
            // TODO Rvalue::CopyForDeref
            createMovePath(assign.getPlace());
            // TODO Rvalue::ShallowInitBox
            gatherInit(assign.getPlace(), InitKind.Deep);
            gatherRvalue(assign.getRvalue());
        } else if (stmt instanceof MirStatement.FakeRead) {
            createMovePath(((MirStatement.FakeRead) stmt).getPlace());
        } else if (stmt instanceof MirStatement.StorageLive) {
            // nothing
        } else if (stmt instanceof MirStatement.StorageDead) {
            gatherMove(new MirPlace(((MirStatement.StorageDead) stmt).getLocal()));
        }
    }

    void gatherTerminator(@NotNull MirLocation loc, @NotNull MirTerminator<?> term) {
        this.loc = loc;
        if (term instanceof MirTerminator.Goto
            || term instanceof MirTerminator.FalseEdge
            || term instanceof MirTerminator.FalseUnwind
            || term instanceof MirTerminator.Return
            || term instanceof MirTerminator.Resume
            || term instanceof MirTerminator.Unreachable
            || term instanceof MirTerminator.Drop) {
            // nothing
        } else if (term instanceof MirTerminator.Assert) {
            gatherOperand(((MirTerminator.Assert) term).getCond());
        } else if (term instanceof MirTerminator.SwitchInt) {
            gatherOperand(((MirTerminator.SwitchInt) term).getDiscriminant());
        } else if (term instanceof MirTerminator.Call) {
            MirTerminator.Call<?> call = (MirTerminator.Call<?>) term;
            gatherOperand(call.getCallee());
            for (MirOperand arg : call.getArgs()) {
                gatherOperand(arg);
            }
            if (call.getTarget() != null) {
                createMovePath(call.getDestination());
                gatherInit(call.getDestination(), InitKind.NonPanicPathOnly);
            }
        }
    }

    private void gatherInit(@NotNull MirPlace place, @NotNull InitKind kind) {
        // TODO union
        LookupResult lookup = data.revLookup.find(place);
        if (lookup instanceof LookupResult.Exact) {
            MovePath path = ((LookupResult.Exact) lookup).getMovePath();
            Init init = data.inits.allocate(index -> new Init(index, path, new InitLocation.Statement(loc), kind));
            List<Init> pathInits = data.initPathMap.get(path);
            if (pathInits != null) {
                pathInits.add(init);
            }
            data.initLocMap.getOrPut(loc, ArrayList::new).add(init);
        }
    }

    private void gatherRvalue(@NotNull MirRvalue rvalue) {
        if (rvalue instanceof MirRvalue.ThreadLocalRef) {
            // not-a-move
        } else if (rvalue instanceof MirRvalue.Use) {
            gatherOperand(((MirRvalue.Use) rvalue).getOperand());
        } else if (rvalue instanceof MirRvalue.Repeat) {
            gatherOperand(((MirRvalue.Repeat) rvalue).getOperand());
        } else if (rvalue instanceof MirRvalue.Aggregate) {
            for (MirOperand operand : ((MirRvalue.Aggregate) rvalue).getOperands()) {
                gatherOperand(operand);
            }
        } else if (rvalue instanceof MirRvalue.BinaryOpUse) {
            MirRvalue.BinaryOpUse binOp = (MirRvalue.BinaryOpUse) rvalue;
            gatherOperand(binOp.getLeft());
            gatherOperand(binOp.getRight());
        } else if (rvalue instanceof MirRvalue.CheckedBinaryOpUse) {
            MirRvalue.CheckedBinaryOpUse checkedBinOp = (MirRvalue.CheckedBinaryOpUse) rvalue;
            gatherOperand(checkedBinOp.getLeft());
            gatherOperand(checkedBinOp.getRight());
        } else if (rvalue instanceof MirRvalue.UnaryOpUse) {
            gatherOperand(((MirRvalue.UnaryOpUse) rvalue).getOperand());
        } else if (rvalue instanceof MirRvalue.CopyForDeref) {
            throw new IllegalStateException("unreachable");
        } else if (rvalue instanceof MirRvalue.Cast) {
            gatherOperand(((MirRvalue.Cast) rvalue).getOperand());
        }
        // Ref, AddressOf, Discriminant, Len, NullaryOpUse -> nothing
    }

    private void gatherOperand(@NotNull MirOperand operand) {
        if (operand instanceof MirOperand.Move) {
            gatherMove(((MirOperand.Move) operand).getPlace());
        }
    }

    private void gatherMove(@NotNull MirPlace place) {
        // TODO ProjectionElem::Subslice
        RsResult<MovePath, MoveError> path = movePathFor(place);
        if (path instanceof RsResult.Ok) {
            recordMove(((RsResult.Ok<MovePath, MoveError>) path).getOk());
        } else {
            throw new UnsupportedOperationException("TODO");
        }
    }

    private void recordMove(@NotNull MovePath path) {
        MoveOut moveOut = data.moves.allocate(index -> new MoveOut(index, path, loc));
        List<MoveOut> pathMoves = data.pathMap.get(path);
        if (pathMoves != null) {
            pathMoves.add(moveOut);
        }
        data.locMap.getOrPut(loc, ArrayList::new).add(moveOut);
    }

    void gatherArgs() {
        for (MirLocal arg : body.getArgs()) {
            MovePath path = data.revLookup.find(arg);
            Init init = data.inits.allocate(index ->
                new Init(index, path, new InitLocation.Argument(arg), InitKind.Deep)
            );
            List<Init> pathInits = data.initPathMap.get(path);
            if (pathInits != null) {
                pathInits.add(init);
            }
        }
    }

    @NotNull
    MoveDataImpl finalize_() {
        return data;
    }

    @NotNull
    static MoveData gatherMoves(@NotNull MirBody body) {
        MoveDataBuilder builder = newBuilder(body);

        builder.gatherArgs();

        for (MirBasicBlock bb : body.getBasicBlocks()) {
            List<MirStatement> statements = bb.getStatements();
            for (int i = 0; i < statements.size(); i++) {
                MirLocation loc = new MirLocation(bb, i);
                builder.gatherStatement(loc, statements.get(i));
            }
            MirLocation terminatorLoc = new MirLocation(bb, statements.size());
            builder.gatherTerminator(terminatorLoc, bb.getTerminator());
        }

        return builder.finalize_();
    }

    @NotNull
    private static MoveDataBuilder newBuilder(@NotNull MirBody body) {
        IndexAlloc<MovePath> movePaths = new IndexAlloc<>();
        IndexKeyMap<MovePath, List<MoveOut>> pathMap = new IndexKeyMap<>();
        IndexKeyMap<MovePath, List<Init>> initPathMap = new IndexKeyMap<>();

        List<MovePath> localMovePaths = new ArrayList<>();
        for (MirLocal local : body.getLocalDecls()) {
            localMovePaths.add(newMovePath(movePaths, pathMap, initPathMap, null, new MirPlace(local)));
        }

        return new MoveDataBuilder(
            body,
            new MoveDataImpl(
                movePaths,
                new IndexAlloc<>(),
                new LocationMap<>(body),
                pathMap,
                new MovePathLookup(
                    IndexKeyMap.fromListUnchecked(localMovePaths),
                    new HashMap<>()
                ),
                new IndexAlloc<>(),
                new LocationMap<>(body),
                initPathMap
            )
        );
    }

    @NotNull
    static MovePath newMovePath(
        @NotNull IndexAlloc<MovePath> movePaths,
        @NotNull IndexKeyMap<MovePath, List<MoveOut>> pathMap,
        @NotNull IndexKeyMap<MovePath, List<Init>> initPathMap,
        @Nullable MovePath parent,
        @NotNull MirPlace place
    ) {
        MovePath movePath = movePaths.allocate(index -> new MovePath(index, place, parent));
        if (parent != null) {
            MovePath nextSibling = parent.getFirstChild();
            parent.setFirstChild(movePath);
            movePath.setNextSibling(nextSibling);
        }
        pathMap.put(movePath, new ArrayList<>());
        initPathMap.put(movePath, new ArrayList<>());
        return movePath;
    }

    /**
     * Internal implementation of MoveData.
     */
    static class MoveDataImpl implements MoveData {
        @NotNull
        final IndexAlloc<MovePath> movePaths;
        @NotNull
        final IndexAlloc<MoveOut> moves;
        @NotNull
        final LocationMap<List<MoveOut>> locMap;
        @NotNull
        final IndexKeyMap<MovePath, List<MoveOut>> pathMap;
        @NotNull
        final MovePathLookup revLookup;
        @NotNull
        final IndexAlloc<Init> inits;
        @NotNull
        final LocationMap<List<Init>> initLocMap;
        @NotNull
        final IndexKeyMap<MovePath, List<Init>> initPathMap;

        MoveDataImpl(
            @NotNull IndexAlloc<MovePath> movePaths,
            @NotNull IndexAlloc<MoveOut> moves,
            @NotNull LocationMap<List<MoveOut>> locMap,
            @NotNull IndexKeyMap<MovePath, List<MoveOut>> pathMap,
            @NotNull MovePathLookup revLookup,
            @NotNull IndexAlloc<Init> inits,
            @NotNull LocationMap<List<Init>> initLocMap,
            @NotNull IndexKeyMap<MovePath, List<Init>> initPathMap
        ) {
            this.movePaths = movePaths;
            this.moves = moves;
            this.locMap = locMap;
            this.pathMap = pathMap;
            this.revLookup = revLookup;
            this.inits = inits;
            this.initLocMap = initLocMap;
            this.initPathMap = initPathMap;
        }

        @Override
        @NotNull
        public Map<MirLocation, List<MoveOut>> getLocMap() {
            return locMap;
        }

        @Override
        @NotNull
        public Map<MovePath, List<MoveOut>> getPathMap() {
            return pathMap;
        }

        @Override
        @NotNull
        public MovePathLookup getRevLookup() {
            return revLookup;
        }

        @Override
        @NotNull
        public Map<MirLocation, List<Init>> getInitLocMap() {
            return initLocMap;
        }

        @Override
        @NotNull
        public Map<MovePath, List<Init>> getInitPathMap() {
            return initPathMap;
        }

        @Override
        public int getMovePathsCount() {
            return movePaths.getSize();
        }
    }
}
