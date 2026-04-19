/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.*;

import java.util.List;
import java.util.function.BiConsumer;

public final class Forward implements Direction {
    public static final Forward INSTANCE = new Forward();

    private Forward() {
    }

    @Override
    public <Domain> void applyEffectsInBlock(
        @NotNull Analysis<Domain> analysis,
        @NotNull Domain state,
        @NotNull MirBasicBlock block
    ) {
        List<MirStatement> statements = block.getStatements();
        for (int index = 0; index < statements.size(); index++) {
            MirLocation location = new MirLocation(block, index);
            analysis.applyStatementEffect(state, statements.get(index), location);
        }

        MirLocation terminatorLocation = new MirLocation(block, statements.size());
        analysis.applyTerminatorEffect(state, block.getTerminator(), terminatorLocation);
    }

    // https://github.com/rust-lang/rust/blob/f7b831ac8a897273f78b9f47165cf8e54066ce4b/compiler/rustc_mir_dataflow/src/framework/direction.rs#L465
    @Override
    public <Domain> void joinStateIntoSuccessorsOf(
        @NotNull Analysis<Domain> analysis,
        @NotNull Domain exitState,
        @NotNull MirBasicBlock block,
        @NotNull BiConsumer<MirBasicBlock, Domain> propagate
    ) {
        MirTerminator<?> terminator = block.getTerminator();
        if (terminator instanceof MirTerminator.Return
            || terminator instanceof MirTerminator.Resume
            || terminator instanceof MirTerminator.Unreachable) {
            // No successors
        } else if (terminator instanceof MirTerminator.Goto) {
            propagate.accept(((MirTerminator.Goto) terminator).getTarget(), exitState);
        } else if (terminator instanceof MirTerminator.Assert) {
            MirTerminator.Assert assertTerm = (MirTerminator.Assert) terminator;
            if (assertTerm.getUnwind() != null) {
                propagate.accept(assertTerm.getUnwind(), exitState);
            }
            propagate.accept(assertTerm.getTarget(), exitState);
        } else if (terminator instanceof MirTerminator.Drop) {
            MirTerminator.Drop dropTerm = (MirTerminator.Drop) terminator;
            if (dropTerm.getUnwind() != null) {
                propagate.accept(dropTerm.getUnwind(), exitState);
            }
            propagate.accept(dropTerm.getTarget(), exitState);
        } else if (terminator instanceof MirTerminator.FalseUnwind) {
            MirTerminator.FalseUnwind falseUnwind = (MirTerminator.FalseUnwind) terminator;
            if (falseUnwind.getUnwind() != null) {
                propagate.accept(falseUnwind.getUnwind(), exitState);
            }
            propagate.accept(falseUnwind.getRealTarget(), exitState);
        } else if (terminator instanceof MirTerminator.FalseEdge) {
            MirTerminator.FalseEdge falseEdge = (MirTerminator.FalseEdge) terminator;
            propagate.accept(falseEdge.getRealTarget(), exitState);
            if (falseEdge.getImaginaryTarget() != null) {
                propagate.accept(falseEdge.getImaginaryTarget(), exitState);
            }
        } else if (terminator instanceof MirTerminator.SwitchInt) {
            @SuppressWarnings("unchecked")
            MirTerminator.SwitchInt<MirBasicBlock> switchInt = (MirTerminator.SwitchInt<MirBasicBlock>) terminator;
            for (MirBasicBlock target : switchInt.getTargets().getTargets()) {
                propagate.accept(target, exitState);
            }
        } else if (terminator instanceof MirTerminator.Call) {
            MirTerminator.Call call = (MirTerminator.Call) terminator;
            if (call.getUnwind() != null) {
                propagate.accept(call.getUnwind(), exitState);
            }
            if (call.getTarget() != null) {
                analysis.applyCallReturnEffect(exitState, block, call.getDestination());
                propagate.accept(call.getTarget(), exitState);
            }
        }
    }

    @Override
    public <FlowState> void visitResultsInBlock(
        @NotNull MirBasicBlock block,
        @NotNull ResultsVisitable<FlowState> results,
        @NotNull ResultsVisitor<FlowState> visitor
    ) {
        FlowState state = results.getCopyOfBlockState(block);
        visitor.visitBlockStart(state, block);

        List<MirStatement> statements = block.getStatements();
        for (int index = 0; index < statements.size(); index++) {
            MirStatement statement = statements.get(index);
            MirLocation location = new MirLocation(block, index);
            results.reconstructBeforeStatementEffect(state, statement, location);
            visitor.visitStatementBeforePrimaryEffect(state, statement, location);
            results.reconstructStatementEffect(state, statement, location);
            visitor.visitStatementAfterPrimaryEffect(state, statement, location);
        }

        MirLocation terminatorLocation = new MirLocation(block, statements.size());
        results.reconstructBeforeTerminatorEffect(state, block.getTerminator(), terminatorLocation);
        visitor.visitTerminatorBeforePrimaryEffect(state, block.getTerminator(), terminatorLocation);
        results.reconstructTerminatorEffect(state, block.getTerminator(), terminatorLocation);
        visitor.visitTerminatorAfterPrimaryEffect(state, block.getTerminator(), terminatorLocation);

        visitor.visitBlockEnd(state, block);
    }
}
