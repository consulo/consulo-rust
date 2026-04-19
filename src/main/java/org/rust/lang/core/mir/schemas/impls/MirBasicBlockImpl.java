/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas.impls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.building.BlockAnd;
import org.rust.lang.core.mir.schemas.*;

import java.util.ArrayList;
import java.util.List;

public class MirBasicBlockImpl implements MirBasicBlock {
    private final int index;
    private final boolean unwind;
    @NotNull
    private final List<MirStatement> statements;
    @NotNull
    @SuppressWarnings("unchecked")
    private MirTerminator<MirBasicBlockImpl> terminator;

    /**
     * Sometimes it is needed to specify terminator source later than
     * actual terminator appears, in these cases this property comes in play.
     * In compiler this is done be specifying some dummy terminator and
     * later changing its kind.
     */
    @Nullable
    private MirSourceInfo terminatorSource;

    public MirBasicBlockImpl(int index, boolean unwind) {
        this(index, unwind, new ArrayList<>(), (MirTerminator<MirBasicBlockImpl>) MirTerminator.DUMMY);
    }

    @SuppressWarnings("unchecked")
    public MirBasicBlockImpl(int index, boolean unwind, @NotNull List<MirStatement> statements) {
        this(index, unwind, statements, (MirTerminator<MirBasicBlockImpl>) MirTerminator.DUMMY);
    }

    public MirBasicBlockImpl(int index, boolean unwind, @NotNull List<MirStatement> statements, @NotNull MirTerminator<MirBasicBlockImpl> terminator) {
        this.index = index;
        this.unwind = unwind;
        this.statements = statements;
        this.terminator = terminator;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public boolean getUnwind() {
        return unwind;
    }

    @Override
    @NotNull
    public List<MirStatement> getStatements() {
        return statements;
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public MirTerminator<MirBasicBlock> getTerminator() {
        return (MirTerminator<MirBasicBlock>) (MirTerminator<?>) terminator;
    }

    @NotNull
    public MirTerminator<MirBasicBlockImpl> getTerminatorImpl() {
        return terminator;
    }

    public void setTerminator(@NotNull MirTerminator<MirBasicBlockImpl> terminator) {
        this.terminator = terminator;
    }

    public void setTerminatorSource(@NotNull MirSourceInfo source) {
        terminatorSource = source;
    }

    @NotNull
    public BlockAnd<Void> andUnit() {
        return BlockAnd.andUnit(this);
    }

    @NotNull
    public MirBasicBlockImpl pushAssign(@NotNull MirPlace place, @NotNull MirRvalue rvalue, @NotNull MirSourceInfo source) {
        return push(new MirStatement.Assign(place, rvalue, source));
    }

    @NotNull
    public MirBasicBlockImpl pushAssignConstant(@NotNull MirPlace place, @NotNull MirConstant constant, @NotNull MirSourceInfo source) {
        return pushAssign(place, new MirRvalue.Use(new MirOperand.Constant(constant)), source);
    }

    @NotNull
    public MirBasicBlockImpl pushStorageLive(@NotNull MirLocal local, @NotNull MirSourceInfo source) {
        return push(new MirStatement.StorageLive(local, source));
    }

    @NotNull
    public MirBasicBlockImpl pushStorageDead(@NotNull MirLocal local, @NotNull MirSourceInfo source) {
        return push(new MirStatement.StorageDead(local, source));
    }

    // https://github.com/rust-lang/rust/blob/f7b831ac8a897273f78b9f47165cf8e54066ce4b/compiler/rustc_mir_build/src/build/cfg.rs#L81
    @NotNull
    public MirBasicBlockImpl pushFakeRead(
        @NotNull MirStatement.FakeRead.Cause cause,
        @NotNull MirPlace place,
        @NotNull MirSourceInfo source
    ) {
        return push(new MirStatement.FakeRead(cause, place, source));
    }

    public void terminateWithReturn(@Nullable MirSourceInfo source) {
        terminator = new MirTerminator.Return<>(getTerminatorSource(source));
    }

    public void terminateWithAssert(
        @NotNull MirOperand cond,
        boolean expected,
        @NotNull MirBasicBlockImpl block,
        @Nullable MirSourceInfo source,
        @NotNull MirAssertKind msg
    ) {
        terminator = new MirTerminator.Assert<>(
            cond,
            expected,
            block,
            msg,
            null,
            getTerminatorSource(source)
        );
    }

    // https://github.com/rust-lang/rust/blob/f7b831ac8a897273f78b9f47165cf8e54066ce4b/compiler/rustc_mir_build/src/build/cfg.rs#L121
    public void terminateWithGoto(@NotNull MirBasicBlockImpl target, @Nullable MirSourceInfo source) {
        terminator = new MirTerminator.Goto<>(target, getTerminatorSource(source));
    }

    public void terminateWithSwitchInt(
        @NotNull MirOperand discriminant,
        @NotNull MirSwitchTargets<MirBasicBlockImpl> targets,
        @Nullable MirSourceInfo source
    ) {
        terminator = new MirTerminator.SwitchInt<>(
            discriminant,
            targets,
            getTerminatorSource(source)
        );
    }

    public void terminateWithIf(
        @NotNull MirOperand cond,
        @NotNull MirBasicBlockImpl thenBlock,
        @NotNull MirBasicBlockImpl elseBlock,
        @Nullable MirSourceInfo source
    ) {
        MirSwitchTargets<MirBasicBlockImpl> targets = MirSwitchTargetsImpl.ifTargets(0, elseBlock, thenBlock);
        terminateWithSwitchInt(cond, targets, getTerminatorSource(source));
    }

    public void terminateWithFalseUnwind(
        @NotNull MirBasicBlockImpl realTarget,
        @Nullable MirBasicBlockImpl unwind,
        @Nullable MirSourceInfo source
    ) {
        terminator = new MirTerminator.FalseUnwind<>(
            realTarget,
            unwind,
            getTerminatorSource(source)
        );
    }

    /**
     * Creates a false edge to imaginaryTarget and a real edge to realTarget.
     * If imaginaryTarget is null, or is the same as the real target,
     * a Goto is generated instead to simplify the generated MIR.
     */
    public void terminateWithFalseEdges(
        @NotNull MirBasicBlockImpl realTarget,
        @Nullable MirBasicBlockImpl imaginaryTarget,
        @Nullable MirSourceInfo source
    ) {
        if (imaginaryTarget != null && imaginaryTarget != realTarget) {
            terminator = new MirTerminator.FalseEdge<>(
                realTarget,
                imaginaryTarget,
                getTerminatorSource(source)
            );
        } else {
            terminateWithGoto(realTarget, source);
        }
    }

    public void terminateWithUnreachable(@Nullable MirSourceInfo source) {
        terminator = new MirTerminator.Unreachable<>(getTerminatorSource(source));
    }

    public void terminateWithResume(@Nullable MirSourceInfo source) {
        terminator = new MirTerminator.Resume<>(getTerminatorSource(source));
    }

    public void terminateWithCall(
        @NotNull MirOperand callee,
        @NotNull List<MirOperand> args,
        @NotNull MirPlace destination,
        @Nullable MirBasicBlockImpl target,
        @Nullable MirBasicBlockImpl unwind,
        boolean fromCall,
        @Nullable MirSourceInfo source
    ) {
        terminator = new MirTerminator.Call<>(
            callee,
            args,
            destination,
            target,
            unwind,
            fromCall,
            getTerminatorSource(source)
        );
    }

    public void terminateWithDrop(
        @NotNull MirPlace place,
        @NotNull MirBasicBlockImpl target,
        @Nullable MirBasicBlockImpl unwind,
        @Nullable MirSourceInfo source
    ) {
        terminator = new MirTerminator.Drop<>(
            place,
            target,
            unwind,
            getTerminatorSource(source)
        );
    }

    @NotNull
    private MirSourceInfo getTerminatorSource(@Nullable MirSourceInfo source) {
        if (!((source == null) ^ (terminatorSource == null))) {
            if (source != null) {
                throw new IllegalArgumentException("Source can't be specified when terminator source is specified");
            } else {
                throw new IllegalArgumentException("Source must be specified if terminator source is not specified");
            }
        }
        return source != null ? source : terminatorSource;
    }

    @NotNull
    private MirBasicBlockImpl push(@NotNull MirStatement statement) {
        statements.add(statement);
        return this;
    }

    @SuppressWarnings("unchecked")
    public void unwindTerminatorTo(@NotNull MirBasicBlockImpl block) {
        MirTerminator<MirBasicBlockImpl> term = terminator;
        if (term.isDummy()) {
            throw new IllegalStateException("Terminator is expected to be specified by this moment");
        } else if (term instanceof MirTerminator.Assert) {
            MirTerminator.Assert<MirBasicBlockImpl> assertTerm = (MirTerminator.Assert<MirBasicBlockImpl>) term;
            terminator = assertTerm.copy(null, null, null, null, block, null);
        } else if (term instanceof MirTerminator.FalseUnwind) {
            MirTerminator.FalseUnwind<MirBasicBlockImpl> fuTerm = (MirTerminator.FalseUnwind<MirBasicBlockImpl>) term;
            terminator = fuTerm.copy(null, block, null);
        } else if (term instanceof MirTerminator.Call) {
            MirTerminator.Call<MirBasicBlockImpl> callTerm = (MirTerminator.Call<MirBasicBlockImpl>) term;
            terminator = callTerm.copy(null, null, null, null, block, null, null);
        } else if (term instanceof MirTerminator.Drop) {
            MirTerminator.Drop<MirBasicBlockImpl> dropTerm = (MirTerminator.Drop<MirBasicBlockImpl>) term;
            terminator = dropTerm.copy(null, null, block, null);
        } else {
            throw new IllegalStateException("Terminator is not unwindable");
        }
    }
}
