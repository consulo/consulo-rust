/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa.borrowck;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.dfa.ControlFlowGraph;
import org.rust.lang.core.dfa.DataFlow;
import org.rust.lang.core.dfa.DataFlow.*;
import org.rust.lang.core.dfa.ExprUseWalker.MutateMode;
import org.rust.lang.core.psi.RsNamedFieldDecl;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.ty.TyUnknown;
import org.rust.openapiext.TestAssertUtil;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

// ---- MoveData ----
public class MoveData {
    @Nonnull
    public final List<MovePath> paths;
    @Nonnull
    public final Map<LoanPath, MovePath> pathMap;
    @Nonnull
    public final List<Move> moves;
    @Nonnull
    public final List<Assignment> varAssignments;
    @Nonnull
    private final List<Assignment> pathAssignments;
    @Nonnull
    private final Set<RsElement> assigneeElements;

    public MoveData() {
        this.paths = new ArrayList<>();
        this.pathMap = new HashMap<>();
        this.moves = new ArrayList<>();
        this.varAssignments = new ArrayList<>();
        this.pathAssignments = new ArrayList<>();
        this.assigneeElements = new HashSet<>();
    }

    public boolean isEmpty() {
        return moves.isEmpty() && pathAssignments.isEmpty() && varAssignments.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    private boolean eachExtendingPath(@Nonnull MovePath movePath, @Nonnull Predicate<MovePath> action) {
        if (!action.test(movePath)) return false;
        MovePath path = movePath.firstChild;
        while (path != null) {
            if (!eachExtendingPath(path, action)) return false;
            path = path.nextSibling;
        }
        return true;
    }

    private boolean eachApplicableMove(@Nonnull MovePath movePath, @Nonnull Predicate<Move> action) {
        boolean[] result = {true};
        eachExtendingPath(movePath, path -> {
            Move move = path.firstMove;
            while (move != null) {
                if (!action.test(move)) {
                    result[0] = false;
                    return false;
                }
                move = move.nextMove;
            }
            return result[0];
        });
        return result[0];
    }

    private void killMoves(@Nonnull MovePath path, @Nonnull RsElement killElement,
                           @Nonnull KillFrom killKind,
                           @Nonnull DataFlowContext<MoveDataFlowOperator> dfcxMoves) {
        if (!LoanPathUtil.isPrecise(path.loanPath)) return;
        eachApplicableMove(path, move -> {
            dfcxMoves.addKill(killKind, killElement, move.index);
            return true;
        });
    }

    public void addGenKills(
        @Nonnull BorrowChecker.BorrowCheckContext bccx,
        @Nonnull DataFlowContext<MoveDataFlowOperator> dfcxMoves,
        @Nonnull DataFlowContext<AssignDataFlowOperator> dfcxAssign
    ) {
        for (int i = 0; i < moves.size(); i++) {
            dfcxMoves.addGen(moves.get(i).element, i);
        }

        for (int i = 0; i < varAssignments.size(); i++) {
            Assignment assignment = varAssignments.get(i);
            dfcxAssign.addGen(assignment.element, i);
            killMoves(assignment.path, assignment.element, KillFrom.Execution, dfcxMoves);
        }

        for (Assignment assignment : pathAssignments) {
            killMoves(assignment.path, assignment.element, KillFrom.Execution, dfcxMoves);
        }

        for (MovePath path : paths) {
            LoanPathKind kind = path.loanPath.kind;
            if (kind instanceof LoanPathKind.Var || kind instanceof LoanPathKind.Downcast) {
                org.rust.lang.core.types.regions.Scope killScope = path.loanPath.killScope(bccx);
                MovePath movePath = pathMap.get(path.loanPath);
                if (movePath == null) return;
                killMoves(movePath, killScope.getElement(), KillFrom.ScopeEnd, dfcxMoves);
            }
        }

        for (int i = 0; i < varAssignments.size(); i++) {
            Assignment assignment = varAssignments.get(i);
            LoanPath lp = assignment.path.loanPath;
            if (lp.kind instanceof LoanPathKind.Var || lp.kind instanceof LoanPathKind.Downcast) {
                org.rust.lang.core.types.regions.Scope killScope = lp.killScope(bccx);
                dfcxAssign.addKill(KillFrom.ScopeEnd, killScope.getElement(), i);
            }
        }
    }

    @Nonnull
    private MovePath movePathOf(@Nonnull LoanPath loanPath) {
        MovePath existing = pathMap.get(loanPath);
        if (existing != null) return existing;

        LoanPathKind kind = loanPath.kind;
        int oldSize;
        if (kind instanceof LoanPathKind.Var) {
            oldSize = paths.size();
            paths.add(new MovePath(loanPath));
        } else {
            LoanPath base;
            if (kind instanceof LoanPathKind.Downcast) {
                base = ((LoanPathKind.Downcast) kind).loanPath;
            } else {
                base = ((LoanPathKind.Extend) kind).loanPath;
            }
            MovePath parentPath = movePathOf(base);
            oldSize = paths.size();
            MovePath newMovePath = new MovePath(loanPath, parentPath, null, null, parentPath.firstChild);
            parentPath.firstChild = newMovePath;
            paths.add(newMovePath);
        }

        TestAssertUtil.testAssert(() -> oldSize == paths.size() - 1);
        pathMap.put(loanPath, paths.get(paths.size() - 1));
        return paths.get(paths.size() - 1);
    }

    private void processUnionFields(@Nonnull LoanPathKind.Extend lpKind, @Nonnull java.util.function.Consumer<LoanPath> action) {
        LoanPath base = lpKind.loanPath;
        if (!(base.ty instanceof TyAdt)) return;
        if (!(lpKind.lpElement instanceof LoanPathElement.Interior)) return;
        TyAdt baseType = (TyAdt) base.ty;
        LoanPathElement.Interior lpElement = (LoanPathElement.Interior) lpKind.lpElement;
        if (!(baseType.getItem() instanceof RsStructItem)) return;
        RsStructItem structItem = (RsStructItem) baseType.getItem();
        if (RsStructItemUtil.getKind(structItem) != RsStructKind.UNION) return;

        String interiorFieldName = lpElement instanceof LoanPathElement.Interior.Field
            ? ((LoanPathElement.Interior.Field) lpElement).name : null;
        RsElement variant = lpElement.getElement();
        org.rust.lang.core.dfa.MemoryCategorization.MutabilityCategory mutCat = lpKind.mutCategory;

        for (RsNamedFieldDecl field : RsFieldsOwnerUtil.getNamedFields(structItem)) {
            if (!Objects.equals(field.getName(), interiorFieldName)) {
                LoanPathKind.Extend siblingLpKind = new LoanPathKind.Extend(
                    base, mutCat, new LoanPathElement.Interior.Field(variant, field.getName()));
                LoanPath siblingLp = new LoanPath(siblingLpKind, TyUnknown.INSTANCE, base.element);
                action.accept(siblingLp);
            }
        }
    }

    public void addMove(@Nonnull LoanPath loanPath, @Nonnull RsElement element, @Nonnull MoveKind kind) {
        LoanPath lp = loanPath;
        LoanPathKind lpKind = lp.kind;
        while (lpKind instanceof LoanPathKind.Extend) {
            LoanPathKind.Extend ext = (LoanPathKind.Extend) lpKind;
            if (LoanPathUtil.isUnion(ext.loanPath)) {
                processUnionFields(ext, lp2 -> addMoveHelper(lp2, element, kind));
            }
            lp = ext.loanPath;
            lpKind = lp.kind;
        }
        addMoveHelper(loanPath, element, kind);
    }

    private void addMoveHelper(@Nonnull LoanPath loanPath, @Nonnull RsElement element, @Nonnull MoveKind kind) {
        MovePath path = movePathOf(loanPath);
        Move nextMove = path.firstMove;
        Move newMove = new Move(path, element, kind, moves.size(), nextMove);
        path.firstMove = newMove;
        moves.add(newMove);
    }

    public void addAssignment(@Nonnull LoanPath loanPath, @Nonnull RsElement assign,
                              @Nonnull RsElement assignee, @Nonnull MutateMode mode) {
        LoanPathKind lpKind = loanPath.kind;
        if (lpKind instanceof LoanPathKind.Extend && LoanPathUtil.isUnion(((LoanPathKind.Extend) lpKind).loanPath)) {
            processUnionFields((LoanPathKind.Extend) lpKind, lp -> addAssignmentHelper(lp, assign, assignee, mode));
        } else {
            addAssignmentHelper(loanPath, assign, assignee, mode);
        }
    }

    private void addAssignmentHelper(@Nonnull LoanPath loanPath, @Nonnull RsElement assign,
                                     @Nonnull RsElement assignee, @Nonnull MutateMode mode) {
        if (mode == MutateMode.Init || mode == MutateMode.JustWrite) {
            assigneeElements.add(assignee);
        }
        MovePath movePath = movePathOf(loanPath);
        Assignment assignment = new Assignment(movePath, assign);
        if (movePath.isVariablePath()) {
            varAssignments.add(assignment);
        } else {
            pathAssignments.add(assignment);
        }
    }

    @Nonnull
    public List<MovePath> existingBasePaths(@Nonnull LoanPath loanPath) {
        List<MovePath> result = new ArrayList<>();
        addExistingBasePaths(loanPath, result);
        return result;
    }

    private void addExistingBasePaths(@Nonnull LoanPath loanPath, @Nonnull List<MovePath> result) {
        MovePath movePath = pathMap.get(loanPath);
        if (movePath != null) {
            eachBasePath(movePath, p -> {
                result.add(p);
                return true;
            });
            return;
        }
        LoanPathKind kind = loanPath.kind;
        LoanPath baseLoanPath = null;
        if (kind instanceof LoanPathKind.Downcast) baseLoanPath = ((LoanPathKind.Downcast) kind).loanPath;
        else if (kind instanceof LoanPathKind.Extend) baseLoanPath = ((LoanPathKind.Extend) kind).loanPath;
        if (baseLoanPath == null) return;
        addExistingBasePaths(baseLoanPath, result);
    }

    public boolean eachBasePath(@Nonnull MovePath movePath, @Nonnull Predicate<MovePath> predicate) {
        MovePath path = movePath;
        while (true) {
            if (!predicate.test(path)) return false;
            if (path.parent == null) return true;
            path = path.parent;
        }
    }
}

// ---- FlowedMoveData ----
class FlowedMoveData {
    @Nonnull
    private final MoveData moveData;
    @Nonnull
    private final DataFlowContext<MoveDataFlowOperator> dfcxMoves;
    @Nonnull
    private final DataFlowContext<AssignDataFlowOperator> dfcxAssign;

    private FlowedMoveData(
        @Nonnull MoveData moveData,
        @Nonnull DataFlowContext<MoveDataFlowOperator> dfcxMoves,
        @Nonnull DataFlowContext<AssignDataFlowOperator> dfcxAssign
    ) {
        this.moveData = moveData;
        this.dfcxMoves = dfcxMoves;
        this.dfcxAssign = dfcxAssign;
    }

    public boolean eachMoveOf(@Nonnull RsElement element, @Nonnull LoanPath loanPath, @Nonnull BiPredicate<Move, LoanPath> predicate) {
        List<MovePath> baseNodes = moveData.existingBasePaths(loanPath);
        if (baseNodes.isEmpty()) return true;

        MovePath movePath = moveData.pathMap.get(loanPath);
        boolean[] result = {true};
        return dfcxMoves.eachBitOnEntry(element, index -> {
            Move move = moveData.moves.get(index);
            MovePath movedPath = move.path;
            if (baseNodes.stream().anyMatch(it -> it == movedPath)) {
                if (!predicate.test(move, movedPath.loanPath)) {
                    result[0] = false;
                }
            } else if (movePath != null) {
                boolean eachExtension = moveData.eachBasePath(movedPath, it -> {
                    if (it == movePath) return predicate.test(move, movedPath.loanPath);
                    return true;
                });
                if (!eachExtension) result[0] = false;
            }
            return result[0];
        });
    }

    @Nonnull
    public static FlowedMoveData buildFor(@Nonnull MoveData moveData, @Nonnull BorrowChecker.BorrowCheckContext bccx, @Nonnull ControlFlowGraph cfg) {
        DataFlowContext<MoveDataFlowOperator> dfcxMoves = new DataFlowContext<>(cfg, MoveDataFlowOperator.INSTANCE, moveData.moves.size(), FlowDirection.Forward);
        DataFlowContext<AssignDataFlowOperator> dfcxAssign = new DataFlowContext<>(cfg, AssignDataFlowOperator.INSTANCE, moveData.varAssignments.size(), FlowDirection.Forward);

        moveData.addGenKills(bccx, dfcxMoves, dfcxAssign);
        dfcxMoves.addKillsFromFlowExits();
        dfcxAssign.addKillsFromFlowExits();
        dfcxMoves.propagate();
        dfcxAssign.propagate();

        return new FlowedMoveData(moveData, dfcxMoves, dfcxAssign);
    }
}

// ---- Move ----
class Move {
    @Nonnull
    public final MovePath path;
    @Nonnull
    public final RsElement element;
    @Nonnull
    public final MoveKind kind;
    public final int index;
    @Nullable
    public final Move nextMove;

    Move(@Nonnull MovePath path, @Nonnull RsElement element, @Nonnull MoveKind kind, int index, @Nullable Move nextMove) {
        this.path = path;
        this.element = element;
        this.kind = kind;
        this.index = index;
        this.nextMove = nextMove;
    }
}

// ---- MovePath ----
class MovePath {
    @Nonnull
    public final LoanPath loanPath;
    @Nullable
    public MovePath parent;
    @Nullable
    public Move firstMove;
    @Nullable
    public MovePath firstChild;
    @Nullable
    public MovePath nextSibling;

    MovePath(@Nonnull LoanPath loanPath) {
        this(loanPath, null, null, null, null);
    }

    MovePath(@Nonnull LoanPath loanPath, @Nullable MovePath parent, @Nullable Move firstMove,
             @Nullable MovePath firstChild, @Nullable MovePath nextSibling) {
        this.loanPath = loanPath;
        this.parent = parent;
        this.firstMove = firstMove;
        this.firstChild = firstChild;
        this.nextSibling = nextSibling;
    }

    public boolean isVariablePath() {
        return parent == null;
    }
}

// ---- DataFlow Operators ----
class MoveDataFlowOperator implements DataFlow.DataFlowOperator {
    public static final MoveDataFlowOperator INSTANCE = new MoveDataFlowOperator();

    @Override
    public int join(int succ, int pred) {
        return succ | pred;
    }

    @Override
    public boolean getInitialValue() {
        return false;
    }
}

class AssignDataFlowOperator implements DataFlow.DataFlowOperator {
    public static final AssignDataFlowOperator INSTANCE = new AssignDataFlowOperator();

    @Override
    public int join(int succ, int pred) {
        return succ | pred;
    }

    @Override
    public boolean getInitialValue() {
        return false;
    }
}

// ---- Assignment ----
class Assignment {
    @Nonnull
    public final MovePath path;
    @Nonnull
    public final RsElement element;

    Assignment(@Nonnull MovePath path, @Nonnull RsElement element) {
        this.path = path;
        this.element = element;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Assignment)) return false;
        Assignment a = (Assignment) o;
        return path.equals(a.path) && element.equals(a.element);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, element);
    }
}

class LoanPathUtil {
    static boolean isUnion(@Nonnull LoanPath lp) {
        if (!(lp.ty instanceof TyAdt)) return false;
        TyAdt adt = (TyAdt) lp.ty;
        if (!(adt.getItem() instanceof RsStructItem)) return false;
        return RsStructItemUtil.getKind((RsStructItem) adt.getItem()) == RsStructKind.UNION;
    }

    static boolean isPrecise(@Nonnull LoanPath lp) {
        LoanPathKind kind = lp.kind;
        if (kind instanceof LoanPathKind.Var) return true;
        if (kind instanceof LoanPathKind.Extend) {
            LoanPathKind.Extend ext = (LoanPathKind.Extend) kind;
            LoanPathElement elem = ext.lpElement;
            if (elem instanceof LoanPathElement.Interior.Index || elem instanceof LoanPathElement.Interior.Pattern) {
                return false;
            }
            if (elem instanceof LoanPathElement.Interior.Field || elem instanceof LoanPathElement.Deref) {
                return isPrecise(ext.loanPath);
            }
        }
        if (kind instanceof LoanPathKind.Downcast) {
            return isPrecise(((LoanPathKind.Downcast) kind).loanPath);
        }
        return false;
    }
}
