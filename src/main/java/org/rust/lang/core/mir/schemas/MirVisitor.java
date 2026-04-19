/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.ty.Ty;

import java.util.Collections;
import java.util.List;

// https://github.com/rust-lang/rust/blob/f7b831ac8a897273f78b9f47165cf8e54066ce4b/compiler/rustc_middle/src/mir/visit.rs#L1206
public interface MirVisitor {
    @NotNull
    MirLocal returnPlace();

    default void visitBody(@NotNull MirBody body) {
        // TODO: process body.generator

        for (MirBasicBlock block : body.getBasicBlocks()) {
            visitBasicBlock(block);
        }

        for (MirSourceScope scope : body.getSourceScopes()) {
            visitSourceScope(scope);
        }

        // TODO: process body.returnTy

        for (MirLocal local : body.getLocalDecls()) {
            visitLocalDecl(local);
        }

        // TODO: process body.userTypeAnnotations

        for (MirVarDebugInfo varDebugInfo : body.getVarDebugInfo()) {
            visitVarDebugInfo(body, varDebugInfo);
        }

        visitSpan(body.getSpan());

        // TODO: process body.requiredConsts
    }

    default void visitBasicBlock(@NotNull MirBasicBlock block) {
        List<MirStatement> statements = block.getStatements();
        for (int index = 0; index < statements.size(); index++) {
            MirLocation location = new MirLocation(block, index);
            visitStatement(statements.get(index), location);
        }

        visitTerminator(block.getTerminator(), block.getTerminatorLocation());
    }

    default void visitSourceScope(@NotNull MirSourceScope scope) {
        visitSpan(scope.getSpan());
        MirSourceScope parentScope = scope.getParentScope();
        if (parentScope != null) {
            visitSourceScope(parentScope);
        }
        // TODO: process scope.inlined
        // TODO: process scope.inlinedParentScope
    }

    default void visitLocalDecl(@NotNull MirLocal local) {
        visitTy(local.getTy(), new TyContext.LocalDecl(local, local.getSource()));
        // TODO: process local.userTy
        visitSourceInfo(local.getSource());
    }

    default void visitVarDebugInfo(@NotNull MirBody body, @NotNull MirVarDebugInfo varDebugInfo) {
        visitSourceInfo(varDebugInfo.getSource());

        MirBasicBlock firstBlock = null;
        for (MirBasicBlock block : body.getBasicBlocks()) {
            if (block.getIndex() == 0) {
                firstBlock = block;
                break;
            }
        }
        if (firstBlock == null) return;
        MirLocation location = new MirLocation(firstBlock, 0);

        MirVarDebugInfo.Contents value = varDebugInfo.getContents();
        if (value instanceof MirVarDebugInfo.Contents.Constant) {
            visitConstant(((MirVarDebugInfo.Contents.Constant) value).getConstant(), location);
        } else if (value instanceof MirVarDebugInfo.Contents.Place) {
            visitPlace(((MirVarDebugInfo.Contents.Place) value).getPlace(), MirPlaceContext.NonUse.VarDebugInfo.INSTANCE, location);
        } else if (value instanceof MirVarDebugInfo.Contents.Composite) {
            MirVarDebugInfo.Contents.Composite composite = (MirVarDebugInfo.Contents.Composite) value;
            visitTy(composite.getTy(), new TyContext.Location(location));
            for (MirVarDebugInfo.Fragment fragment : composite.getFragments()) {
                visitPlace(fragment.getContents(), MirPlaceContext.NonUse.VarDebugInfo.INSTANCE, location);
            }
        }
    }

    default void visitPlace(@NotNull MirPlace place, @NotNull MirPlaceContext context, @NotNull MirLocation location) {
        MirPlaceContext currentContext = context;
        if (!place.getProjections().isEmpty()) {
            if (currentContext.isUse()) {
                if (currentContext.isMutatingUse()) {
                    currentContext = MirPlaceContext.MutatingUse.Projection.INSTANCE;
                } else {
                    currentContext = MirPlaceContext.NonMutatingUse.Projection.INSTANCE;
                }
            }
        }
        visitLocal(place.getLocal(), currentContext, location);
        visitProjection(place, currentContext, location);
    }

    default void visitProjection(@NotNull MirPlace place, @NotNull MirPlaceContext context, @NotNull MirLocation location) {
        List<MirProjectionElem<Ty>> projections = place.getProjections();
        for (int i = projections.size() - 1; i >= 0; i--) {
            visitProjectionElem(projections.get(i), location);
        }
    }

    default void visitProjectionElem(@NotNull MirProjectionElem<Ty> elem, @NotNull MirLocation location) {
        if (elem instanceof MirProjectionElem.Field) {
            MirProjectionElem.Field<Ty> field = (MirProjectionElem.Field<Ty>) elem;
            visitTy(field.getElem(), new TyContext.Location(location));
        } else if (elem instanceof MirProjectionElem.Index) {
            MirProjectionElem.Index<Ty> index = (MirProjectionElem.Index<Ty>) elem;
            visitLocal(index.getIndex(), MirPlaceContext.NonMutatingUse.Copy.INSTANCE, location);
        }
        // Deref, ConstantIndex, Downcast -> no-op
    }

    default void visitLocal(@NotNull MirLocal local, @NotNull MirPlaceContext context, @NotNull MirLocation location) {
    }

    default void visitConstant(@NotNull MirConstant constant, @NotNull MirLocation location) {
        visitSpan(constant.getSpan());
        // TODO: process constant.literal
    }

    default void visitSpan(@NotNull MirSpan span) {
    }

    default void visitStatement(@NotNull MirStatement statement, @NotNull MirLocation location) {
        visitSourceInfo(statement.getSource());
        if (statement instanceof MirStatement.Assign) {
            MirStatement.Assign assign = (MirStatement.Assign) statement;
            visitAssign(assign.getPlace(), assign.getRvalue(), location);
        } else if (statement instanceof MirStatement.FakeRead) {
            MirStatement.FakeRead fakeRead = (MirStatement.FakeRead) statement;
            visitPlace(fakeRead.getPlace(), MirPlaceContext.NonMutatingUse.Inspect.INSTANCE, location);
        } else if (statement instanceof MirStatement.StorageDead) {
            visitLocal(((MirStatement.StorageDead) statement).getLocal(), MirPlaceContext.NonUse.StorageLive.INSTANCE, location);
        } else if (statement instanceof MirStatement.StorageLive) {
            visitLocal(((MirStatement.StorageLive) statement).getLocal(), MirPlaceContext.NonUse.StorageDead.INSTANCE, location);
        }
    }

    default void visitAssign(@NotNull MirPlace place, @NotNull MirRvalue rvalue, @NotNull MirLocation location) {
        visitPlace(place, MirPlaceContext.MutatingUse.Store.INSTANCE, location);
        visitRvalue(rvalue, location);
    }

    default void visitRvalue(@NotNull MirRvalue rvalue, @NotNull MirLocation location) {
        if (rvalue instanceof MirRvalue.Use) {
            visitOperand(((MirRvalue.Use) rvalue).getOperand(), location);
        } else if (rvalue instanceof MirRvalue.Repeat) {
            MirRvalue.Repeat repeat = (MirRvalue.Repeat) rvalue;
            visitOperand(repeat.getOperand(), location);
            visitTyConst(repeat.getCount(), location);
        } else if (rvalue instanceof MirRvalue.ThreadLocalRef) {
            // no-op
        } else if (rvalue instanceof MirRvalue.Ref) {
            MirRvalue.Ref ref = (MirRvalue.Ref) rvalue;
            // TODO: visitRegion(rvalue.region, location)
            MirPlaceContext context;
            MirBorrowKind borrowKind = ref.getBorrowKind();
            if (borrowKind instanceof MirBorrowKind.Shared) {
                context = MirPlaceContext.NonMutatingUse.SharedBorrow.INSTANCE;
            } else if (borrowKind instanceof MirBorrowKind.Shallow) {
                context = MirPlaceContext.NonMutatingUse.ShallowBorrow.INSTANCE;
            } else if (borrowKind instanceof MirBorrowKind.Unique) {
                context = MirPlaceContext.NonMutatingUse.UniqueBorrow.INSTANCE;
            } else if (borrowKind instanceof MirBorrowKind.Mut) {
                context = MirPlaceContext.MutatingUse.Borrow.INSTANCE;
            } else {
                throw new IllegalStateException("Unknown borrow kind: " + borrowKind);
            }
            visitPlace(ref.getPlace(), context, location);
        } else if (rvalue instanceof MirRvalue.CopyForDeref) {
            throw new UnsupportedOperationException("TODO");
        } else if (rvalue instanceof MirRvalue.AddressOf) {
            throw new UnsupportedOperationException("TODO");
        } else if (rvalue instanceof MirRvalue.BinaryOpUse) {
            MirRvalue.BinaryOpUse binOp = (MirRvalue.BinaryOpUse) rvalue;
            visitOperand(binOp.getLeft(), location);
            visitOperand(binOp.getRight(), location);
        } else if (rvalue instanceof MirRvalue.CheckedBinaryOpUse) {
            MirRvalue.CheckedBinaryOpUse checkedBinOp = (MirRvalue.CheckedBinaryOpUse) rvalue;
            visitOperand(checkedBinOp.getLeft(), location);
            visitOperand(checkedBinOp.getRight(), location);
        } else if (rvalue instanceof MirRvalue.UnaryOpUse) {
            visitOperand(((MirRvalue.UnaryOpUse) rvalue).getOperand(), location);
        } else if (rvalue instanceof MirRvalue.Discriminant) {
            visitPlace(((MirRvalue.Discriminant) rvalue).getPlace(), MirPlaceContext.NonMutatingUse.Inspect.INSTANCE, location);
        } else if (rvalue instanceof MirRvalue.NullaryOpUse) {
            throw new UnsupportedOperationException("TODO");
        } else if (rvalue instanceof MirRvalue.Aggregate) {
            MirRvalue.Aggregate aggregate = (MirRvalue.Aggregate) rvalue;
            if (aggregate instanceof MirRvalue.Aggregate.Adt) {
                // TODO: visitSubsts(rvalue.substs, location)
            } else if (aggregate instanceof MirRvalue.Aggregate.Array) {
                visitTy(((MirRvalue.Aggregate.Array) aggregate).getTy(), new TyContext.Location(location));
            }
            // Tuple -> no-op
            for (MirOperand operand : aggregate.getOperands()) {
                visitOperand(operand, location);
            }
        } else if (rvalue instanceof MirRvalue.Len) {
            visitPlace(((MirRvalue.Len) rvalue).getPlace(), MirPlaceContext.NonMutatingUse.Inspect.INSTANCE, location);
        } else {
            throw new UnsupportedOperationException("TODO");
        }
    }

    default void visitTerminator(@NotNull MirTerminator<MirBasicBlock> terminator, @NotNull MirLocation location) {
        visitSourceInfo(terminator.getSource());
        if (terminator instanceof MirTerminator.Assert) {
            MirTerminator.Assert<?> assertTerminator = (MirTerminator.Assert<?>) terminator;
            visitOperand(assertTerminator.getCond(), location);
            visitAssertMessage(assertTerminator.getMsg(), location);
        } else if (terminator instanceof MirTerminator.Return) {
            visitLocal(returnPlace(), MirPlaceContext.NonMutatingUse.Move.INSTANCE, location);
        } else if (terminator instanceof MirTerminator.SwitchInt) {
            visitOperand(((MirTerminator.SwitchInt<?>) terminator).getDiscriminant(), location);
        } else if (terminator instanceof MirTerminator.Drop) {
            visitPlace(((MirTerminator.Drop<?>) terminator).getPlace(), MirPlaceContext.MutatingUse.Drop.INSTANCE, location);
        } else if (terminator instanceof MirTerminator.Goto
            || terminator instanceof MirTerminator.Resume
            || terminator instanceof MirTerminator.Unreachable
            || terminator instanceof MirTerminator.FalseEdge
            || terminator instanceof MirTerminator.FalseUnwind) {
            // no-op
        } else if (terminator instanceof MirTerminator.Call) {
            MirTerminator.Call<?> call = (MirTerminator.Call<?>) terminator;
            visitOperand(call.getCallee(), location);
            for (MirOperand arg : call.getArgs()) {
                visitOperand(arg, location);
            }
            visitPlace(call.getDestination(), MirPlaceContext.MutatingUse.Call.INSTANCE, location);
        }
    }

    default void visitOperand(@NotNull MirOperand operand, @NotNull MirLocation location) {
        if (operand instanceof MirOperand.Copy) {
            visitPlace(((MirOperand.Copy) operand).getPlace(), MirPlaceContext.NonMutatingUse.Copy.INSTANCE, location);
        } else if (operand instanceof MirOperand.Move) {
            visitPlace(((MirOperand.Move) operand).getPlace(), MirPlaceContext.NonMutatingUse.Move.INSTANCE, location);
        } else if (operand instanceof MirOperand.Constant) {
            visitConstant(((MirOperand.Constant) operand).getConstant(), location);
        }
    }

    default void visitAssertMessage(@NotNull MirAssertKind msg, @NotNull MirLocation location) {
        if (msg instanceof MirAssertKind.Overflow) {
            MirAssertKind.Overflow overflow = (MirAssertKind.Overflow) msg;
            visitOperand(overflow.getLeft(), location);
            visitOperand(overflow.getRight(), location);
        } else if (msg instanceof MirAssertKind.OverflowNeg) {
            visitOperand(((MirAssertKind.OverflowNeg) msg).getArg(), location);
        } else if (msg instanceof MirAssertKind.DivisionByZero) {
            visitOperand(((MirAssertKind.DivisionByZero) msg).getArg(), location);
        } else if (msg instanceof MirAssertKind.ReminderByZero) {
            visitOperand(((MirAssertKind.ReminderByZero) msg).getArg(), location);
        } else if (msg instanceof MirAssertKind.BoundsCheck) {
            MirAssertKind.BoundsCheck boundsCheck = (MirAssertKind.BoundsCheck) msg;
            visitOperand(boundsCheck.getLen(), location);
            visitOperand(boundsCheck.getIndex(), location);
        }
    }

    default void visitSourceInfo(@NotNull MirSourceInfo source) {
        visitSpan(source.getSpan());
        visitSourceScope(source.getScope());
    }

    default void visitTy(@NotNull Ty ty, @NotNull TyContext context) {
    }

    default void visitTyConst(@NotNull Const aConst, @NotNull MirLocation location) {
    }
}
