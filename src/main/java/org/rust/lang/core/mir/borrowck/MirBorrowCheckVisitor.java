/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.borrowck;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.dataflow.framework.BorrowCheckResults;
import org.rust.lang.core.mir.dataflow.framework.BorrowData;
import org.rust.lang.core.mir.dataflow.framework.BorrowSet;
import org.rust.lang.core.mir.dataflow.framework.ResultsVisitor;
import org.rust.lang.core.mir.dataflow.move.*;
import org.rust.lang.core.mir.schemas.*;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.*;
import java.util.function.Consumer;

public class MirBorrowCheckVisitor implements ResultsVisitor<BorrowCheckResults.State> {

    @NotNull
    private final MirBody body;
    @NotNull
    private final MoveData moveData;

    /** The set of borrows extracted from the MIR */
    @NotNull
    private final BorrowSet borrowSet;

    /**
     * This keeps track of whether local variables are free-ed when the function exits even without a {@code StorageDead},
     * which appears to be the case for constants.
     */
    private final boolean localsAreInvalidatedAtExit;

    @NotNull
    private final Set<RsElement> usesOfMovedValue = new LinkedHashSet<>();
    @NotNull
    private final Set<RsElement> usesOfUninitializedVariable = new LinkedHashSet<>();
    @NotNull
    private final Set<RsElement> moveOutWhileBorrowedValues = new LinkedHashSet<>();

    public MirBorrowCheckVisitor(
        @NotNull MirBody body,
        @NotNull MoveData moveData,
        @NotNull BorrowSet borrowSet,
        boolean localsAreInvalidatedAtExit
    ) {
        this.body = body;
        this.moveData = moveData;
        this.borrowSet = borrowSet;
        this.localsAreInvalidatedAtExit = localsAreInvalidatedAtExit;
    }

    @NotNull
    public MirBorrowCheckResult getResult() {
        return new MirBorrowCheckResult(
            new ArrayList<>(usesOfUninitializedVariable),
            new ArrayList<>(usesOfMovedValue),
            new ArrayList<>(moveOutWhileBorrowedValues)
        );
    }

    @Override
    public void visitStatementBeforePrimaryEffect(
        @NotNull BorrowCheckResults.State state,
        @NotNull MirStatement statement,
        @NotNull MirLocation location
    ) {
        checkActivations(location, state);

        if (statement instanceof MirStatement.Assign) {
            MirStatement.Assign assign = (MirStatement.Assign) statement;
            consumeRvalue(location, assign.getRvalue(), state);
            mutatePlace(location, assign.getPlace(), state);
        } else if (statement instanceof MirStatement.FakeRead) {
            MirStatement.FakeRead fakeRead = (MirStatement.FakeRead) statement;
            checkIfPathOrSubpathIsMoved(location, fakeRead.getPlace(), state);
        } else if (statement instanceof MirStatement.StorageDead) {
            // TODO accessPlace(location, statement.local, Write(StorageDeadOrDrop), state)
        } else if (statement instanceof MirStatement.StorageLive) {
            // no-op
        }
    }

    @Override
    public void visitTerminatorBeforePrimaryEffect(
        @NotNull BorrowCheckResults.State state,
        @NotNull MirTerminator<MirBasicBlock> terminator,
        @NotNull MirLocation location
    ) {
        checkActivations(location, state);
    }

    @Override
    public void visitTerminatorAfterPrimaryEffect(
        @NotNull BorrowCheckResults.State state,
        @NotNull MirTerminator<MirBasicBlock> terminator,
        @NotNull MirLocation location
    ) {
        if (terminator instanceof MirTerminator.Resume || terminator instanceof MirTerminator.Return) {
            // Returning from the function implicitly kills storage for all locals and statics.
            // Often, the storage will already have been killed by an explicit StorageDead, but we don't always emit
            // those (notably on unwind paths), so this "extra check" serves as a kind of backup.
            for (BorrowData borrow : borrowSet) {
                if (!state.getBorrows().get(borrow.getIndex())) continue;
                // TODO: checkForInvalidationAtExit
            }
        }
    }

    private void mutatePlace(@NotNull MirLocation location, @NotNull MirPlace place, @NotNull BorrowCheckResults.State state) {
        // TODO: checkIfAssignedPathIsMoved
        accessPlace(location, place, new MirReadOrWrite.Write(MirWriteKind.Mutate.INSTANCE), state);
    }

    private void consumeRvalue(@NotNull MirLocation location, @NotNull MirRvalue rvalue, @NotNull BorrowCheckResults.State state) {
        Consumer<MirPlace> implLenAndDiscriminant = place -> {
            accessPlace(location, place, new MirReadOrWrite.Read(MirReadKind.Copy.INSTANCE), state);
            checkIfPathOrSubpathIsMoved(location, place, state);
        };

        if (rvalue instanceof MirRvalue.Ref) {
            MirRvalue.Ref ref = (MirRvalue.Ref) rvalue;
            MirBorrowKind borrowKind = ref.getBorrowKind();
            MirReadOrWrite readOrWrite;
            if (borrowKind instanceof MirBorrowKind.Shallow || borrowKind instanceof MirBorrowKind.Shared) {
                readOrWrite = new MirReadOrWrite.Read(new MirReadKind.Borrow(borrowKind));
            } else if (borrowKind instanceof MirBorrowKind.Unique || borrowKind instanceof MirBorrowKind.Mut) {
                MirWriteKind.MutableBorrow writeKind = new MirWriteKind.MutableBorrow(borrowKind);
                if (borrowKind.getAllowTwoPhaseBorrow()) {
                    readOrWrite = new MirReadOrWrite.Reservation(writeKind);
                } else {
                    readOrWrite = new MirReadOrWrite.Write(writeKind);
                }
            } else {
                throw new IllegalStateException("Unknown borrow kind: " + borrowKind);
            }

            accessPlace(location, ref.getPlace(), readOrWrite, state);
            checkIfPathOrSubpathIsMoved(location, ref.getPlace(), state);
        } else if (rvalue instanceof MirRvalue.AddressOf) {
            throw new UnsupportedOperationException("TODO");
        } else if (rvalue instanceof MirRvalue.ThreadLocalRef) {
            // no-op
        } else if (rvalue instanceof MirRvalue.Use) {
            consumeOperand(location, ((MirRvalue.Use) rvalue).getOperand(), state);
        } else if (rvalue instanceof MirRvalue.UnaryOpUse) {
            consumeOperand(location, ((MirRvalue.UnaryOpUse) rvalue).getOperand(), state);
        } else if (rvalue instanceof MirRvalue.BinaryOpUse) {
            MirRvalue.BinaryOpUse binOp = (MirRvalue.BinaryOpUse) rvalue;
            consumeOperand(location, binOp.getLeft(), state);
            consumeOperand(location, binOp.getRight(), state);
        } else if (rvalue instanceof MirRvalue.CheckedBinaryOpUse) {
            MirRvalue.CheckedBinaryOpUse checkedBinOp = (MirRvalue.CheckedBinaryOpUse) rvalue;
            consumeOperand(location, checkedBinOp.getLeft(), state);
            consumeOperand(location, checkedBinOp.getRight(), state);
        } else if (rvalue instanceof MirRvalue.NullaryOpUse) {
            // nullary ops take no dynamic input; no borrowck effect.
        } else if (rvalue instanceof MirRvalue.Repeat) {
            consumeOperand(location, ((MirRvalue.Repeat) rvalue).getOperand(), state);
        } else if (rvalue instanceof MirRvalue.Aggregate) {
            MirRvalue.Aggregate aggregate = (MirRvalue.Aggregate) rvalue;
            // when (rvalue) { is Adt, is Array, is Tuple -> Unit; is Closure -> propagateClosureUsedMutUpvar() }
            for (MirOperand operand : aggregate.getOperands()) {
                consumeOperand(location, operand, state);
            }
        } else if (rvalue instanceof MirRvalue.CopyForDeref) {
            throw new UnsupportedOperationException("TODO");
        } else if (rvalue instanceof MirRvalue.Len) {
            implLenAndDiscriminant.accept(((MirRvalue.Len) rvalue).getPlace());
        } else if (rvalue instanceof MirRvalue.Discriminant) {
            implLenAndDiscriminant.accept(((MirRvalue.Discriminant) rvalue).getPlace());
        } else if (rvalue instanceof MirRvalue.Cast) {
            consumeOperand(location, ((MirRvalue.Cast) rvalue).getOperand(), state);
        }
    }

    private void consumeOperand(@NotNull MirLocation location, @NotNull MirOperand operand, @NotNull BorrowCheckResults.State state) {
        if (operand instanceof MirOperand.Constant) {
            // no-op
        } else if (operand instanceof MirOperand.Copy) {
            MirOperand.Copy copy = (MirOperand.Copy) operand;
            accessPlace(location, copy.getPlace(), new MirReadOrWrite.Read(MirReadKind.Copy.INSTANCE), state);
            checkIfPathOrSubpathIsMoved(location, copy.getPlace(), state);
        } else if (operand instanceof MirOperand.Move) {
            MirOperand.Move move = (MirOperand.Move) operand;
            accessPlace(location, move.getPlace(), new MirReadOrWrite.Write(MirWriteKind.Move.INSTANCE), state);
            checkIfPathOrSubpathIsMoved(location, move.getPlace(), state);
        }
    }

    private void accessPlace(
        @NotNull MirLocation location,
        @NotNull MirPlace place,
        @NotNull MirReadOrWrite readOrWrite,
        @NotNull BorrowCheckResults.State state
    ) {
        checkAccessForConflict(location, place, readOrWrite, state);
    }

    private void checkAccessForConflict(
        @NotNull MirLocation location,
        @NotNull MirPlace place,
        @NotNull MirReadOrWrite readOrWrite,
        @NotNull BorrowCheckResults.State state
    ) {
        for (BorrowData borrow : borrowSet) {
            if (!state.getBorrows().get(borrow.getIndex())) continue;
            // TODO: if (!borrowConflictsWithPlace()) continue

            // Obviously an activation is compatible with its own reservation (or even prior activating uses of same
            // borrow); so don't check if they interfere.
            //
            // NOTE: *reservations* do conflict with themselves; thus aren't injecting unsoundness w/ this check.)
            if (readOrWrite instanceof MirReadOrWrite.Activation
                && ((MirReadOrWrite.Activation) readOrWrite).getBorrow() == borrow) {
                continue;
            }

            if (readOrWrite instanceof MirReadOrWrite.Read) {
                MirReadOrWrite.Read read = (MirReadOrWrite.Read) readOrWrite;
                if (borrow.getKind() instanceof MirBorrowKind.Shared || borrow.getKind() instanceof MirBorrowKind.Shallow) {
                    continue;
                }
                if (read.getKind() instanceof MirReadKind.Borrow
                    && ((MirReadKind.Borrow) read.getKind()).getKind() instanceof MirBorrowKind.Shallow
                    && (borrow.getKind() instanceof MirBorrowKind.Unique || borrow.getKind() instanceof MirBorrowKind.Mut)) {
                    continue;
                }
            }

            if (readOrWrite instanceof MirReadOrWrite.Reservation) {
                if (borrow.getKind() instanceof MirBorrowKind.Shallow || borrow.getKind() instanceof MirBorrowKind.Shared) {
                    // This used to be a future compatibility warning (to be disallowed on NLL).
                    // See rust-lang/rust#56254
                    continue;
                }
            }

            if (readOrWrite instanceof MirReadOrWrite.Write) {
                MirReadOrWrite.Write write = (MirReadOrWrite.Write) readOrWrite;
                if (write.getKind() instanceof MirWriteKind.Move && borrow.getKind() instanceof MirBorrowKind.Shallow) {
                    // Handled by initialization checks.
                    continue;
                }
            }

            if (readOrWrite instanceof MirReadOrWrite.Read) {
                MirReadOrWrite.Read read = (MirReadOrWrite.Read) readOrWrite;
                if (borrow.getKind() instanceof MirBorrowKind.Unique || borrow.getKind() instanceof MirBorrowKind.Mut) {
                    // TODO: check `!isActive`
                    if (read.getKind() instanceof MirReadKind.Copy) {
                        // TODO: reportUseWhileMutablyBorrowed
                    } else if (read.getKind() instanceof MirReadKind.Borrow) {
                        // TODO: reportConflictingBorrow
                    }
                    continue;
                }
            }

            if (readOrWrite instanceof MirReadOrWrite.Activation
                || readOrWrite instanceof MirReadOrWrite.Reservation
                || readOrWrite instanceof MirReadOrWrite.Write) {

                MirWriteKind kind;
                if (readOrWrite instanceof MirReadOrWrite.Activation) {
                    kind = ((MirReadOrWrite.Activation) readOrWrite).getKind();
                } else if (readOrWrite instanceof MirReadOrWrite.Reservation) {
                    kind = ((MirReadOrWrite.Reservation) readOrWrite).getKind();
                } else {
                    kind = ((MirReadOrWrite.Write) readOrWrite).getKind();
                }

                if (kind instanceof MirWriteKind.MutableBorrow) {
                    // TODO: reportConflictingBorrow
                } else if (kind instanceof MirWriteKind.StorageDeadOrDrop) {
                    // TODO: reportStorageDeadOrDropOfBorrowed
                } else if (kind instanceof MirWriteKind.Mutate) {
                    // TODO: reportIllegalMutationOfBorrowed
                } else if (kind instanceof MirWriteKind.Move) {
                    reportMoveOutWhileBorrowed(location);
                }
            }
        }
    }

    private void reportMoveOutWhileBorrowed(@NotNull MirLocation location) {
        PsiElement element = location.getSource().getSpan().getReference();
        if (element instanceof RsElement) {
            moveOutWhileBorrowedValues.add((RsElement) element);
        }
    }

    private void checkActivations(@NotNull MirLocation location, @NotNull BorrowCheckResults.State state) {
        // Two-phase borrow support: For each activation that is newly generated at this statement, check if it
        // interferes with another borrow.
        for (BorrowData borrow : borrowSet.activationsAtLocation(location)) {
            if (borrow.getKind() instanceof MirBorrowKind.Shallow || borrow.getKind() instanceof MirBorrowKind.Shared) {
                throw new IllegalStateException("only mutable borrows should be 2-phase");
            }
            // Unique and Mut are fine

            accessPlace(
                location,
                borrow.getBorrowedPlace(),
                new MirReadOrWrite.Activation(new MirWriteKind.MutableBorrow(borrow.getKind()), borrow),
                state
            );

            // We do not need to call `checkIfPathOrSubpathIsMoved` again, as we already called it when we made
            // the initial reservation.
        }
    }

    private void checkIfFullPathIsMoved(@NotNull MirLocation location, @NotNull MirPlace place, @NotNull BorrowCheckResults.State state) {
        BitSet maybeUninits = state.getUninits();
        MovePath movePath = movePathClosestTo(place);
        if (maybeUninits.get(movePath.getIndex())) {
            reportUseOfMovedOrUninitialized(location, movePath.getPlace(), place, movePath);
        }
    }

    private void checkIfPathOrSubpathIsMoved(@NotNull MirLocation location, @NotNull MirPlace place, @NotNull BorrowCheckResults.State state) {
        checkIfFullPathIsMoved(location, place, state);

        // TODO MirProjectionElem.Subslice

        MovePath movePath = movePathForPlace(place);
        if (movePath == null) return;
        MovePath uninitMovePath = movePath.findInMovePathOrItsDescendants(
            mp -> state.getUninits().get(mp.getIndex())
        );
        if (uninitMovePath == null) return;
        reportUseOfMovedOrUninitialized(location, place, place, uninitMovePath);
    }

    @NotNull
    private MovePath movePathClosestTo(@NotNull MirPlace place) {
        LookupResult result = moveData.getRevLookup().find(place);
        if (result instanceof LookupResult.Exact) {
            return ((LookupResult.Exact) result).getMovePath();
        } else if (result instanceof LookupResult.Parent) {
            MovePath movePath = ((LookupResult.Parent) result).getMovePath();
            if (movePath == null) {
                throw new IllegalStateException("should have move path for every Local");
            }
            return movePath;
        }
        throw new IllegalStateException("Unknown LookupResult: " + result);
    }

    /**
     * If returns {@code null}, then there is no move path corresponding to a direct owner of {@code place}
     * (which means there is nothing that borrowck tracks for its analysis).
     */
    @Nullable
    private MovePath movePathForPlace(@NotNull MirPlace place) {
        LookupResult result = moveData.getRevLookup().find(place);
        if (result instanceof LookupResult.Exact) {
            return ((LookupResult.Exact) result).getMovePath();
        } else if (result instanceof LookupResult.Parent) {
            return null;
        }
        throw new IllegalStateException("Unknown LookupResult: " + result);
    }

    private void reportUseOfMovedOrUninitialized(
        @NotNull MirLocation location,
        @NotNull MirPlace movedPlace,
        @NotNull MirPlace usedPlace,
        @NotNull MovePath movePath
    ) {
        List<MoveOut> moveOutIndices = getMovedIndexes(location, movePath);
        PsiElement element = location.getSource().getSpan().getReference();
        if (element instanceof RsElement) {
            if (moveOutIndices.isEmpty()) {
                usesOfUninitializedVariable.add((RsElement) element);
            } else {
                usesOfMovedValue.add((RsElement) element);
            }
        }
    }

    @NotNull
    private List<MoveOut> getMovedIndexes(@NotNull MirLocation location, @NotNull MovePath movePath) {
        List<MovePath> movePaths = movePath.getAncestors();

        ArrayDeque<MirLocation> stack = new ArrayDeque<>();
        ArrayDeque<MirLocation> backEdgeStack = new ArrayDeque<>();
        for (MirLocation predecessor : predecessorLocations(body, location)) {
            boolean dominates = false; // TODO `location.dominates(predecessor)`
            if (dominates) {
                backEdgeStack.push(predecessor);
            } else {
                stack.push(predecessor);
            }
        }

        boolean reachedStart = false;

        /* Check if the mpi is initialized as an argument */
        boolean isArgument = false;
        for (MirLocal arg : body.getArgs()) {
            MovePath argMovePath = moveData.getRevLookup().find(arg);
            if (movePaths.contains(argMovePath)) {
                isArgument = true;
                break;
            }
        }

        Set<MirLocation> visited = new LinkedHashSet<>();
        List<MoveOut> result = new ArrayList<>();

        while (!stack.isEmpty()) {
            MirLocation location1 = stack.pop();
            if (dfsIter(location1, movePaths, movePath, visited, result)) continue;

            boolean hasPredecessor = false;
            for (MirLocation predecessor : predecessorLocations(body, location1)) {
                boolean dominates = false; // TODO `location1.dominates(predecessor)`
                if (dominates) {
                    backEdgeStack.push(predecessor);
                } else {
                    stack.push(predecessor);
                }
                hasPredecessor = true;
            }

            if (!hasPredecessor) {
                reachedStart = true;
            }
        }

        // Process back edges (moves in future loop iterations) only if
        // the move path is definitely initialized upon loop entry
        if ((isArgument || !reachedStart) && result.isEmpty()) {
            while (!backEdgeStack.isEmpty()) {
                MirLocation location1 = backEdgeStack.pop();
                if (dfsIter(location1, movePaths, movePath, visited, result)) continue;

                for (MirLocation predecessor : predecessorLocations(body, location1)) {
                    backEdgeStack.push(location1);
                }
            }
        }

        return result;
    }

    /**
     * Returns true if we should continue (skip processing predecessors).
     */
    private boolean dfsIter(
        @NotNull MirLocation location,
        @NotNull List<MovePath> movePaths,
        @NotNull MovePath movePath,
        @NotNull Set<MirLocation> visited,
        @NotNull List<MoveOut> result
    ) {
        if (!visited.add(location)) return true;

        // check for moves
        if (!(location.getStatement() instanceof MirStatement.StorageDead)) {
            // this analysis only tries to find moves explicitly written by the user,
            // so we ignore the move-outs created by `StorageDead` and at the beginning of a function

            List<MoveOut> moveOuts = moveData.getLocMap().get(location);
            if (moveOuts != null) {
                for (MoveOut moveOut : moveOuts) {
                    if (movePaths.contains(moveOut.getPath())) {
                        result.add(moveOut);
                        return true;
                    }
                }
            }
        }

        // check for inits
        boolean anyMatch = false;
        List<Init> inits = moveData.getInitLocMap().get(location);
        if (inits != null) {
            for (Init init : inits) {
                InitKind kind = init.getKind();
                if (kind == InitKind.Deep || kind == InitKind.NonPanicPathOnly) {
                    if (movePaths.contains(init.getPath())) {
                        anyMatch = true;
                    }
                } else if (kind == InitKind.Shallow) {
                    if (movePath.equals(init.getPath())) {
                        anyMatch = true;
                    }
                }
            }
        }
        return anyMatch;
    }

    @NotNull
    private static List<MirLocation> predecessorLocations(@NotNull MirBody body, @NotNull MirLocation location) {
        if (location.getStatementIndex() == 0) {
            List<MirBasicBlock> preds = body.getBasicBlocksPredecessors().get(location.getBlock());
            List<MirLocation> result = new ArrayList<>(preds.size());
            for (MirBasicBlock pred : preds) {
                result.add(pred.getTerminatorLocation());
            }
            return result;
        } else {
            return Collections.singletonList(new MirLocation(location.getBlock(), location.getStatementIndex() - 1));
        }
    }
}
