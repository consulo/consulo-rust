/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir;

import com.intellij.openapi.util.Ref;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.building.*;
import org.rust.lang.core.mir.schemas.*;
import org.rust.lang.core.mir.schemas.impls.MirBasicBlockImpl;
import org.rust.lang.core.mir.schemas.impls.MirBodyImpl;
import org.rust.lang.core.mir.schemas.impls.MirSwitchTargetsImpl;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.thir.*;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtValue;
import org.rust.lang.core.types.regions.Scope;
import org.rust.lang.core.types.regions.ScopeTree;
import org.rust.lang.core.types.ty.*;
import org.rust.openapiext.TestAssertUtil;
import com.intellij.openapi.util.Pair;

import java.util.*;
import java.util.function.Supplier;

/**
 *
 * instance methods that take a BlockAnd as the first parameter. The inline functions
 * patterns have been converted to explicit static method calls.
 */
public class MirBuilder {
    @NotNull
    private final RsElement element;
    @NotNull
    private final ImplLookup implLookup;
    @NotNull
    private final MirrorContext mirrorContext;
    private final boolean checkOverflow;
    @NotNull
    private final MirSpan span;
    private final int argCount;

    @NotNull
    private ScopeTree getRegionScopeTree() {
        return mirrorContext.regionScopeTree;
    }

    @NotNull
    private final BasicBlocksBuilder basicBlocks = new BasicBlocksBuilder();
    @NotNull
    private final Scopes scopes = new Scopes();
    @NotNull
    private final SourceScopesBuilder sourceScopes;
    @NotNull
    private final LocalsBuilder localDecls;
    @NotNull
    private final List<MirVarDebugInfo> varDebugInfo = new ArrayList<>();
    @NotNull
    private final Map<LocalVar, MirLocalForNode> varIndices = new HashMap<>();

    @Nullable
    private MirPlace unitTemp;

    @NotNull
    private MirPlace getUnitTemp() {
        if (unitTemp == null) {
            unitTemp = localDecls
                .newLocal(TyUnit.INSTANCE, outermostSourceInfo(span))
                .intoPlace();
        }
        return unitTemp;
    }

    private MirBuilder(
        @NotNull RsElement element,
        @NotNull ImplLookup implLookup,
        @NotNull MirrorContext mirrorContext,
        boolean checkOverflow,
        @NotNull MirSpan span,
        int argCount,
        @NotNull Ty returnTy,
        @NotNull MirSpan returnSpan
    ) {
        this.element = element;
        this.implLookup = implLookup;
        this.mirrorContext = mirrorContext;
        this.checkOverflow = checkOverflow;
        this.span = span;
        this.argCount = argCount;
        this.sourceScopes = new SourceScopesBuilder(span);
        this.localDecls = new LocalsBuilder(returnTy, outermostSourceInfo(returnSpan));
    }

    @NotNull
    public MirBody buildFunction(@NotNull Thir thir, @NotNull RsElement body) {
        inScope(new Scope.CallSite(body), () -> {
            MirSpan fnEndSpan = span.getEnd();
            BlockAnd<Void> returnBlockAnd = inBreakableScope(null, localDecls.getReturnPlace(), fnEndSpan, () -> {
                return inScope(new Scope.Arguments(body), () -> {
                    return argsAndBody(
                        basicBlocks.startBlock().andUnit(),
                        thir.getExpr(), thir.getParams(), new Scope.Arguments(body)
                    );
                });
            });
            returnBlockAnd.getBlock().terminateWithReturn(sourceInfo(fnEndSpan));
            buildDropTrees();
            return returnBlockAnd;
        });
        return finish();
    }

    @NotNull
    public MirBody buildConstant(@NotNull Thir thir) {
        exprIntoPlace(
            basicBlocks.startBlock().andUnit(),
            thir.getExpr(),
            localDecls.getReturnPlace()
        ).getBlock().terminateWithReturn(sourceInfo(MirUtils.asSpan(element)));
        buildDropTrees();
        return finish();
    }

    @NotNull
    private MirBody finish() {
        return new MirBodyImpl(
            element,
            basicBlocks.build(),
            localDecls.build(),
            span,
            sourceScopes.build(),
            argCount,
            varDebugInfo
        );
    }

    // TODO: captured values
    @NotNull
    private BlockAnd<Void> argsAndBody(
        @NotNull BlockAnd<?> blockAnd,
        @NotNull ThirExpr expr,
        @NotNull List<ThirParam> arguments,
        @NotNull Scope argumentScope
    ) {
        for (int index = 0; index < arguments.size(); index++) {
            ThirParam param = arguments.get(index);
            MirSourceInfo sourceInfo = outermostSourceInfo(param.getPat() != null ? param.getPat().getSource() : span);
            MirPlace argLocal = localDecls.newLocal(param.getTy(), sourceInfo).intoPlace();
            if (param.getPat() != null) {
                String name = ThirPat.getSimpleIdent(param.getPat());
                if (name != null) {
                    MirVarDebugInfo info = new MirVarDebugInfo(
                        name,
                        sourceInfo,
                        new MirVarDebugInfo.Contents.Place(argLocal),
                        index + 1
                    );
                    varDebugInfo.add(info);
                }
            }
        }
        // TODO: insert upvars
        MirSourceScope scope = null;
        for (int index = 0; index < arguments.size(); index++) {
            ThirParam param = arguments.get(index);
            MirLocal local = localDecls.get(index + 1);
            scheduleDrop(argumentScope, local, Drop.Kind.VALUE);
            if (param.getPat() == null) continue;
            MirSourceScope originalSourceScope = sourceScopes.getSourceScope();
            // TODO: set_correct_source_scope_for_arg
            if (param.getPat() instanceof ThirPat.Binding
                && ((ThirPat.Binding) param.getPat()).getMode() instanceof ThirBindingMode.ByValue
                && ((ThirPat.Binding) param.getPat()).getSubpattern() == null) {
                ThirPat.Binding binding = (ThirPat.Binding) param.getPat();
                MirLocalInfo localInfo;
                if (param.getSelfKind() != null) {
                    localInfo = new MirLocalInfo.User(new MirClearCrossCrate.Set<>(new MirBindingForm.ImplicitSelf(param.getSelfKind())));
                } else {
                    localInfo = new MirLocalInfo.User(
                        new MirClearCrossCrate.Set<>(
                            new MirBindingForm.Var(
                                new MirVarBindingForm(
                                    new MirBindingMode.BindByValue(binding.getMutability()),
                                    param.getTySpan(),
                                    new Pair<>(null, binding.getSource()),
                                    binding.getSource()
                                )
                            )
                        )
                    );
                }
                localDecls.update(
                    index + 1,
                    binding.getMutability(),
                    new MirSourceInfo(local.getSource().getSpan(), sourceScopes.getSourceScope()),
                    localInfo
                );
                varIndices.put(binding.getVariable(), new MirLocalForNode.One(localDecls.get(index + 1)));
            } else {
                throw new UnsupportedOperationException("TODO");
            }
            sourceScopes.setSourceScope(originalSourceScope);
        }
        if (scope != null) {
            sourceScopes.setSourceScope(scope);
        }
        return exprIntoPlace(blockAnd, expr, localDecls.getReturnPlace());
    }

    private void buildDropTrees() {
        // TODO: something about generator
        buildUnwindTree();
    }

    private void buildUnwindTree() {
        Map<DropTree.DropNode, MirBasicBlockImpl> blocks = scopes.getUnwindDrops().buildMir(new Unwind(basicBlocks), null);
        MirBasicBlockImpl rootBlock = blocks.get(scopes.getUnwindDrops().getRoot());
        if (rootBlock != null) {
            rootBlock.terminateWithResume(outermostSourceInfo(span));
        }
    }

    // https://github.com/rust-lang/rust/blob/f7b831ac8a897273f78b9f47165cf8e54066ce4b/compiler/rustc_mir_build/src/build/expr/into.rs#L18
    @NotNull
    private BlockAnd<Void> exprIntoPlace(@NotNull BlockAnd<?> blockAndIn, @NotNull ThirExpr expr, @NotNull MirPlace place) {
        MirBasicBlockImpl block = blockAndIn.getBlock();
        MirSourceInfo source = sourceInfo(expr.getSpan());

        if (expr instanceof ThirExpr.Unary
            || expr instanceof ThirExpr.Binary
            || expr instanceof ThirExpr.BoxExpr
            || expr instanceof ThirExpr.Cast
            || expr instanceof ThirExpr.Pointer
            || expr instanceof ThirExpr.Repeat
            || expr instanceof ThirExpr.Array
            || expr instanceof ThirExpr.Tuple
            || expr instanceof ThirExpr.Closure
            || expr instanceof ThirExpr.ConstBlock
            || expr instanceof ThirExpr.Literal
            || expr instanceof ThirExpr.NamedConst
            || expr instanceof ThirExpr.NonHirLiteral
            || expr instanceof ThirExpr.ZstLiteral
            || expr instanceof ThirExpr.ConstParam
            || expr instanceof ThirExpr.ThreadLocalRef
            || expr instanceof ThirExpr.StaticRef
            || expr instanceof ThirExpr.OffsetOf) {
            BlockAnd<MirRvalue> rvalueResult = toLocalRvalue(blockAndIn, expr);
            return rvalueResult.getBlock().pushAssign(place, rvalueResult.getElem(), source).andUnit();
        } else if (expr instanceof ThirExpr.ScopeExpr) {
            ThirExpr.ScopeExpr scopeExpr = (ThirExpr.ScopeExpr) expr;
            return inScope(scopeExpr.getRegionScope(), () -> exprIntoPlace(blockAndIn, scopeExpr.getExpr(), place));
        } else if (expr instanceof ThirExpr.Block) {
            return astBlockIntoPlace(blockAndIn, ((ThirExpr.Block) expr).getBlock(), place, expr.getSpan());
        } else if (expr instanceof ThirExpr.Logical) {
            ThirExpr.Logical logicalExpr = (ThirExpr.Logical) expr;
            MirBasicBlockImpl shortcircuitBlock = basicBlocks.newBlock();
            MirBasicBlockImpl elseBlock = basicBlocks.newBlock();
            MirBasicBlockImpl joinBlock = basicBlocks.newBlock();
            BlockAnd<MirOperand> operandResult = toLocalOperand(blockAndIn, logicalExpr.getLeft());
            if (logicalExpr.getOp() == LogicOp.AND) {
                operandResult.getBlock().terminateWithIf(operandResult.getElem(), elseBlock, shortcircuitBlock, source);
            } else {
                operandResult.getBlock().terminateWithIf(operandResult.getElem(), shortcircuitBlock, elseBlock, source);
            }
            MirConstant shortcircuitValue;
            if (logicalExpr.getOp() == LogicOp.AND) {
                shortcircuitValue = toConstant(false, TyBool.INSTANCE, expr.getSpan());
            } else {
                shortcircuitValue = toConstant(true, TyBool.INSTANCE, expr.getSpan());
            }
            shortcircuitBlock.pushAssign(place, new MirRvalue.Use(new MirOperand.Constant(shortcircuitValue)), source);
            shortcircuitBlock.terminateWithGoto(joinBlock, source);
            BlockAnd<MirOperand> rightResult = toLocalOperand(elseBlock.andUnit(), logicalExpr.getRight());
            rightResult.getBlock().pushAssign(place, new MirRvalue.Use(rightResult.getElem()), source);
            rightResult.getBlock().terminateWithGoto(joinBlock, source);
            return joinBlock.andUnit();
        } else if (expr instanceof ThirExpr.If) {
            ThirExpr.If ifExpr = (ThirExpr.If) expr;
            Scope conditionScope = localScope();

            BlockAnd<MirBasicBlockImpl> thenAndElse = inScope(ifExpr.getIfThenScope(), () -> {
                MirSourceInfo sourceInfoVal;
                if (isLet(ifExpr.getCond())) {
                    MirSourceScope variableScope = sourceScopes.newSourceScope(ifExpr.getThen().getSpan());
                    sourceScopes.setSourceScope(variableScope);
                    sourceInfoVal = new MirSourceInfo(ifExpr.getThen().getSpan(), variableScope);
                } else {
                    sourceInfoVal = sourceInfo(ifExpr.getThen().getSpan());
                }

                return inIfThenScope(conditionScope, ifExpr.getThen().getSpan(), () -> {
                    BlockAnd<Void> thenResult = thenElseBreak(blockAndIn, ifExpr.getCond(), conditionScope, conditionScope, sourceInfoVal);
                    return exprIntoPlace(thenResult, ifExpr.getThen(), place);
                });
            });

            MirBasicBlockImpl thenBlock = thenAndElse.getBlock();
            MirBasicBlockImpl elseBlock = thenAndElse.getElem();

            MirBasicBlockImpl elseEnd;
            if (ifExpr.getElseExpr() != null) {
                elseEnd = exprIntoPlace(elseBlock.andUnit(), ifExpr.getElseExpr(), place).getBlock();
            } else {
                pushAssignUnit(elseBlock, place, sourceInfo(expr.getSpan().getEnd()));
                elseEnd = elseBlock;
            }

            MirBasicBlockImpl joinBlock = basicBlocks.newBlock();
            thenBlock.terminateWithGoto(joinBlock, source);
            elseEnd.terminateWithGoto(joinBlock, source);
            return joinBlock.andUnit();
        } else if (expr instanceof ThirExpr.Loop) {
            ThirExpr.Loop loopExpr = (ThirExpr.Loop) expr;
            MirBasicBlockImpl loopBlock = basicBlocks.newBlock();
            block.terminateWithGoto(loopBlock, source);
            return inBreakableScope(loopBlock, place, expr.getSpan(), () -> {
                MirBasicBlockImpl bodyBlock = basicBlocks.newBlock();
                loopBlock.terminateWithFalseUnwind(bodyBlock, null, source);
                divergeFrom(loopBlock);
                BlockAnd<Void> bodyBlockEnd = exprIntoPlace(bodyBlock.andUnit(), loopExpr.getBody(), getUnitTemp());
                bodyBlockEnd.getBlock().terminateWithGoto(loopBlock, source);
                return null;
            });
        } else if (expr instanceof ThirExpr.Call) {
            ThirExpr.Call callExpr = (ThirExpr.Call) expr;
            BlockAnd<MirOperand> calleeResult = toLocalOperand(blockAndIn, callExpr.getCallee());
            MirOperand callee = calleeResult.getElem();
            BlockAnd<?> current = calleeResult;
            List<MirOperand> args = new ArrayList<>();
            for (ThirExpr arg : callExpr.getArgs()) {
                BlockAnd<MirOperand> argResult = toLocalCallOperand(current, arg);
                current = argResult;
                args.add(argResult.getElem());
            }
            MirBasicBlockImpl success = basicBlocks.newBlock();
            // TODO record_operands_moved(args)
            current.getBlock().terminateWithCall(
                callee, args, place, success, null, callExpr.getFromCall(), source
            );
            divergeFrom(current.getBlock());
            return success.andUnit();
        } else if (expr instanceof ThirExpr.NeverToAny) {
            ThirExpr.NeverToAny neverExpr = (ThirExpr.NeverToAny) expr;
            MirBasicBlockImpl newBlock = toTemp(blockAndIn, neverExpr.getSpanExpr(), scopes.topmost(), Mutability.MUTABLE).getBlock();
            newBlock.terminateWithUnreachable(source);
            return basicBlocks.newBlock().andUnit();
        } else if (expr instanceof ThirExpr.Continue || expr instanceof ThirExpr.Break || expr instanceof ThirExpr.Return) {
            return statementExpr(blockAndIn, expr, null);
        } else if (expr instanceof ThirExpr.VarRef
            || expr instanceof ThirExpr.UpvarRef
            || expr instanceof ThirExpr.PlaceTypeAscription
            || expr instanceof ThirExpr.ValueTypeAscription
            || expr instanceof ThirExpr.Index
            || expr instanceof ThirExpr.Deref
            || expr instanceof ThirExpr.Field) {
            if (expr instanceof ThirExpr.Field) {
                if (!place.getProjections().isEmpty()) {
                    // TODO Do we really need it?
                    // localDecls.newLocal(..., expr.ty, expr.span)
                }
            }
            BlockAnd<MirPlace> placeResult = toPlace(blockAndIn, expr);
            MirOperand operand = consumeByCopyOrMove(placeResult.getElem());
            MirRvalue rvalue = new MirRvalue.Use(operand);
            placeResult.getBlock().pushAssign(place, rvalue, source);
            return placeResult.getBlock().andUnit();
        } else if (expr instanceof ThirExpr.Assign || expr instanceof ThirExpr.AssignOp) {
            BlockAnd<Void> result = statementExpr(blockAndIn, expr, null);
            pushAssignUnit(blockAndIn.getBlock(), place, source);
            return result;
        } else if (expr instanceof ThirExpr.Adt) {
            ThirExpr.Adt adtExpr = (ThirExpr.Adt) expr;
            // first process the set of fields that were provided (evaluating them in order given by user)
            MirBasicBlockImpl currentBlock = block;
            Map<Integer, MirOperand> fieldsMap = new LinkedHashMap<>();
            for (FieldExpr fieldExpr : adtExpr.getFields()) {
                BlockAnd<MirOperand> blockAndOperand = toOperand(currentBlock.andUnit(), fieldExpr.getExpr(), scopes.topmost(), NeedsTemporary.Maybe);
                currentBlock = blockAndOperand.getBlock();
                fieldsMap.put(fieldExpr.getName(), blockAndOperand.getElem());
            }
            int fieldCount = RsFieldsOwnerExtUtil.getSize(ThirUtilUtil.variant(adtExpr.getDefinition(), adtExpr.getVariantIndex()));
            List<Integer> fieldsNames = new ArrayList<>();
            for (int i = 0; i < fieldCount; i++) {
                fieldsNames.add(i);
            }
            List<MirOperand> fields;
            if (adtExpr.getBase() != null) {
                throw new UnsupportedOperationException("TODO");
            } else {
                fields = new ArrayList<>();
                for (int idx : fieldsNames) {
                    MirOperand op = fieldsMap.get(idx);
                    if (op == null) {
                        throw new IllegalStateException("Mismatched fields in struct literal and definition");
                    }
                    fields.add(op);
                }
            }
            MirRvalue rvalue = new MirRvalue.Aggregate.Adt(adtExpr.getDefinition(), adtExpr.getVariantIndex(), adtExpr.getTy(), fields);
            currentBlock.pushAssign(place, rvalue, source);
            return currentBlock.andUnit();
        } else if (expr instanceof ThirExpr.Use) {
            return exprIntoPlace(blockAndIn, ((ThirExpr.Use) expr).getSource(), place);
        } else if (expr instanceof ThirExpr.Borrow) {
            ThirExpr.Borrow borrowExpr = (ThirExpr.Borrow) expr;
            BlockAnd<MirPlace> borrowResult;
            if (borrowExpr.getKind() instanceof MirBorrowKind.Shared) {
                borrowResult = toReadOnlyPlace(blockAndIn, borrowExpr.getArg());
            } else {
                borrowResult = toPlace(blockAndIn, borrowExpr.getArg());
            }
            MirRvalue borrow = new MirRvalue.Ref(borrowExpr.getKind(), borrowResult.getElem());
            borrowResult.getBlock().pushAssign(place, borrow, source);
            return borrowResult.getBlock().andUnit();
        } else if (expr instanceof ThirExpr.Match) {
            ThirExpr.Match matchExpr = (ThirExpr.Match) expr;
            return matchExpr(blockAndIn, place, expr.getSpan(), matchExpr.getExpr(), matchExpr.getArms());
        } else if (expr instanceof ThirExpr.Let) {
            ThirExpr.Let letExpr = (ThirExpr.Let) expr;
            Scope letScope = localScope();
            BlockAnd<MirBasicBlockImpl> thenAndFalse = inIfThenScope(letScope, expr.getSpan(), () -> {
                return lowerLetExpr(blockAndIn, letExpr.getExpr(), letExpr.getPat(), letScope, null, expr.getSpan(), true);
            });

            MirBasicBlockImpl trueBlock = thenAndFalse.getBlock();
            MirBasicBlockImpl falseBlock = thenAndFalse.getElem();

            MirConstValue trueConstValue = new MirConstValue.Scalar(MirScalar.from(true));
            MirConstant trueConstant = new MirConstant.Value(trueConstValue, TyBool.INSTANCE, expr.getSpan());
            trueBlock.pushAssignConstant(place, trueConstant, source);

            MirConstValue falseConstValue = new MirConstValue.Scalar(MirScalar.from(false));
            MirConstant falseConstant = new MirConstant.Value(falseConstValue, TyBool.INSTANCE, expr.getSpan());
            falseBlock.pushAssignConstant(place, falseConstant, source);

            MirBasicBlockImpl joinBlock = basicBlocks.newBlock();
            trueBlock.terminateWithGoto(joinBlock, source);
            falseBlock.terminateWithGoto(joinBlock, source);
            return joinBlock.andUnit();
        } else {
            throw new UnsupportedOperationException("TODO: " + expr.getClass().getSimpleName());
        }
    }

    @NotNull
    public MirPlace temp(@NotNull Ty ty, @NotNull MirSpan span) {
        MirLocal temp = localDecls.newLocal(true, ty, outermostSourceInfo(span));
        return new MirPlace(temp);
    }

    @NotNull
    private MirOperand consumeByCopyOrMove(@NotNull MirPlace place) {
        if (TyUtil.isMovesByDefault(place.ty().getTy(), implLookup)) {
            return new MirOperand.Move(place);
        } else {
            return new MirOperand.Copy(place);
        }
    }

    @NotNull
    private BlockAnd<MirPlace> toReadOnlyPlace(@NotNull BlockAnd<?> blockAnd, @NotNull ThirExpr expr) {
        BlockAnd<PlaceBuilder> result = toReadOnlyPlaceBuilder(blockAnd, expr);
        return new BlockAnd<>(result.getBlock(), result.getElem().toPlace());
    }

    @NotNull
    private BlockAnd<MirPlace> toPlace(@NotNull BlockAnd<?> blockAnd, @NotNull ThirExpr expr) {
        BlockAnd<PlaceBuilder> result = toPlaceBuilder(blockAnd, expr);
        return new BlockAnd<>(result.getBlock(), result.getElem().toPlace());
    }

    @NotNull
    private BlockAnd<PlaceBuilder> toReadOnlyPlaceBuilder(@NotNull BlockAnd<?> blockAnd, @NotNull ThirExpr expr) {
        return exprToPlace(blockAnd, expr, Mutability.IMMUTABLE, null);
    }

    @NotNull
    private BlockAnd<PlaceBuilder> toPlaceBuilder(@NotNull BlockAnd<?> blockAnd, @NotNull ThirExpr expr) {
        return exprToPlace(blockAnd, expr, Mutability.MUTABLE, null);
    }

    @NotNull
    private BlockAnd<PlaceBuilder> exprToPlace(
        @NotNull BlockAnd<?> blockAnd,
        @NotNull ThirExpr expr,
        @NotNull Mutability mutability,
        @Nullable List<MirLocal> fakeBorrowTemps
    ) {
        if (expr instanceof ThirExpr.ScopeExpr) {
            ThirExpr.ScopeExpr scopeExpr = (ThirExpr.ScopeExpr) expr;
            return inScope(scopeExpr.getRegionScope(), () -> exprToPlace(blockAnd, scopeExpr.getExpr(), mutability, fakeBorrowTemps));
        } else if (expr instanceof ThirExpr.Field) {
            ThirExpr.Field fieldExpr = (ThirExpr.Field) expr;
            BlockAnd<PlaceBuilder> innerResult = exprToPlace(blockAnd, fieldExpr.getExpr(), mutability, fakeBorrowTemps);
            PlaceBuilder placeBuilder = innerResult.getElem();
            Ty ty = fieldExpr.getExpr().getTy();
            if (ty instanceof TyAdt && ((TyAdt) ty).getItem() instanceof RsEnumItem) {
                throw new UnsupportedOperationException("TODO: placeBuilder.downcast");
            }
            return new BlockAnd<>(innerResult.getBlock(), placeBuilder.field(fieldExpr.getFieldIndex(), fieldExpr.getTy()));
        } else if (expr instanceof ThirExpr.Deref) {
            BlockAnd<PlaceBuilder> innerResult = exprToPlace(blockAnd, ((ThirExpr.Deref) expr).getArg(), mutability, fakeBorrowTemps);
            return new BlockAnd<>(innerResult.getBlock(), innerResult.getElem().deref());
        } else if (expr instanceof ThirExpr.Index) {
            ThirExpr.Index indexExpr = (ThirExpr.Index) expr;
            return lowerIndexExpression(
                blockAnd, indexExpr.getLhs(), indexExpr.getIndex(), mutability,
                fakeBorrowTemps, indexExpr.getTempLifetime(), expr.getSpan(), sourceInfo(expr.getSpan())
            );
        } else if (expr instanceof ThirExpr.VarRef) {
            ThirExpr.VarRef varRefExpr = (ThirExpr.VarRef) expr;
            return new BlockAnd<>(blockAnd.getBlock(), new PlaceBuilder(varLocal(varRefExpr.getLocal())));
        } else if (expr instanceof ThirExpr.UpvarRef
            || expr instanceof ThirExpr.PlaceTypeAscription
            || expr instanceof ThirExpr.ValueTypeAscription) {
            throw new UnsupportedOperationException("TODO");
        } else {
            // All other expressions: create a temporary
            BlockAnd<MirLocal> tempResult = toTemp(blockAnd, expr, expr.getTempLifetime(), mutability);
            return new BlockAnd<>(tempResult.getBlock(), new PlaceBuilder(tempResult.getElem()));
        }
    }

    @NotNull
    private BlockAnd<PlaceBuilder> lowerIndexExpression(
        @NotNull BlockAnd<?> blockAnd,
        @NotNull ThirExpr base,
        @NotNull ThirExpr index,
        @NotNull Mutability mutability,
        @Nullable List<MirLocal> fakeBorrowTemps,
        @Nullable Scope tempLifetime,
        @NotNull MirSpan span,
        @NotNull MirSourceInfo sourceInfo
    ) {
        List<MirLocal> newFakeBorrowTemps = fakeBorrowTemps != null ? fakeBorrowTemps : new ArrayList<>();

        BlockAnd<PlaceBuilder> baseResult = exprToPlace(blockAnd, base, mutability, newFakeBorrowTemps);
        PlaceBuilder basePlace = baseResult.getElem();

        BlockAnd<MirLocal> idxResult = toTemp(baseResult, index, tempLifetime, Mutability.IMMUTABLE);
        MirLocal idx = idxResult.getElem();

        MirBasicBlockImpl currentBlock = boundsCheck(idxResult.getBlock(), basePlace, idx, span, sourceInfo);

        boolean isOutermostIndex = fakeBorrowTemps == null;
        if (isOutermostIndex) {
            currentBlock = readFakeBorrows(currentBlock, newFakeBorrowTemps, sourceInfo);
        } else {
            addFakeBorrowsOfBase(basePlace.toPlace());
        }

        return new BlockAnd<>(currentBlock, basePlace.index(idx));
    }

    @NotNull
    private MirBasicBlockImpl boundsCheck(
        @NotNull MirBasicBlockImpl block,
        @NotNull PlaceBuilder slice,
        @NotNull MirLocal index,
        @NotNull MirSpan span,
        @NotNull MirSourceInfo sourceInfo
    ) {
        Ty usizeTy = TyInteger.USize.INSTANCE;
        Ty boolTy = TyBool.INSTANCE;
        MirPlace len = localDecls.newLocal(usizeTy, outermostSourceInfo(span)).intoPlace();
        MirPlace lt = localDecls.newLocal(boolTy, outermostSourceInfo(span)).intoPlace();

        MirBasicBlockImpl blockTemp = block.pushAssign(len, new MirRvalue.Len(slice.toPlace()), sourceInfo);
        blockTemp = blockTemp.pushAssign(
            lt,
            new MirRvalue.BinaryOpUse(MirBinaryOperator.toMir(ComparisonOp.LT), new MirOperand.Copy(new MirPlace(index)), new MirOperand.Copy(len)),
            sourceInfo
        );
        MirAssertKind msg = new MirAssertKind.BoundsCheck(new MirOperand.Move(len), new MirOperand.Copy(new MirPlace(index)));
        blockTemp = doAssert(blockTemp, new MirOperand.Move(lt), true, span, msg);
        return blockTemp;
    }

    private void addFakeBorrowsOfBase(@NotNull MirPlace basePlace) {
        // Simplified stub - full implementation requires type inspection
    }

    @NotNull
    private MirBasicBlockImpl readFakeBorrows(
        @NotNull MirBasicBlockImpl block,
        @NotNull List<MirLocal> fakeBorrowTemps,
        @NotNull MirSourceInfo sourceInfo
    ) {
        MirBasicBlockImpl blockTemp = block;
        for (MirLocal temp : fakeBorrowTemps) {
            blockTemp = blockTemp.pushFakeRead(MirStatement.FakeRead.Cause.ForIndex.INSTANCE, new MirPlace(temp), sourceInfo);
        }
        return blockTemp;
    }

    @NotNull
    private BlockAnd<Void> thenElseBreak(
        @NotNull BlockAnd<?> blockAnd,
        @NotNull ThirExpr cond,
        @Nullable Scope tempScopeOverride,
        @NotNull Scope breakScope,
        @NotNull MirSourceInfo variableSource
    ) {
        if (cond instanceof ThirExpr.ScopeExpr) {
            ThirExpr.ScopeExpr scopeExpr = (ThirExpr.ScopeExpr) cond;
            return inScope(scopeExpr.getRegionScope(), () -> thenElseBreak(blockAnd, scopeExpr.getExpr(), tempScopeOverride, breakScope, variableSource));
        } else if (cond instanceof ThirExpr.Let) {
            ThirExpr.Let letExpr = (ThirExpr.Let) cond;
            return lowerLetExpr(blockAnd, letExpr.getExpr(), letExpr.getPat(), breakScope, variableSource.getScope(), variableSource.getSpan(), true);
        } else {
            Scope tempScope = tempScopeOverride != null ? tempScopeOverride : scopes.topmost();
            BlockAnd<MirLocal> tempResult = toTemp(blockAnd, cond, tempScope, Mutability.MUTABLE);
            MirOperand operand = new MirOperand.Move(new MirPlace(tempResult.getElem()));
            MirBasicBlockImpl thenBlock = basicBlocks.newBlock();
            MirBasicBlockImpl elseBlock = basicBlocks.newBlock();
            MirSourceInfo src = sourceInfo(cond.getSpan());
            tempResult.getBlock().terminateWithIf(operand, thenBlock, elseBlock, src);
            breakForElse(elseBlock, breakScope, src);
            return thenBlock.andUnit();
        }
    }

    private void breakForElse(@NotNull MirBasicBlockImpl block, @NotNull Scope breakScope, @NotNull MirSourceInfo source) {
        IfThenScope ifThenScope = scopes.getIfThenScope();
        if (ifThenScope == null) throw new IllegalStateException("ifThenScope is null");
        int scopeIndex = scopes.scopeIndex(breakScope);
        DropTree.DropNode dropNode = ifThenScope.getElseDrops().getRoot();
        List<MirScope> scopesList = scopes.scopes();
        for (int i = scopeIndex + 1; i < scopesList.size(); i++) {
            for (Drop drop : scopesList.get(i).drops()) {
                dropNode = ifThenScope.getElseDrops().addDrop(drop, dropNode);
            }
        }
        ifThenScope.getElseDrops().addEntry(block, dropNode);
        block.setTerminatorSource(source);
    }

    @NotNull
    private BlockAnd<Void> lowerLetExpr(
        @NotNull BlockAnd<?> blockAnd,
        @NotNull ThirExpr expr,
        @NotNull ThirPat pat,
        @NotNull Scope elseTarget,
        @Nullable MirSourceScope sourceScope,
        @NotNull MirSpan span,
        boolean declareBindings
    ) {
        BlockAnd<PlaceBuilder> scrutineeResult = lowerScrutinee(blockAnd, expr);
        PlaceBuilder exprPlaceBuilder = scrutineeResult.getElem();
        ThirPat wildcard = new ThirPat.Wild(pat.getTy(), MirSpan.Fake.INSTANCE);
        MirMatch.MirCandidate guardCandidate = new MirMatch.MirCandidate(exprPlaceBuilder.copy(), pat, false);
        MirMatch.MirCandidate otherwiseCandidate = new MirMatch.MirCandidate(exprPlaceBuilder.copy(), wildcard, false);
        lowerMatchTree(
            scrutineeResult.getBlock(),
            pat.getSource(),
            pat.getSource(),
            false,
            Arrays.asList(guardCandidate, otherwiseCandidate)
        );
        MirPlace exprPlace = exprPlaceBuilder.tryToPlace();
        MirBasicBlockImpl otherwisePostGuardBlock = otherwiseCandidate.getPreBindingBlock();
        if (otherwisePostGuardBlock == null) throw new IllegalStateException("otherwisePostGuardBlock is null");
        breakForElse(otherwisePostGuardBlock, elseTarget, sourceInfo(expr.getSpan()));

        if (declareBindings) {
            Pair<MirPlace, MirSpan> matchPlacePair = exprPlace != null ? new Pair<>(exprPlace, expr.getSpan()) : null;
            declareBindings(sourceScope, pat.getSource(), pat, null, matchPlacePair);
        }

        MirBasicBlockImpl postGuardBlock = bindPattern(sourceInfo(pat.getSource()), guardCandidate, expr.getSpan(), null, false);
        return postGuardBlock.andUnit();
    }

    @NotNull
    private BlockAnd<PlaceBuilder> lowerScrutinee(@NotNull BlockAnd<?> blockAnd, @NotNull ThirExpr scrutinee) {
        BlockAnd<PlaceBuilder> result = toPlaceBuilder(blockAnd, scrutinee);
        MirPlace scrutineePlace = result.getElem().tryToPlace();
        if (scrutineePlace != null) {
            MirStatement.FakeRead.Cause cause = new MirStatement.FakeRead.Cause.ForMatchedPlace(null);
            MirSourceInfo src = sourceInfo(scrutinee.getSpan());
            result.getBlock().pushFakeRead(cause, scrutineePlace, src);
        }
        return result;
    }

    private void lowerMatchTree(
        @NotNull MirBasicBlockImpl block,
        @NotNull MirSpan scrutineeSpan,
        @NotNull MirSpan matchStartSpan,
        boolean matchHasGuard,
        @NotNull List<MirMatch.MirCandidate> candidates
    ) {
        Ref<MirBasicBlockImpl> otherwise = new Ref<>(null);
        matchCandidates(matchStartSpan, scrutineeSpan, block, otherwise, candidates);

        if (!otherwise.isNull()) {
            MirSourceInfo src = sourceInfo(scrutineeSpan);
            otherwise.get().terminateWithUnreachable(src);
        }

        MirMatch.MirCandidate[] previousCandidate = {null};
        for (MirMatch.MirCandidate candidate : candidates) {
            candidate.visitLeaves(leafCandidate -> {
                if (previousCandidate[0] != null) {
                    previousCandidate[0].setNextCandidatePreBindingBlock(leafCandidate.getPreBindingBlock());
                }
                previousCandidate[0] = leafCandidate;
            });
        }
        // TODO fake borrows
    }

    @NotNull
    private BlockAnd<Void> matchExpr(
        @NotNull BlockAnd<?> blockAnd,
        @NotNull MirPlace destination,
        @NotNull MirSpan span,
        @NotNull ThirExpr scrutinee,
        @NotNull List<MirMatch.MirArm> arms
    ) {
        BlockAnd<PlaceBuilder> scrutineeResult = lowerScrutinee(blockAnd, scrutinee);
        PlaceBuilder scrutineePlace = scrutineeResult.getElem();

        List<MirMatch.MirCandidate> candidates = new ArrayList<>();
        for (MirMatch.MirArm arm : arms) {
            candidates.add(new MirMatch.MirCandidate(scrutineePlace.copy(), arm.getPattern(), arm.getGuard() != null));
        }
        boolean matchHasGuard = candidates.stream().anyMatch(MirMatch.MirCandidate::isHasGuard);

        MirSpan matchStartSpan = MirUtils.asSpan(((RsMatchExpr) span.getReference()).getMatch()); // TODO
        lowerMatchTree(scrutineeResult.getBlock(), scrutinee.getSpan(), matchStartSpan, matchHasGuard, candidates);

        List<Pair<MirMatch.MirArm, MirMatch.MirCandidate>> armCandidates = new ArrayList<>();
        for (int i = 0; i < arms.size(); i++) {
            armCandidates.add(new Pair<>(arms.get(i), candidates.get(i)));
        }
        return lowerMatchArms(destination, scrutineePlace, scrutinee.getSpan(), armCandidates, sourceInfo(span));
    }

    @NotNull
    private BlockAnd<Void> lowerMatchArms(
        @NotNull MirPlace destination,
        @NotNull PlaceBuilder scrutineePlaceBuilder,
        @NotNull MirSpan scrutineeSpan,
        @NotNull List<Pair<MirMatch.MirArm, MirMatch.MirCandidate>> armCandidates,
        @NotNull MirSourceInfo outerSourceInfo
    ) {
        List<BlockAnd<Void>> armEndBlocks = new ArrayList<>();
        for (Pair<MirMatch.MirArm, MirMatch.MirCandidate> pair : armCandidates) {
            MirMatch.MirArm arm = pair.getFirst();
            MirMatch.MirCandidate candidate = pair.getSecond();
            Scope matchScope = localScope();
            BlockAnd<Void> armEnd = inScope(arm.getScope(), () -> {
                MirPlace scrutineePlace = scrutineePlaceBuilder.tryToPlace();
                Pair<MirPlace, MirSpan> optScrutineePlace = scrutineePlace != null ? new Pair<>(scrutineePlace, scrutineeSpan) : null;
                MirSourceScope scope = declareBindings(null, arm.getSpan(), arm.getPattern(), arm.getGuard(), optScrutineePlace);

                MirBasicBlockImpl armBlock = bindPattern(
                    outerSourceInfo,
                    candidate,
                    scrutineeSpan,
                    new Pair<>(arm, matchScope),
                    false
                );

                if (scope != null) {
                    sourceScopes.setSourceScope(scope);
                }

                return exprIntoPlace(armBlock.andUnit(), arm.getBody(), destination);
            });
            armEndBlocks.add(armEnd);
        }

        MirBasicBlockImpl endBlock = basicBlocks.newBlock();
        MirSourceInfo endBrace = sourceInfo(outerSourceInfo.getSpan().getEndPoint());
        for (BlockAnd<Void> armBlock : armEndBlocks) {
            MirBasicBlockImpl blk = armBlock.getBlock();
            List<MirStatement> stmts = blk.getStatements();
            MirSourceInfo lastLocation = !stmts.isEmpty() ? stmts.get(stmts.size() - 1).getSource() : endBrace;
            blk.terminateWithGoto(endBlock, lastLocation);
        }

        sourceScopes.setSourceScope(outerSourceInfo.getScope());
        return endBlock.andUnit();
    }

    @NotNull
    private MirBasicBlockImpl bindPattern(
        @NotNull MirSourceInfo outerSourceInfo,
        @NotNull MirMatch.MirCandidate candidate,
        @NotNull MirSpan scrutineeSpan,
        @Nullable Pair<MirMatch.MirArm, Scope> armMatchScope,
        boolean storagesAlive
    ) {
        if (candidate.getSubcandidates().isEmpty()) {
            return bindAndGuardMatchedCandidate(
                candidate,
                Collections.emptyList(),
                scrutineeSpan,
                armMatchScope,
                true,
                storagesAlive
            );
        } else {
            throw new UnsupportedOperationException("TODO");
        }
    }

    @NotNull
    private MirBasicBlockImpl bindAndGuardMatchedCandidate(
        @NotNull MirMatch.MirCandidate candidate,
        @NotNull List<MirMatch.MirBinding> parentBindings,
        @NotNull MirSpan scrutineeSpan,
        @Nullable Pair<MirMatch.MirArm, Scope> armMatchScope,
        boolean scheduleDrops,
        boolean storagesAlive
    ) {
        TestAssertUtil.testAssert(() -> candidate.getMatchPairs().isEmpty());
        MirSourceInfo candidateSourceInfo = sourceInfo(candidate.getSpan());
        MirBasicBlockImpl block = candidate.getPreBindingBlock();
        if (block == null) throw new IllegalStateException("preBindingBlock is null");
        if (candidate.getNextCandidatePreBindingBlock() != null) {
            MirBasicBlockImpl freshBlock = basicBlocks.newBlock();
            block.terminateWithFalseEdges(freshBlock, candidate.getNextCandidatePreBindingBlock(), candidateSourceInfo);
            block = freshBlock;
        }

        // TODO ascribe_types

        if (armMatchScope != null && armMatchScope.getFirst().getGuard() != null) {
            throw new UnsupportedOperationException("TODO");
        } else {
            List<MirMatch.MirBinding> allBindings = new ArrayList<>(parentBindings);
            allBindings.addAll(candidate.getBindings());
            bindMatchedCandidateForArmBody(block, scheduleDrops, allBindings, storagesAlive);
            return block;
        }
    }

    private void bindMatchedCandidateForArmBody(
        @NotNull MirBasicBlockImpl block,
        boolean scheduleDrops,
        @NotNull List<MirMatch.MirBinding> bindings,
        boolean storagesAlive
    ) {
        for (MirMatch.MirBinding binding : bindings) {
            MirSourceInfo src = sourceInfo(binding.getSpan());
            MirPlace local;
            if (storagesAlive) {
                local = varLocal(binding.getVariable()).intoPlace();
            } else {
                local = storageLiveBinding(block, binding.getVariable(), binding.getSpan(), scheduleDrops);
            }
            if (scheduleDrops) {
                scheduleDropForBinding(binding.getVariable(), binding.getSpan());
            }
            MirRvalue rvalue;
            ThirBindingMode bindingMode = binding.getBindingMode();
            if (bindingMode instanceof ThirBindingMode.ByValue) {
                rvalue = new MirRvalue.Use(consumeByCopyOrMove(binding.getSource()));
            } else if (bindingMode instanceof ThirBindingMode.ByRef) {
                rvalue = new MirRvalue.Ref(((ThirBindingMode.ByRef) bindingMode).kind, binding.getSource());
            } else {
                throw new IllegalStateException("Unknown binding mode");
            }
            block.pushAssign(local, rvalue, src);
        }
    }

    private void matchCandidates(
        @NotNull MirSpan span,
        @NotNull MirSpan scrutineeSpan,
        @NotNull MirBasicBlockImpl startBlock,
        @NotNull Ref<MirBasicBlockImpl> otherwiseBlock,
        @NotNull List<MirMatch.MirCandidate> candidates
    ) {
        boolean splitOrCandidate = false;
        for (MirMatch.MirCandidate candidate : candidates) {
            if (simplifyCandidate(candidate)) {
                splitOrCandidate = true;
            }
        }

        if (splitOrCandidate) {
            List<MirMatch.MirCandidate> newCandidates = new ArrayList<>();
            for (MirMatch.MirCandidate candidate : candidates) {
                candidate.visitLeaves(newCandidates::add);
            }
            matchSimplifiedCandidates(span, scrutineeSpan, startBlock, otherwiseBlock, newCandidates);
        } else {
            matchSimplifiedCandidates(span, scrutineeSpan, startBlock, otherwiseBlock, candidates);
        }
    }

    private boolean simplifyCandidate(@NotNull MirMatch.MirCandidate candidate) {
        List<MirMatch.MirBinding> existingBindings = candidate.getBindings();
        candidate.setBindings(new ArrayList<>());
        List<MirMatch.MirBinding> newBindings = new ArrayList<>();
        while (true) {
            List<MirMatch.MirMatchPair> matchPairs = candidate.getMatchPairs();
            candidate.setMatchPairs(new ArrayList<>());

            if (matchPairs.size() == 1 && matchPairs.get(0).getPattern() instanceof ThirPat.Or) {
                throw new UnsupportedOperationException("TODO");
            }

            boolean changed = false;
            for (MirMatch.MirMatchPair matchPair : matchPairs) {
                if (simplifyMatchPair(matchPair, candidate)) {
                    changed = true;
                } else {
                    candidate.getMatchPairs().add(matchPair);
                }
            }

            List<MirMatch.MirBinding> combined = new ArrayList<>(candidate.getBindings());
            combined.addAll(newBindings);
            newBindings = combined;
            candidate.getBindings().clear();

            if (!changed) {
                List<MirMatch.MirBinding> finalBindings = new ArrayList<>(existingBindings);
                finalBindings.addAll(newBindings);
                candidate.setBindings(finalBindings);
                existingBindings.clear();
                candidate.getMatchPairs().sort((a, b) -> {
                    boolean aIsOr = a.getPattern() instanceof ThirPat.Or;
                    boolean bIsOr = b.getPattern() instanceof ThirPat.Or;
                    return Boolean.compare(aIsOr, bIsOr);
                });
                return false;
            }
        }
    }

    private boolean simplifyMatchPair(@NotNull MirMatch.MirMatchPair matchPair, @NotNull MirMatch.MirCandidate candidate) {
        ThirPat pattern = matchPair.getPattern();
        if (pattern instanceof ThirPat.AscribeUserType || pattern instanceof ThirPat.Wild) {
            return true;
        } else if (pattern instanceof ThirPat.Binding) {
            ThirPat.Binding binding = (ThirPat.Binding) pattern;
            MirPlace source = matchPair.getPlace().tryToPlace();
            if (source != null) {
                candidate.getBindings().add(new MirMatch.MirBinding(
                    pattern.getSource(), source, binding.getVariable(), binding.getMode()
                ));
            }
            if (binding.getSubpattern() != null) {
                throw new UnsupportedOperationException("TODO");
            }
            return true;
        } else if (pattern instanceof ThirPat.Variant) {
            ThirPat.Variant variant = (ThirPat.Variant) pattern;
            boolean irrefutable = RsEnumItemUtil.getVariants(variant.getItem()).size() == 1;
            if (irrefutable) {
                PlaceBuilder placeBuilder = matchPair.getPlace().downcast(variant.getItem(), variant.getVariantIndex());
                candidate.getMatchPairs().addAll(fieldMatchPairs(placeBuilder, variant.getSubpatterns()));
                return true;
            } else {
                return false;
            }
        } else if (pattern instanceof ThirPat.Leaf) {
            ThirPat.Leaf leaf = (ThirPat.Leaf) pattern;
            candidate.getMatchPairs().addAll(fieldMatchPairs(matchPair.getPlace(), leaf.getSubpatterns()));
            return true;
        } else if (pattern instanceof ThirPat.Deref) {
            ThirPat.Deref deref = (ThirPat.Deref) pattern;
            PlaceBuilder placeBuilder = matchPair.getPlace().deref();
            candidate.getMatchPairs().add(MirMatch.MirMatchPair.create(placeBuilder, deref.getSubpattern()));
            return true;
        } else {
            throw new UnsupportedOperationException("TODO: " + pattern.getClass().getSimpleName());
        }
    }

    @NotNull
    private List<MirMatch.MirMatchPair> fieldMatchPairs(@NotNull PlaceBuilder place, @NotNull List<ThirFieldPat> subpatterns) {
        List<MirMatch.MirMatchPair> result = new ArrayList<>();
        for (ThirFieldPat fp : subpatterns) {
            PlaceBuilder place2 = place.cloneProject(new MirProjectionElem.Field<>(fp.getField(), fp.getPattern().getTy()));
            result.add(MirMatch.MirMatchPair.create(place2, fp.getPattern()));
        }
        return result;
    }

    private void matchSimplifiedCandidates(
        @NotNull MirSpan span,
        @NotNull MirSpan scrutineeSpan,
        @NotNull MirBasicBlockImpl startBlock,
        @NotNull Ref<MirBasicBlockImpl> otherwiseBlock,
        @NotNull List<MirMatch.MirCandidate> candidates
    ) {
        int matchedCount = 0;
        for (MirMatch.MirCandidate c : candidates) {
            if (c.getMatchPairs().isEmpty()) matchedCount++;
            else break;
        }
        List<MirMatch.MirCandidate> matchedCandidates = candidates.subList(0, matchedCount);
        List<MirMatch.MirCandidate> unmatchedCandidates = candidates.subList(matchedCount, candidates.size());

        MirBasicBlockImpl block;
        if (!matchedCandidates.isEmpty()) {
            MirBasicBlockImpl result = selectMatchedCandidates(matchedCandidates, startBlock);
            if (result != null) {
                block = result;
            } else {
                if (unmatchedCandidates.isEmpty()) return;
                block = basicBlocks.newBlock();
            }
        } else {
            block = startBlock;
        }

        if (unmatchedCandidates.isEmpty()) {
            if (!otherwiseBlock.isNull()) {
                block.terminateWithGoto(otherwiseBlock.get(), sourceInfo(span));
            } else {
                otherwiseBlock.set(block);
            }
            return;
        }

        testCandidatesWithOr(span, scrutineeSpan, unmatchedCandidates, block, otherwiseBlock);
    }

    @Nullable
    private MirBasicBlockImpl selectMatchedCandidates(
        @NotNull List<MirMatch.MirCandidate> matchedCandidates,
        @NotNull MirBasicBlockImpl startBlock
    ) {
        TestAssertUtil.testAssert(() -> !matchedCandidates.isEmpty());

        int fullyMatchedWithGuard = -1;
        for (int i = 0; i < matchedCandidates.size(); i++) {
            if (!matchedCandidates.get(i).isHasGuard()) {
                fullyMatchedWithGuard = i;
                break;
            }
        }
        if (fullyMatchedWithGuard == -1) fullyMatchedWithGuard = matchedCandidates.size() - 1;

        List<MirMatch.MirCandidate> reachableCandidates = matchedCandidates.subList(0, fullyMatchedWithGuard + 1);
        List<MirMatch.MirCandidate> unreachableCandidates = matchedCandidates.subList(fullyMatchedWithGuard + 1, matchedCandidates.size());

        MirBasicBlockImpl nextPrebinding = startBlock;
        for (MirMatch.MirCandidate candidate : reachableCandidates) {
            candidate.setPreBindingBlock(nextPrebinding);
            if (candidate.isHasGuard()) {
                nextPrebinding = basicBlocks.newBlock();
                candidate.setOtherwiseBlock(nextPrebinding);
            }
        }

        for (MirMatch.MirCandidate candidate : unreachableCandidates) {
            candidate.setPreBindingBlock(basicBlocks.newBlock());
        }

        return reachableCandidates.get(reachableCandidates.size() - 1).getOtherwiseBlock();
    }

    private void testCandidatesWithOr(
        @NotNull MirSpan span,
        @NotNull MirSpan scrutineeSpan,
        @NotNull List<MirMatch.MirCandidate> candidates,
        @NotNull MirBasicBlockImpl block,
        @NotNull Ref<MirBasicBlockImpl> otherwiseBlock
    ) {
        MirMatch.MirCandidate firstCandidate = candidates.get(0);
        if (!(firstCandidate.getMatchPairs().get(0).getPattern() instanceof ThirPat.Or)) {
            testCandidates(span, scrutineeSpan, candidates, block, otherwiseBlock);
            return;
        }
        throw new UnsupportedOperationException("TODO");
    }

    private void testCandidates(
        @NotNull MirSpan span,
        @NotNull MirSpan scrutineeSpan,
        @NotNull List<MirMatch.MirCandidate> candidates1,
        @NotNull MirBasicBlockImpl block,
        @NotNull Ref<MirBasicBlockImpl> otherwiseBlock
    ) {
        MirMatch.MirMatchPair matchPair = candidates1.get(0).getMatchPairs().get(0);
        MirTest test = MirTest.test(matchPair);
        PlaceBuilder matchPlace = matchPair.getPlace().copy();

        if (test instanceof MirTest.Switch) {
            for (MirMatch.MirCandidate candidate : candidates1) {
                if (!addVariantsToSwitch(matchPlace, candidate, ((MirTest.Switch) test).getVariants())) {
                    break;
                }
            }
        }

        int targetCount = test.targets();
        @SuppressWarnings("unchecked")
        List<MirMatch.MirCandidate>[] targetCandidates = new List[targetCount];
        for (int i = 0; i < targetCount; i++) {
            targetCandidates[i] = new ArrayList<>();
        }

        int skipped = 0;
        for (MirMatch.MirCandidate candidate : candidates1) {
            Integer index = sortCandidate(matchPlace, test, candidate);
            if (index == null) break;
            targetCandidates[index].add(candidate);
            skipped++;
        }
        List<MirMatch.MirCandidate> candidates2 = candidates1.subList(skipped, candidates1.size());
        TestAssertUtil.testAssert(() -> candidates1.size() > candidates2.size());

        Supplier<List<MirBasicBlockImpl>> makeTargetBlocks = () -> {
            Ref<MirBasicBlockImpl> remainderStart;
            if (candidates2.isEmpty()) {
                remainderStart = otherwiseBlock;
            } else {
                remainderStart = new Ref<>(null);
            }
            List<MirBasicBlockImpl> targetBlocks = new ArrayList<>();
            for (List<MirMatch.MirCandidate> candidateList : targetCandidates) {
                if (!candidateList.isEmpty()) {
                    MirBasicBlockImpl candidateStart = basicBlocks.newBlock();
                    matchCandidates(span, scrutineeSpan, candidateStart, remainderStart, candidateList);
                    targetBlocks.add(candidateStart);
                } else {
                    MirBasicBlockImpl existing = remainderStart.get();
                    if (existing == null) {
                        existing = basicBlocks.newBlock();
                        remainderStart.set(existing);
                    }
                    targetBlocks.add(existing);
                }
            }
            if (!candidates2.isEmpty()) {
                MirBasicBlockImpl rem = remainderStart.get();
                if (rem == null) rem = basicBlocks.newBlock();
                matchCandidates(span, scrutineeSpan, rem, otherwiseBlock, candidates2);
            }
            return targetBlocks;
        };

        performTest(span, scrutineeSpan, block, matchPlace, test, makeTargetBlocks);
    }

    // Stub methods that delegate to building infrastructure
    private void performTest(
        @NotNull MirSpan span,
        @NotNull MirSpan scrutineeSpan,
        @NotNull MirBasicBlockImpl block,
        @NotNull PlaceBuilder matchPlace,
        @NotNull MirTest test,
        @NotNull Supplier<List<MirBasicBlockImpl>> makeTargetBlocks
    ) {
        // This delegates to the test infrastructure in building/
        MirTestUtil.performTest(this, span, scrutineeSpan, block, matchPlace, test, makeTargetBlocks);
    }

    private boolean addVariantsToSwitch(@NotNull PlaceBuilder matchPlace, @NotNull MirMatch.MirCandidate candidate, @NotNull BitSet variants) {
        // Stub - delegated to MirTest infrastructure
        return MirTestUtil.addVariantsToSwitch(matchPlace, candidate, variants);
    }

    @Nullable
    private Integer sortCandidate(@NotNull PlaceBuilder matchPlace, @NotNull MirTest test, @NotNull MirMatch.MirCandidate candidate) {
        return MirTestUtil.sortCandidate(matchPlace, test, candidate);
    }

    @NotNull
    private BlockAnd<Void> astBlockIntoPlace(@NotNull BlockAnd<?> blockAnd, @NotNull ThirBlock block, @NotNull MirPlace place, @NotNull MirSpan span) {
        return inScope(block.getDestructionScope(), () ->
            inScope(block.getRegionScope(), () ->
                astBlockStmtsIntoPlace(blockAnd, block, place, span)
            )
        );
    }

    @NotNull
    private BlockAnd<Void> astBlockStmtsIntoPlace(@NotNull BlockAnd<?> blockAndIn, @NotNull ThirBlock block, @NotNull MirPlace place, @NotNull MirSpan span) {
        BlockAnd<?> blockAnd = blockAndIn;
        MirSourceScope outerSourceScope = sourceScopes.getSourceScope();
        List<Scope> letScopeStack = new ArrayList<>();
        MirSourceInfo source = sourceInfo(span);

        for (ThirStatement statement : block.getStatements()) {
            if (statement instanceof ThirStatement.Let) {
                ThirStatement.Let letStmt = (ThirStatement.Let) statement;
                if (letStmt.getElseBlock() == null) {
                    pushScope(letStmt.getRemainderScope());
                    letScopeStack.add(letStmt.getRemainderScope());
                    MirSpan remainderSourceInfo = MirUtils.getSpan(letStmt.getRemainderScope());
                    MirSourceScope visibilityScope = sourceScopes.newSourceScope(remainderSourceInfo);
                    if (letStmt.getInitializer() != null) {
                        final BlockAnd<?> capturedBlockAnd = blockAnd;
                        blockAnd = inScope(letStmt.getDestructionScope(), () ->
                            inScope(letStmt.getInitScope(), () -> {
                                declareBindings(
                                    visibilityScope, remainderSourceInfo, letStmt.getPattern(), null,
                                    new Pair<>(null, letStmt.getInitializer().getSpan())
                                );
                                return exprIntoPattern(capturedBlockAnd, letStmt.getPattern(), letStmt.getInitializer());
                            })
                        );
                    } else {
                        final BlockAnd<?> capturedBlockAnd2 = blockAnd;
                        blockAnd = inScope(letStmt.getDestructionScope(), () ->
                            inScope(letStmt.getInitScope(), () -> {
                                declareBindings(visibilityScope, remainderSourceInfo, letStmt.getPattern(), null, null);
                                return capturedBlockAnd2.getBlock().andUnit();
                            })
                        );
                        final BlockAnd<?> finalBlockAnd = blockAnd;
                        visitPrimaryBindings(letStmt.getPattern(), (mutability, name, mode, node, bindSpan, ty) -> {
                            storageLiveBinding(finalBlockAnd.getBlock(), node, bindSpan, true);
                            scheduleDropForBinding(node, bindSpan);
                        });
                    }
                    sourceScopes.setSourceScope(visibilityScope);
                } else {
                    throw new UnsupportedOperationException("TODO: let-else");
                }
            } else if (statement instanceof ThirStatement.Expr) {
                ThirStatement.Expr exprStmt = (ThirStatement.Expr) statement;
                final BlockAnd<?> capturedBlockAnd = blockAnd;
                blockAnd = inScope(exprStmt.getDestructionScope(), () ->
                    inScope(exprStmt.getScope(), () ->
                        statementExpr(capturedBlockAnd, exprStmt.getExpr(), exprStmt.getScope())
                    )
                );
            } else {
                throw new UnsupportedOperationException("TODO");
            }
        }

        if (block.getExpr() != null) {
            blockAnd = exprIntoPlace(blockAnd, block.getExpr(), place);
        } else {
            if (place.getLocal().getTy() instanceof TyUnit) {
                pushAssignUnit(blockAnd.getBlock(), place, source);
            }
        }

        for (int i = 0; i < letScopeStack.size(); i++) {
            blockAnd = popScope(blockAnd);
        }
        sourceScopes.setSourceScope(outerSourceScope);
        return blockAnd.getBlock().andUnit();
    }

    @NotNull
    private BlockAnd<Void> exprIntoPattern(@NotNull BlockAnd<?> blockAnd, @NotNull ThirPat pattern, @NotNull ThirExpr initializer) {
        if (pattern instanceof ThirPat.Binding) {
            ThirPat.Binding binding = (ThirPat.Binding) pattern;
            if (binding.getMode() instanceof ThirBindingMode.ByValue && binding.getSubpattern() == null) {
                MirPlace local = storageLiveBinding(blockAnd.getBlock(), binding.getVariable(), binding.getSource(), true);
                BlockAnd<Void> result = exprIntoPlace(blockAnd, initializer, local);
                MirSourceInfo src = sourceInfo(binding.getSource());
                result.getBlock().pushFakeRead(new MirStatement.FakeRead.Cause.ForLet(null), local, src);
                scheduleDropForBinding(binding.getVariable(), binding.getSource());
                return result.getBlock().andUnit();
            }
        }
        throw new UnsupportedOperationException("TODO");
    }

    private void scheduleDropForBinding(@NotNull LocalVar variable, @NotNull MirSpan span) {
        MirLocal local = varLocal(variable);
        Scope scope = MirUtils.getVariableScope(getRegionScopeTree(), variable);
        if (scope != null) {
            scheduleDrop(scope, local, Drop.Kind.VALUE);
        }
    }

    @NotNull
    private MirPlace storageLiveBinding(
        @NotNull MirBasicBlockImpl block,
        @NotNull LocalVar variable,
        @NotNull MirSpan source,
        boolean scheduleDrop
    ) {
        MirLocal local = varLocal(variable);
        block.pushStorageLive(local, sourceInfo(source));
        if (scheduleDrop) {
            Scope scope = MirUtils.getVariableScope(getRegionScopeTree(), variable);
            if (scope != null) {
                scheduleDrop(scope, local, Drop.Kind.STORAGE);
            }
        }
        return new MirPlace(local);
    }

    @NotNull
    private MirLocal varLocal(@NotNull LocalVar variable) {
        MirLocalForNode localForNode = varIndices.get(variable);
        if (localForNode instanceof MirLocalForNode.ForGuard) {
            throw new UnsupportedOperationException("TODO");
        } else if (localForNode instanceof MirLocalForNode.One) {
            return ((MirLocalForNode.One) localForNode).getLocal();
        } else {
            throw new IllegalStateException("Could not find variable: " + variable.getName());
        }
    }

    @Nullable
    private MirSourceScope declareBindings(
        @Nullable MirSourceScope visibilityScope,
        @NotNull MirSpan scopeSource,
        @NotNull ThirPat pattern,
        @Nullable Object guard,
        @Nullable Pair<MirPlace, MirSpan> matchPlace
    ) {
        MirSourceScope[] actualVisibilityScope = {visibilityScope};
        visitPrimaryBindings(pattern, (mutability, name, mode, variable, span, ty) -> {
            if (actualVisibilityScope[0] == null) {
                actualVisibilityScope[0] = sourceScopes.newSourceScope(scopeSource);
            }
            declareBinding(
                sourceInfo(span),
                actualVisibilityScope[0],
                mutability,
                name,
                mode,
                variable,
                ty,
                matchPlace,
                pattern.getSource()
            );
        });
        if (guard != null) {
            throw new UnsupportedOperationException("TODO");
        }
        return actualVisibilityScope[0];
    }

    private void declareBinding(
        @NotNull MirSourceInfo source,
        @NotNull MirSourceScope visibilityScope,
        @NotNull Mutability mutability,
        @NotNull String name,
        @NotNull ThirBindingMode mode,
        @NotNull LocalVar variable,
        @NotNull Ty variableTy,
        @Nullable Pair<MirPlace, MirSpan> matchPlace,
        @NotNull MirSpan patternSource
    ) {
        MirSourceInfo debugSource = new MirSourceInfo(source.getSpan(), visibilityScope);
        MirBindingMode bindingMode;
        if (mode instanceof ThirBindingMode.ByValue) {
            bindingMode = new MirBindingMode.BindByValue(mutability);
        } else {
            bindingMode = new MirBindingMode.BindByReference(mutability);
        }
        MirLocal localForArmBody = localDecls.newLocal(
            mutability, false,
            new MirLocalInfo.User(
                new MirClearCrossCrate.Set<>(
                    new MirBindingForm.Var(
                        new MirVarBindingForm(bindingMode, null, matchPlace, patternSource)
                    )
                )
            ),
            null, variableTy, source
        );
        varDebugInfo.add(new MirVarDebugInfo(name, debugSource, new MirVarDebugInfo.Contents.Place(new MirPlace(localForArmBody))));
        varIndices.put(variable, new MirLocalForNode.One(localForArmBody));
    }

    private void visitPrimaryBindings(@NotNull ThirPat pattern, @NotNull PrimaryBindingVisitor action) {
        if (pattern instanceof ThirPat.Binding) {
            ThirPat.Binding binding = (ThirPat.Binding) pattern;
            if (binding.getIsPrimary()) {
                action.visit(binding.getMutability(), binding.getName(), binding.getMode(), binding.getVariable(), binding.getSource(), binding.getTy());
            }
            if (binding.getSubpattern() != null) {
                visitPrimaryBindings(binding.getSubpattern(), action);
            }
        } else if (pattern instanceof ThirPat.Const || pattern instanceof ThirPat.Range || pattern instanceof ThirPat.Wild) {
            // no-op
        } else if (pattern instanceof ThirPat.Deref) {
            visitPrimaryBindings(((ThirPat.Deref) pattern).getSubpattern(), action);
        } else if (pattern instanceof ThirPat.Leaf) {
            for (ThirFieldPat subpattern : ((ThirPat.Leaf) pattern).getSubpatterns()) {
                visitPrimaryBindings(subpattern.getPattern(), action);
            }
        } else if (pattern instanceof ThirPat.Variant) {
            for (ThirFieldPat subpattern : ((ThirPat.Variant) pattern).getSubpatterns()) {
                visitPrimaryBindings(subpattern.getPattern(), action);
            }
        } else {
            throw new UnsupportedOperationException("TODO: " + pattern.getClass().getSimpleName());
        }
    }

    @FunctionalInterface
    private interface PrimaryBindingVisitor {
        void visit(@NotNull Mutability mutability, @NotNull String name, @NotNull ThirBindingMode mode, @NotNull LocalVar variable, @NotNull MirSpan span, @NotNull Ty ty);
    }

    @NotNull
    private BlockAnd<MirRvalue> toLocalRvalue(@NotNull BlockAnd<?> blockAnd, @NotNull ThirExpr expr) {
        return toRvalue(blockAnd, expr, scopes.topmost());
    }

    @NotNull
    private BlockAnd<MirOperand> toLocalOperand(@NotNull BlockAnd<?> blockAnd, @NotNull ThirExpr expr) {
        return toOperand(blockAnd, expr, scopes.topmost(), NeedsTemporary.Maybe);
    }

    @NotNull
    private BlockAnd<MirOperand> toLocalCallOperand(@NotNull BlockAnd<?> blockAnd, @NotNull ThirExpr expr) {
        return toCallOperand(blockAnd, expr, scopes.topmost());
    }

    @NotNull
    private BlockAnd<MirOperand> toCallOperand(@NotNull BlockAnd<?> blockAnd, @NotNull ThirExpr expr, @NotNull Scope scope) {
        if (expr instanceof ThirExpr.ScopeExpr) {
            ThirExpr.ScopeExpr scopeExpr = (ThirExpr.ScopeExpr) expr;
            return inScope(scopeExpr.getRegionScope(), () -> toCallOperand(blockAnd, scopeExpr.getExpr(), scope));
        }
        return toOperand(blockAnd, expr, scope, NeedsTemporary.Maybe);
    }

    @NotNull
    private BlockAnd<MirRvalue> toRvalue(@NotNull BlockAnd<?> blockAnd, @NotNull ThirExpr expr, @NotNull Scope scope) {
        if (expr instanceof ThirExpr.ScopeExpr) {
            ThirExpr.ScopeExpr scopeExpr = (ThirExpr.ScopeExpr) expr;
            return inScope(scopeExpr.getRegionScope(), () -> toRvalue(blockAnd, scopeExpr.getExpr(), scope));
        } else if (expr instanceof ThirExpr.Literal || expr instanceof ThirExpr.NamedConst
            || expr instanceof ThirExpr.NonHirLiteral || expr instanceof ThirExpr.ZstLiteral
            || expr instanceof ThirExpr.ConstParam || expr instanceof ThirExpr.ConstBlock
            || expr instanceof ThirExpr.StaticRef) {
            MirConstant constant = toConstant(expr);
            return new BlockAnd<>(blockAnd.getBlock(), new MirRvalue.Use(new MirOperand.Constant(constant)));
        } else if (expr instanceof ThirExpr.Unary) {
            ThirExpr.Unary unaryExpr = (ThirExpr.Unary) expr;
            BlockAnd<MirOperand> operandResult = toOperand(blockAnd, unaryExpr.getArg(), scope, NeedsTemporary.No);
            BlockAnd<MirOperand> assertResult = assertNoNegOverflow(operandResult, unaryExpr, unaryExpr.getTy(), sourceInfo(expr.getSpan()));
            return new BlockAnd<>(assertResult.getBlock(), new MirRvalue.UnaryOpUse(unaryExpr.getOp(), assertResult.getElem()));
        } else if (expr instanceof ThirExpr.Binary) {
            ThirExpr.Binary binaryExpr = (ThirExpr.Binary) expr;
            BlockAnd<MirOperand> leftResult = toOperand(blockAnd, binaryExpr.getLeft(), scope, NeedsTemporary.Maybe);
            BlockAnd<MirOperand> rightResult = toOperand(leftResult, binaryExpr.getRight(), scope, NeedsTemporary.No);
            return buildBinaryOp(rightResult.getBlock(), leftResult.getElem(), rightResult.getElem(), binaryExpr.getOp(), binaryExpr.getTy(), binaryExpr.getSpan());
        } else if (expr instanceof ThirExpr.Array) {
            ThirExpr.Array arrayExpr = (ThirExpr.Array) expr;
            BlockAnd<?> current = blockAnd;
            Ty elementType = (arrayExpr.getTy() instanceof TyArray) ? ((TyArray) arrayExpr.getTy()).getBase() : TyUnknown.INSTANCE;
            List<MirOperand> fields = new ArrayList<>();
            for (ThirExpr field : arrayExpr.getFields()) {
                BlockAnd<MirOperand> fieldResult = toOperand(current, field, scope, NeedsTemporary.Maybe);
                current = fieldResult;
                fields.add(fieldResult.getElem());
            }
            return new BlockAnd<>(current.getBlock(), new MirRvalue.Aggregate.Array(elementType, fields));
        } else if (expr instanceof ThirExpr.Tuple) {
            ThirExpr.Tuple tupleExpr = (ThirExpr.Tuple) expr;
            BlockAnd<?> current = blockAnd;
            List<MirOperand> fields = new ArrayList<>();
            for (ThirExpr field : tupleExpr.getFields()) {
                BlockAnd<MirOperand> fieldResult = toOperand(current, field, scope, NeedsTemporary.Maybe);
                current = fieldResult;
                fields.add(fieldResult.getElem());
            }
            return new BlockAnd<>(current.getBlock(), new MirRvalue.Aggregate.Tuple(fields));
        } else if (expr instanceof ThirExpr.Repeat) {
            ThirExpr.Repeat repeatExpr = (ThirExpr.Repeat) expr;
            if (CtValue.asLong(repeatExpr.getCount()) == 0L) {
                return buildZeroRepeat(blockAnd, repeatExpr.getValue(), scope, sourceInfo(expr.getSpan()));
            } else {
                BlockAnd<MirOperand> operandResult = toOperand(blockAnd, repeatExpr.getValue(), scope, NeedsTemporary.No);
                return new BlockAnd<>(operandResult.getBlock(), new MirRvalue.Repeat(operandResult.getElem(), repeatExpr.getCount()));
            }
        } else if (expr instanceof ThirExpr.Cast) {
            ThirExpr.Cast castExpr = (ThirExpr.Cast) expr;
            BlockAnd<MirOperand> sourceResult = toOperand(blockAnd, castExpr.getSource(), scope, NeedsTemporary.No);
            MirTyples.MirCastTy fromTy = MirTyples.MirCastTy.from(castExpr.getSource().getTy());
            MirTyples.MirCastTy castTy = MirTyples.MirCastTy.from(castExpr.getTy());
            MirRvalue.Cast cast = MirUtils.createCast(fromTy, castTy, sourceResult.getElem(), castExpr.getTy());
            return new BlockAnd<>(sourceResult.getBlock(), cast);
        } else {
            BlockAnd<MirOperand> operandResult = toOperand(blockAnd, expr, scope, NeedsTemporary.No);
            return new BlockAnd<>(operandResult.getBlock(), new MirRvalue.Use(operandResult.getElem()));
        }
    }

    @NotNull
    private BlockAnd<MirRvalue> buildZeroRepeat(@NotNull BlockAnd<?> blockAnd, @NotNull ThirExpr value, @Nullable Scope scope, @NotNull MirSourceInfo outerSourceInfo) {
        MirBasicBlockImpl block = blockAnd.getBlock();
        if (!(value instanceof ThirExpr.ConstBlock || value instanceof ThirExpr.Literal
            || value instanceof ThirExpr.NonHirLiteral || value instanceof ThirExpr.ZstLiteral
            || value instanceof ThirExpr.ConstParam || value instanceof ThirExpr.StaticRef
            || value instanceof ThirExpr.NamedConst)) {
            BlockAnd<MirOperand> operandResult = toOperand(blockAnd, value, scope, NeedsTemporary.No);
            block = operandResult.getBlock();
            MirOperand valueOperand = operandResult.getElem();
            if (valueOperand instanceof MirOperand.Move) {
                MirPlace toDrop = ((MirOperand.Move) valueOperand).getPlace();
                MirBasicBlockImpl success = basicBlocks.newBlock();
                block.terminateWithDrop(toDrop, success, null, outerSourceInfo);
                divergeFrom(block);
                block = success;
            }
        }
        return new BlockAnd<>(block, new MirRvalue.Aggregate.Array(value.getTy(), Collections.emptyList()));
    }

    @NotNull
    private BlockAnd<Void> statementExpr(@NotNull BlockAnd<?> blockAnd, @NotNull ThirExpr expr, @Nullable Scope statementScope) {
        MirSourceInfo source = sourceInfo(expr.getSpan());
        if (expr instanceof ThirExpr.ScopeExpr) {
            ThirExpr.ScopeExpr scopeExpr = (ThirExpr.ScopeExpr) expr;
            return inScope(scopeExpr.getRegionScope(), () -> statementExpr(blockAnd, scopeExpr.getExpr(), statementScope));
        } else if (expr instanceof ThirExpr.Break) {
            ThirExpr.Break breakExpr = (ThirExpr.Break) expr;
            return breakScope(blockAnd, breakExpr.getExpr(), new BreakableTarget.Break(breakExpr.getLabel()), source);
        } else if (expr instanceof ThirExpr.Assign) {
            ThirExpr.Assign assignExpr = (ThirExpr.Assign) expr;
            if (MirUtils.needsDrop(assignExpr.getLeft().getTy())) {
                throw new UnsupportedOperationException("TODO");
            }
            BlockAnd<MirRvalue> rvalueResult = toLocalRvalue(blockAnd, assignExpr.getRight());
            BlockAnd<MirPlace> placeResult = toPlace(rvalueResult, assignExpr.getLeft());
            placeResult.getBlock().pushAssign(placeResult.getElem(), rvalueResult.getElem(), source);
            return placeResult.getBlock().andUnit();
        } else if (expr instanceof ThirExpr.AssignOp) {
            ThirExpr.AssignOp assignOpExpr = (ThirExpr.AssignOp) expr;
            BlockAnd<MirOperand> rightResult = toLocalOperand(blockAnd, assignOpExpr.getRight());
            BlockAnd<MirPlace> leftResult = toPlace(rightResult, assignOpExpr.getLeft());
            MirOperand leftOperand = new MirOperand.Copy(leftResult.getElem());
            BlockAnd<MirRvalue> result = buildBinaryOp(leftResult.getBlock(), leftOperand, rightResult.getElem(), assignOpExpr.getOp(), assignOpExpr.getLeft().getTy(), expr.getSpan());
            result.getBlock().pushAssign(leftResult.getElem(), result.getElem(), source);
            return result.getBlock().andUnit();
        } else {
            if (statementScope == null) {
                throw new IllegalStateException("Should not call statementExpr on a general expression without a statement scope");
            }
            BlockAnd<MirLocal> result = toTemp(blockAnd, expr, statementScope, Mutability.IMMUTABLE);
            return result.getBlock().andUnit();
        }
    }

    @NotNull
    private BlockAnd<Void> breakScope(@NotNull BlockAnd<?> blockAnd, @Nullable ThirExpr value, @NotNull BreakableTarget target, @NotNull MirSourceInfo source) {
        BreakableScope breakableScope;
        MirPlace destination;
        if (target instanceof BreakableTarget.Break) {
            Scope targetScope = ((BreakableTarget.Break) target).getScope();
            breakableScope = null;
            for (BreakableScope bs : scopes.reversedBreakableScopes()) {
                if (bs.getScope().equals(targetScope)) {
                    breakableScope = bs;
                    break;
                }
            }
            if (breakableScope == null) throw new IllegalStateException("No enclosing breakable scope found");
            destination = breakableScope.getBreakDestination();
        } else {
            throw new IllegalStateException("Unknown target type");
        }

        BlockAnd<?> resultBlockAnd;
        if (value != null) {
            resultBlockAnd = exprIntoPlace(blockAnd, value, destination);
        } else {
            resultBlockAnd = blockAnd;
            pushAssignUnit(resultBlockAnd.getBlock(), destination, source);
        }
        int scopeIndex = scopes.scopeIndex(breakableScope.getScope());
        DropTree drops = breakableScope.getBreakDrops();
        DropTree.DropNode dropNode = drops.getRoot();
        List<MirScope> scopesList = scopes.scopes();
        for (int i = scopeIndex + 1; i < scopesList.size(); i++) {
            for (Drop drop : scopesList.get(i).drops()) {
                dropNode = drops.addDrop(drop, dropNode);
            }
        }
        drops.addEntry(resultBlockAnd.getBlock(), dropNode);
        resultBlockAnd.getBlock().setTerminatorSource(source);
        return basicBlocks.newBlock().andUnit();
    }

    private void pushAssignUnit(@NotNull MirBasicBlockImpl block, @NotNull MirPlace place, @NotNull MirSourceInfo source) {
        block.pushAssign(
            place,
            new MirRvalue.Use(new MirOperand.Constant(MirConstant.zeroSized(TyUnit.INSTANCE, source.getSpan()))),
            source
        );
    }

    @NotNull
    private BlockAnd<MirRvalue> buildBinaryOp(
        @NotNull MirBasicBlockImpl block,
        @NotNull MirOperand left,
        @NotNull MirOperand right,
        @NotNull BinaryOperator op,
        @NotNull Ty ty,
        @NotNull MirSpan span
    ) {
        MirSourceInfo source = sourceInfo(span);
        if (checkOverflow && op instanceof ArithmeticOp && MirUtils.isCheckable((ArithmeticOp) op) && TyUtil.isIntegral(ty)) {
            Ty resultTy = new TyTuple(Arrays.asList(ty, TyBool.INSTANCE));
            MirPlace resultPlace = localDecls.newLocal(resultTy, outermostSourceInfo(span)).intoPlace();
            MirPlace value = resultPlace.makeField(0, ty);
            MirPlace overflow = resultPlace.makeField(1, TyBool.INSTANCE);
            MirBasicBlockImpl afterAssert = block
                .pushAssign(resultPlace, new MirRvalue.CheckedBinaryOpUse(MirBinaryOperator.toMir(op), left.toCopy(), right.toCopy()), source);
            afterAssert = doAssert(afterAssert, new MirOperand.Move(overflow), false, span, new MirAssertKind.Overflow((ArithmeticOp) op, left, right));
            return new BlockAnd<>(afterAssert, new MirRvalue.Use(new MirOperand.Move(value)));
        } else {
            if (TyUtil.isIntegral(ty) && (op == ArithmeticOp.DIV || op == ArithmeticOp.REM)) {
                ArithmeticOp arithOp = (ArithmeticOp) op;
                MirAssertKind zeroAssert = (arithOp == ArithmeticOp.DIV)
                    ? new MirAssertKind.DivisionByZero(left.toCopy())
                    : new MirAssertKind.ReminderByZero(left.toCopy());
                MirAssertKind overflowAssert = new MirAssertKind.Overflow(arithOp, left.toCopy(), right.toCopy());

                MirPlace isZero = localDecls.newLocal(TyBool.INSTANCE, outermostSourceInfo(span)).intoPlace();
                MirOperand zero = new MirOperand.Constant(toConstant(0, ty, span));
                block.pushAssign(isZero, new MirRvalue.BinaryOpUse(MirBinaryOperator.toMir(EqualityOp.EQ), right.toCopy(), zero), source);
                MirBasicBlockImpl afterZeroAssert = doAssert(block, new MirOperand.Move(isZero), false, span, zeroAssert);
                MirBasicBlockImpl afterOverflow = assertDivOverflow(afterZeroAssert, left, right, ty, overflowAssert, source);
                return new BlockAnd<>(afterOverflow, new MirRvalue.BinaryOpUse(MirBinaryOperator.toMir(op), left, right));
            }
            return new BlockAnd<>(block, new MirRvalue.BinaryOpUse(MirBinaryOperator.toMir(op), left, right));
        }
    }

    @NotNull
    private MirBasicBlockImpl assertDivOverflow(
        @NotNull MirBasicBlockImpl block,
        @NotNull MirOperand left,
        @NotNull MirOperand right,
        @NotNull Ty ty,
        @NotNull MirAssertKind assertKind,
        @NotNull MirSourceInfo source
    ) {
        if (!MirUtils.isSigned(ty)) return block;
        if (!(ty instanceof TyInteger)) throw new IllegalStateException("Expected TyInteger");
        TyInteger intTy = (TyInteger) ty;
        MirOperand negOne = new MirOperand.Constant(toConstant(-1, ty, source.getSpan()));
        MirOperand min = new MirOperand.Constant(toConstant(MirUtils.getMinValue(intTy), ty, source.getSpan()));
        MirPlace isNegOne = localDecls.newLocal(TyBool.INSTANCE, source).intoPlace();
        MirPlace isMin = localDecls.newLocal(TyBool.INSTANCE, source).intoPlace();
        MirPlace overflow = localDecls.newLocal(TyBool.INSTANCE, source).intoPlace();

        block.pushAssign(isNegOne, new MirRvalue.BinaryOpUse(MirBinaryOperator.toMir(EqualityOp.EQ), right.toCopy(), negOne), source);
        block.pushAssign(isMin, new MirRvalue.BinaryOpUse(MirBinaryOperator.toMir(EqualityOp.EQ), left.toCopy(), min), source);
        block.pushAssign(overflow, new MirRvalue.BinaryOpUse(MirBinaryOperator.toMir(ArithmeticOp.BIT_AND), new MirOperand.Move(isNegOne), new MirOperand.Move(isMin)), source);
        return doAssert(block, new MirOperand.Move(overflow), false, source.getSpan(), assertKind);
    }

    @NotNull
    private BlockAnd<MirOperand> assertNoNegOverflow(
        @NotNull BlockAnd<MirOperand> blockAnd,
        @NotNull ThirExpr.Unary kind,
        @NotNull Ty type,
        @NotNull MirSourceInfo source
    ) {
        boolean needsAssertion = checkOverflow && kind.getOp() == UnaryOperator.MINUS && MirUtils.isSigned(type);
        if (!needsAssertion) return blockAnd;
        if (!(type instanceof TyInteger)) throw new IllegalStateException("Expected TyInteger");
        TyInteger intTy = (TyInteger) type;
        MirPlace isMin = localDecls.newLocal(TyBool.INSTANCE, source).intoPlace();
        MirRvalue eq = new MirRvalue.BinaryOpUse(
            MirBinaryOperator.toMir(EqualityOp.EQ),
            blockAnd.getElem().toCopy(),
            new MirOperand.Constant(toConstant(MirUtils.getMinValue(intTy), type, source.getSpan()))
        );
        MirBasicBlockImpl afterAssign = blockAnd.getBlock().pushAssign(isMin, eq, source);
        MirBasicBlockImpl afterAssert = doAssert(afterAssign, new MirOperand.Move(isMin), false, source.getSpan(), new MirAssertKind.OverflowNeg(blockAnd.getElem().toCopy()));
        return new BlockAnd<>(afterAssert, blockAnd.getElem());
    }

    @NotNull
    private Scope localScope() {
        return scopes.topmost();
    }

    @NotNull
    private <R> BlockAnd<R> inScope(@Nullable Scope scope, @NotNull Supplier<BlockAnd<R>> body) {
        MirSourceScope sourceScope = sourceScopes.getSourceScope();
        if (scope != null) {
            pushScope(scope);
        }
        BlockAnd<R> res = body.get();
        if (scope != null) {
            res = popScope(res);
        }
        sourceScopes.setSourceScope(sourceScope);
        return res;
    }

    @NotNull
    private BlockAnd<MirBasicBlockImpl> inIfThenScope(@NotNull Scope scope, @NotNull MirSpan source, @NotNull Supplier<BlockAnd<?>> body) {
        IfThenScope prevScope = scopes.getIfThenScope();
        scopes.setIfThenScope(new IfThenScope(scope, new DropTree()));
        MirBasicBlockImpl thenBlock = body.get().getBlock();
        IfThenScope ifThenScope = scopes.getIfThenScope();
        scopes.setIfThenScope(prevScope);
        if (ifThenScope == null) throw new IllegalStateException("ifThenScope is null");
        BlockAnd<Void> exitResult = buildExitTree(ifThenScope.getElseDrops(), ifThenScope.getScope(), source, null);
        MirBasicBlockImpl elseBlock = exitResult != null ? exitResult.getBlock() : basicBlocks.newBlock();
        return new BlockAnd<>(thenBlock, elseBlock);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private <R> BlockAnd<R> inBreakableScope(
        @Nullable MirBasicBlockImpl loopBlock,
        @NotNull MirPlace breakDestination,
        @NotNull MirSpan span,
        @NotNull Supplier<BlockAnd<R>> action
    ) {
        Scope regionScope = scopes.topmost();
        BreakableScope scope = new BreakableScope(
            regionScope, breakDestination, new DropTree(),
            loopBlock != null ? new DropTree() : null
        );
        scopes.pushBreakable(scope);
        BlockAnd<R> normalExitBlock = action.get();
        scopes.popBreakable();
        BlockAnd<Void> breakBlock = buildExitTree(scope.getBreakDrops(), regionScope, span, null);
        if (scope.getContinueDrops() != null) {
            buildExitTree(scope.getContinueDrops(), regionScope, span, loopBlock);
        }
        if (normalExitBlock != null && breakBlock == null) {
            return normalExitBlock;
        } else if (normalExitBlock == null && breakBlock != null) {
            return (BlockAnd<R>) breakBlock;
        } else if (normalExitBlock != null) {
            MirBasicBlockImpl target = basicBlocks.newBlock();
            MirSourceInfo src = sourceInfo(span);
            normalExitBlock.getBlock().terminateWithGoto(target, src);
            breakBlock.getBlock().terminateWithGoto(target, src);
            return (BlockAnd<R>) target.andUnit();
        } else {
            return (BlockAnd<R>) basicBlocks.newBlock().andUnit();
        }
    }

    @Nullable
    private BlockAnd<Void> buildExitTree(
        @NotNull DropTree drops,
        @NotNull Scope elseScope,
        @NotNull MirSpan source,
        @Nullable MirBasicBlockImpl continueBlock
    ) {
        Map<DropTree.DropNode, MirBasicBlockImpl> blocks = drops.buildMir(new ExitScopes(basicBlocks), continueBlock);
        MirBasicBlockImpl root = blocks.get(drops.getRoot());
        return root != null ? root.andUnit() : null;
    }

    private void pushScope(@NotNull Scope regionScope) {
        scopes.push(new MirScope(sourceScopes.getSourceScope(), regionScope));
    }

    @NotNull
    private <T> BlockAnd<T> popScope(@NotNull BlockAnd<T> blockAnd) {
        BlockAnd<T> result = buildDrops(blockAnd, scopes.last());
        scopes.pop();
        return result;
    }

    @NotNull
    private <T> BlockAnd<T> buildDrops(@NotNull BlockAnd<T> blockAnd, @NotNull MirScope scope) {
        Iterator<Drop> reversedDropsIter = scope.reversedDrops();
        while (reversedDropsIter.hasNext()) {
            Drop drop = reversedDropsIter.next();
            if (drop.getKind() == Drop.Kind.VALUE) {
                // TODO
            } else if (drop.getKind() == Drop.Kind.STORAGE) {
                blockAnd.getBlock().pushStorageDead(drop.getLocal(), drop.getSource());
            }
        }
        return blockAnd;
    }

    @NotNull
    private MirBasicBlockImpl doAssert(
        @NotNull MirBasicBlockImpl block,
        @NotNull MirOperand cond,
        boolean expected,
        @NotNull MirSpan span,
        @NotNull MirAssertKind msg
    ) {
        MirBasicBlockImpl successBlock = basicBlocks.newBlock();
        block.terminateWithAssert(cond, expected, successBlock, sourceInfo(span), msg);
        divergeFrom(block);
        return successBlock;
    }

    private void divergeFrom(@NotNull MirBasicBlockImpl block) {
        DropTree.DropNode nextDrop = divergeCleanup();
        scopes.getUnwindDrops().addEntry(block, nextDrop);
    }

    @NotNull
    private DropTree.DropNode divergeCleanup() {
        return divergeCleanup(scopes.topmost());
    }

    @NotNull
    private DropTree.DropNode divergeCleanup(@NotNull Scope target) {
        int targetIndex = scopes.scopeIndex(target);
        DropTree.DropNode drop = scopes.getUnwindDrops().getRoot();
        int scopeIndex = 0;
        List<MirScope> scopeList = scopes.scopes();
        for (int i = Math.min(targetIndex, scopeList.size() - 1); i >= 0; i--) {
            DropTree.DropNode cached = scopeList.get(i).getCachedUnwindDrop();
            if (cached != null) {
                drop = cached;
                scopeIndex = i;
                break;
            }
        }

        if (scopeIndex > targetIndex) return drop;

        for (int i = scopeIndex; i <= targetIndex && i < scopeList.size(); i++) {
            scopeList.get(i).setCachedUnwindDrop(drop);
        }

        return drop;
    }

    @NotNull
    private BlockAnd<MirOperand> toOperand(
        @NotNull BlockAnd<?> blockAnd,
        @NotNull ThirExpr expr,
        @Nullable Scope scope,
        @NotNull NeedsTemporary needsTemporary
    ) {
        if (expr instanceof ThirExpr.ScopeExpr) {
            ThirExpr.ScopeExpr scopeExpr = (ThirExpr.ScopeExpr) expr;
            return inScope(scopeExpr.getRegionScope(), () -> toOperand(blockAnd, scopeExpr.getExpr(), scope, needsTemporary));
        }

        MirCategory category = MirCategory.of(expr);
        if (category instanceof MirCategory.Constant) {
            if (needsTemporary == NeedsTemporary.No || !MirUtils.needsDrop(expr.getTy())) {
                MirConstant constant = toConstant(expr);
                return new BlockAnd<>(blockAnd.getBlock(), new MirOperand.Constant(constant));
            } else {
                BlockAnd<MirLocal> tempResult = toTemp(blockAnd, expr, scope, Mutability.MUTABLE);
                return new BlockAnd<>(tempResult.getBlock(), new MirOperand.Move(new MirPlace(tempResult.getElem())));
            }
        } else if (category instanceof MirCategory.Place || category instanceof MirCategory.Rvalue) {
            BlockAnd<MirLocal> tempResult = toTemp(blockAnd, expr, scope, Mutability.MUTABLE);
            return new BlockAnd<>(tempResult.getBlock(), new MirOperand.Move(new MirPlace(tempResult.getElem())));
        } else {
            throw new UnsupportedOperationException("TODO");
        }
    }

    @NotNull
    private BlockAnd<MirLocal> toTemp(@NotNull BlockAnd<?> blockAnd, @NotNull ThirExpr expr, @Nullable Scope tempLifetime, @NotNull Mutability mutability) {
        if (expr instanceof ThirExpr.ScopeExpr) {
            ThirExpr.ScopeExpr scopeExpr = (ThirExpr.ScopeExpr) expr;
            return inScope(scopeExpr.getRegionScope(), () -> toTemp(blockAnd, scopeExpr.getExpr(), tempLifetime, mutability));
        }
        MirPlace localPlace = localDecls.newLocal(mutability, false, null, null, expr.getTy(), outermostSourceInfo(expr.getSpan())).intoPlace();
        MirSourceInfo source = sourceInfo(expr.getSpan());
        if (!(expr instanceof ThirExpr.Break || expr instanceof ThirExpr.Continue || expr instanceof ThirExpr.Return)) {
            if (expr instanceof ThirExpr.Block && ((ThirExpr.Block) expr).getBlock().getExpr() == null && expr.getTy() instanceof TyNever) {
                // TODO: check targeted_by_break
            } else {
                blockAnd.getBlock().pushStorageLive(localPlace.getLocal(), source);
                if (tempLifetime != null) {
                    scheduleDrop(tempLifetime, localPlace.getLocal(), Drop.Kind.STORAGE);
                }
            }
        }
        if (tempLifetime != null) {
            scheduleDrop(tempLifetime, localPlace.getLocal(), Drop.Kind.VALUE);
        }
        BlockAnd<Void> result = exprIntoPlace(blockAnd, expr, localPlace);
        return new BlockAnd<>(result.getBlock(), localPlace.getLocal());
    }

    private void scheduleDrop(@NotNull Scope regionScope, @NotNull MirLocal local, @NotNull Drop.Kind dropKind) {
        boolean needsDrop;
        if (dropKind == Drop.Kind.VALUE) {
            if (!MirUtils.needsDrop(local.getTy())) return;
            needsDrop = true;
        } else {
            needsDrop = false;
        }

        for (MirScope scope : scopes.scopes(true)) {
            if (needsDrop) scope.invalidateCaches();
            if (scope.getRegionScope().equals(regionScope)) {
                MirSpan regionScopeSource = MirUtils.getSpan(regionScope);
                MirSourceInfo sourceInfo = new MirSourceInfo(regionScopeSource.getEndPoint(), scope.getSourceScope());
                scope.addDrop(new Drop(local, dropKind, sourceInfo));
                return;
            }
        }

        throw new IllegalStateException("Corresponding scope is not found");
    }

    @NotNull
    private MirConstant toConstant(@NotNull ThirExpr expr) {
        if (expr instanceof ThirExpr.Literal) {
            ThirExpr.Literal literal = (ThirExpr.Literal) expr;
            if (RsLitExprUtil.getIntegerValue(literal.getLiteral()) != null && TyUtil.isIntegral(expr.getTy())) {
                long value = RsLitExprUtil.getIntegerValue(literal.getLiteral());
                if (literal.getNeg()) value = -value;
                return toConstant(value, expr.getTy(), expr.getSpan());
            } else if (RsLitExprUtil.getBooleanValue(literal.getLiteral()) != null && expr.getTy() instanceof TyBool) {
                return toConstant(RsLitExprUtil.getBooleanValue(literal.getLiteral()), expr.getTy(), expr.getSpan());
            } else {
                throw new UnsupportedOperationException("TODO");
            }
        } else if (expr instanceof ThirExpr.ZstLiteral) {
            return MirConstant.zeroSized(expr.getTy(), expr.getSpan());
        } else if (expr instanceof ThirExpr.NamedConst) {
            ThirExpr.NamedConst namedConst = (ThirExpr.NamedConst) expr;
            return new MirConstant.Unevaluated(namedConst.getDef(), expr.getTy(), expr.getSpan());
        } else {
            throw new IllegalStateException("expression is not a valid constant: " + expr);
        }
    }

    @NotNull
    private MirConstant toConstant(boolean bool, @NotNull Ty ty, @NotNull MirSpan source) {
        return toConstant(bool ? 1L : 0L, ty, source);
    }

    @NotNull
    private MirConstant toConstant(long value, @NotNull Ty ty, @NotNull MirSpan source) {
        MirScalarInt scalarInt = new MirScalarInt(value, (byte) 0);
        return new MirConstant.Value(
            new MirConstValue.Scalar(new MirScalar.Int(scalarInt)),
            ty,
            source
        );
    }

    @NotNull
    private MirSourceInfo outermostSourceInfo(@NotNull MirSpan span) {
        return new MirSourceInfo(span, sourceScopes.getOutermost());
    }

    @NotNull
    public MirSourceInfo sourceInfo(@NotNull MirSpan span) {
        return new MirSourceInfo(span, sourceScopes.getSourceScope());
    }

    private boolean isLet(@NotNull ThirExpr expr) {
        if (expr instanceof ThirExpr.Let) return true;
        if (expr instanceof ThirExpr.ScopeExpr) return isLet(((ThirExpr.ScopeExpr) expr).getExpr());
        return false;
    }

    // === Static build methods (companion object equivalent) ===

    @NotNull
    public static MirBody build(@NotNull RsConstant constant) {
        MirBuilder builder = new MirBuilder(
            constant,
            ImplLookup.relativeTo(constant),
            new MirrorContext(constant),
            true,
            MirUtils.asSpan(constant),
            0,
            ExtensionsUtil.getNormType(constant.getTypeReference()),
            MirUtils.asSpan(constant.getTypeReference())
        );
        return builder.buildConstant(Thir.from(constant));
    }

    @NotNull
    public static MirBody build(@NotNull RsFunction function) {
        MirSpan returnSpan;
        if (function.getRetType() != null && function.getRetType().getTypeReference() != null) {
            returnSpan = MirUtils.asSpan(function.getRetType().getTypeReference());
        } else if (RsFunctionUtil.getBlock(function) != null) {
            returnSpan = MirUtils.asStartSpan(RsFunctionUtil.getBlock(function));
        } else {
            throw new IllegalStateException("Could not get block of function");
        }

        RsBlock body = RsFunctionUtil.getBlock(function);
        if (body == null) throw new IllegalStateException("Could not get block of function");

        Thir thir = Thir.from(function);

        MirBuilder builder = new MirBuilder(
            function,
            ImplLookup.relativeTo(function),
            new MirrorContext(function),
            true,
            MirUtils.asSpan(function),
            thir.getParams().size(),
            RsFunctionUtil.getNormReturnType(function),
            returnSpan
        );

        return builder.buildFunction(thir, body);
    }
}
