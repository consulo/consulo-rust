/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.dfa.MemoryCategorization.*;
import org.rust.lang.core.dfa.liveness.Liveness;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ResolveUtil;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.SubstitutionUtil;
import org.rust.lang.core.types.regions.ReScope;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.regions.Scope;
import org.rust.lang.core.types.ty.*;

import java.util.List;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.macros.MacroExpansion;
import org.rust.lang.core.resolve.NameResolution;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.types.infer.FoldUtil;

// ---- Delegate interface ----

public class ExprUseWalker {

    public interface Delegate {
        void consume(@Nonnull RsElement element, @Nonnull Cmt cmt, @Nonnull ConsumeMode mode);

        void matchedPat(@Nonnull RsPat pat, @Nonnull Cmt cmt, @Nonnull MatchMode mode);

        void consumePat(@Nonnull RsPat pat, @Nonnull Cmt cmt, @Nonnull ConsumeMode mode);

        void declarationWithoutInit(@Nonnull RsPatBinding binding);

        void mutate(@Nonnull RsElement assignmentElement, @Nonnull Cmt assigneeCmt, @Nonnull MutateMode mode);

        void useElement(@Nonnull RsElement element, @Nonnull Cmt cmt);
    }

    // ---- ConsumeMode ----
    public static abstract class ConsumeMode {
        private ConsumeMode() {
        }

        public static final ConsumeMode Copy = new ConsumeMode() {
        };

        public static class Move extends ConsumeMode {
            @Nonnull
            public final MoveReason reason;

            public Move(@Nonnull MoveReason reason) {
                this.reason = reason;
            }
        }

        @Nonnull
        public MatchMode getMatchMode() {
            if (this == Copy) return MatchMode.CopyingMatch;
            return MatchMode.MovingMatch;
        }
    }

    // ---- MoveReason ----
    public enum MoveReason {
        DirectRefMove, PatBindingMove, CaptureMove
    }

    // ---- MatchMode ----
    public enum MatchMode {
        NonBindingMatch, BorrowingMatch, CopyingMatch, MovingMatch, NonConsumingMatch
    }

    // ---- TrackMatchMode ----
    public static abstract class TrackMatchMode {
        private TrackMatchMode() {
        }

        public static final TrackMatchMode Unknown = new TrackMatchMode() {
        };

        public static class Definite extends TrackMatchMode {
            @Nonnull
            public final MatchMode mode;

            public Definite(@Nonnull MatchMode mode) {
                this.mode = mode;
            }
        }

        public static final TrackMatchMode Conflicting = new TrackMatchMode() {
        };

        @Nonnull
        public MatchMode getMatchMode() {
            if (this == Unknown) return MatchMode.NonBindingMatch;
            if (this instanceof Definite) return ((Definite) this).mode;
            return MatchMode.MovingMatch;
        }

        @Nonnull
        public TrackMatchMode leastUpperBound(@Nonnull MatchMode mode) {
            if (this == Unknown) return new Definite(mode);
            if (this instanceof Definite) {
                Definite d = (Definite) this;
                if (d.mode == mode) return this;
                if (mode == MatchMode.NonBindingMatch) return this;
                if (d.mode == MatchMode.NonBindingMatch) return new Definite(mode);
                if (mode == MatchMode.CopyingMatch) return this;
                if (d.mode == MatchMode.CopyingMatch) return new Definite(mode);
                return Conflicting;
            }
            return this;
        }
    }

    // ---- MutateMode ----
    public enum MutateMode {
        Init, JustWrite, WriteAndRead
    }

    // ---- Utility ----
    @Nonnull
    public static ConsumeMode copyOrMove(@Nonnull MemoryCategorizationContext mc, @Nonnull Cmt cmt, @Nonnull MoveReason moveReason) {
        if (TyUtil.isMovesByDefault(cmt.ty, mc.lookup)) {
            return new ConsumeMode.Move(moveReason);
        }
        return ConsumeMode.Copy;
    }

    // ---- Walker ----

    @Nonnull
    private final Delegate delegate;
    @Nonnull
    private final MemoryCategorizationContext mc;

    public ExprUseWalker(@Nonnull Delegate delegate, @Nonnull MemoryCategorizationContext mc) {
        this.delegate = delegate;
        this.mc = mc;
    }

    public void consumeBody(@Nonnull RsBlock body) {
        if (!(body.getParent() instanceof RsFunction)) return;
        RsFunction function = (RsFunction) body.getParent();

        for (RsValueParameter parameter : function.getValueParameters()) {
            if (parameter.getTypeReference() == null) continue;
            Ty parameterType = ExtensionsUtil.normType(parameter.getTypeReference(), mc.lookup);
            RsPat parameterPat = parameter.getPat();
            if (parameterPat == null) continue;

            Region bodyScopeRegion = new ReScope(new Scope.Node(body));
            Cmt parameterCmt = mc.processRvalue(parameter, bodyScopeRegion, parameterType);
            walkIrrefutablePat(parameterCmt, parameterPat);
        }

        walkBlock(body);
    }

    private void delegateConsume(@Nonnull RsElement element, @Nonnull Cmt cmt) {
        ConsumeMode mode = copyOrMove(mc, cmt, MoveReason.DirectRefMove);
        delegate.consume(element, cmt, mode);
    }

    private void consumeExprs(@Nonnull List<RsExpr> exprs) {
        for (RsExpr expr : exprs) {
            consumeExpr(expr);
        }
    }

    private void consumeExpr(@Nonnull RsExpr expr) {
        Cmt cmt = mc.processExpr(expr);
        delegateConsume(expr, cmt);
        walkExpr(expr);
    }

    private void usePath(@Nonnull RsPathExpr pathExpr) {
        Cmt cmt = mc.processExpr(pathExpr);
        delegate.useElement(pathExpr, cmt);
    }

    private void useAllPaths(@Nonnull RsElement element) {
        if (element instanceof RsPathExpr) {
            usePath((RsPathExpr) element);
        }
        for (RsPathExpr path : RsElementUtil.descendantsOfType(element, RsPathExpr.class)) {
            usePath(path);
        }
    }

    private void useMacroBodyIdent(@Nonnull RsMacroBodyIdent ident) {
        String referenceName = ident.getReferenceName();
        if (referenceName == null) return;
        RsElement resolved = (RsElement) org.rust.lang.core.resolve.NameResolution.findInScope(ident, referenceName, org.rust.lang.core.resolve.Namespace.VALUES);
        if (!(resolved instanceof RsPatBinding)) return;
        RsPatBinding declaration = (RsPatBinding) resolved;
        Mutability mutability = RsPatBindingUtil.getMutability(declaration);
        Ty type = RsTypesUtil.getType(declaration);
        Cmt cmt = new Cmt(ident, new Categorization.Local(declaration), MutabilityCategory.from(mutability), type);
        delegate.useElement(ident, cmt);
    }

    private void useAllMacroBodyIdents(@Nonnull RsElement element) {
        for (RsMacroBodyIdent ident : RsElementUtil.descendantsOfType(element, RsMacroBodyIdent.class)) {
            useMacroBodyIdent(ident);
        }
    }

    private void mutateExpr(@Nonnull RsExpr assignmentExpr, @Nonnull RsExpr expr, @Nonnull MutateMode mode) {
        Cmt cmt = mc.processExpr(expr);
        delegate.mutate(assignmentExpr, cmt, mode);
        walkExpr(expr);
    }

    private void selectFromExpr(@Nonnull RsExpr expr) {
        walkExpr(expr);
        useAllPaths(expr);
    }

    private void walkExpr(@Nonnull RsExpr expr) {
        if (expr instanceof RsUnaryExpr) {
            RsUnaryExpr unaryExpr = (RsUnaryExpr) expr;
            RsExpr base = unaryExpr.getExpr();
            if (base == null) return;
            if (unaryExpr.getMul() != null) selectFromExpr(base);
            else if (unaryExpr.getAnd() != null) selectFromExpr(base);
            else consumeExpr(base);
        } else if (expr instanceof RsDotExpr) {
            RsDotExpr dotExpr = (RsDotExpr) expr;
            RsExpr base = dotExpr.getExpr();
            if (dotExpr.getFieldLookup() != null) {
                selectFromExpr(base);
            } else if (dotExpr.getMethodCall() != null) {
                selectFromExpr(base);
                consumeExprs(dotExpr.getMethodCall().getValueArgumentList().getExprList());
            }
        } else if (expr instanceof RsIndexExpr) {
            RsIndexExpr indexExpr = (RsIndexExpr) expr;
            selectFromExpr(RsIndexExprUtil.getContainerExpr(indexExpr));
            RsExpr indexE = RsIndexExprUtil.getIndexExpr(indexExpr);
            if (indexE != null) consumeExpr(indexE);
        } else if (expr instanceof RsCallExpr) {
            RsCallExpr callExpr = (RsCallExpr) expr;
            walkCallee(callExpr.getExpr());
            consumeExprs(callExpr.getValueArgumentList().getExprList());
        } else if (expr instanceof RsStructLiteral) {
            RsStructLiteral structLiteral = (RsStructLiteral) expr;
            walkStructExpr(structLiteral.getStructLiteralBody().getStructLiteralFieldList(), structLiteral.getStructLiteralBody().getExpr());
        } else if (expr instanceof RsTupleExpr) {
            consumeExprs(((RsTupleExpr) expr).getExprList());
        } else if (expr instanceof RsIfExpr) {
            RsIfExpr ifExpr = (RsIfExpr) expr;
            RsCondition cond = ifExpr.getCondition();
            if (cond != null && cond.getExpr() != null) walkExpr(cond.getExpr());
            if (ifExpr.getBlock() != null) walkBlock(ifExpr.getBlock());
            RsElseBranch elseBranch = ifExpr.getElseBranch();
            if (elseBranch != null) {
                if (elseBranch.getIfExpr() != null) walkExpr(elseBranch.getIfExpr());
                if (elseBranch.getBlock() != null) walkBlock(elseBranch.getBlock());
            }
        } else if (expr instanceof RsMatchExpr) {
            RsMatchExpr matchExpr = (RsMatchExpr) expr;
            RsExpr discriminant = matchExpr.getExpr();
            if (discriminant == null) return;
            Cmt discriminantCmt = mc.processExpr(discriminant);
            selectFromExpr(discriminant);
            for (RsMatchArm arm : RsMatchExprUtil.getArms(matchExpr)) {
                MatchMode mode = armMoveMode(discriminantCmt, arm).getMatchMode();
                walkArm(discriminantCmt, arm, mode);
            }
        } else if (expr instanceof RsArrayExpr) {
            consumeExprs(((RsArrayExpr) expr).getExprList());
        } else if (expr instanceof RsLoopExpr) {
            RsBlock block = ((RsLoopExpr) expr).getBlock();
            if (block != null) walkBlock(block);
        } else if (expr instanceof RsWhileExpr) {
            RsWhileExpr whileExpr = (RsWhileExpr) expr;
            RsCondition cond = whileExpr.getCondition();
            if (cond != null && cond.getExpr() != null) walkExpr(cond.getExpr());
            if (whileExpr.getBlock() != null) walkBlock(whileExpr.getBlock());
        } else if (expr instanceof RsForExpr) {
            RsForExpr forExpr = (RsForExpr) expr;
            RsExpr init = forExpr.getExpr();
            RsPat pat = forExpr.getPat();
            if (init != null && pat != null) {
                walkExpr(init);
                Cmt initCmt = mc.processExpr(init);
                walkPat(initCmt, pat, MatchMode.NonConsumingMatch);
            }
            if (forExpr.getBlock() != null) walkBlock(forExpr.getBlock());
        } else if (expr instanceof RsBinaryExpr) {
            RsBinaryExpr binaryExpr = (RsBinaryExpr) expr;
            RsExpr left = binaryExpr.getLeft();
            RsExpr right = binaryExpr.getRight();
            if (right == null) return;
            BinaryOperator op = RsBinaryOpUtil.getOperatorType(binaryExpr.getBinaryOp());
            if (op instanceof ArithmeticAssignmentOp) {
                mutateExpr(binaryExpr, left, MutateMode.WriteAndRead);
            } else if (op instanceof AssignmentOp) {
                mutateExpr(binaryExpr, left, MutateMode.JustWrite);
            } else {
                consumeExpr(left);
            }
            consumeExpr(right);
        } else if (expr instanceof RsLambdaExpr) {
            RsExpr lambdaBody = ((RsLambdaExpr) expr).getExpr();
            if (lambdaBody != null) walkExpr(lambdaBody);
        } else if (expr instanceof RsBlockExpr) {
            walkBlock(((RsBlockExpr) expr).getBlock());
        } else if (expr instanceof RsBreakExpr) {
            RsExpr breakVal = ((RsBreakExpr) expr).getExpr();
            if (breakVal != null) consumeExpr(breakVal);
        } else if (expr instanceof RsRetExpr) {
            RsExpr retVal = ((RsRetExpr) expr).getExpr();
            if (retVal != null) consumeExpr(retVal);
        } else if (expr instanceof RsCastExpr) {
            consumeExpr(((RsCastExpr) expr).getExpr());
        } else if (expr instanceof RsParenExpr) {
            RsExpr inner = ((RsParenExpr) expr).getExpr();
            if (inner != null) walkExpr(inner);
        } else if (expr instanceof RsTryExpr) {
            walkExpr(((RsTryExpr) expr).getExpr());
        } else if (expr instanceof RsMacroExpr) {
            walkMacroCall(((RsMacroExpr) expr).getMacroCall());
        } else if (expr instanceof RsPathExpr) {
            usePath((RsPathExpr) expr);
        } else if (expr instanceof RsRangeExpr) {
            for (RsExpr e : ((RsRangeExpr) expr).getExprList()) walkExpr(e);
        } else if (expr instanceof RsLetExpr) {
            walkLetExpr((RsLetExpr) expr);
        }
    }

    private void walkCallee(@Nonnull RsExpr callee) {
        if (RsExprUtil.getType(callee) instanceof TyFunctionBase) {
            consumeExpr(callee);
        } else {
            useAllPaths(callee);
        }
    }

    private void walkMacroCall(@Nonnull RsMacroCall macroCall) {
        Object argument = RsMacroCallUtil.getMacroArgumentElement(macroCall);

        if (argument instanceof RsExprMacroArgument) {
            RsExpr e = ((RsExprMacroArgument) argument).getExpr();
            if (e != null) walkExpr(e);
        } else if (argument instanceof RsIncludeMacroArgument) {
            RsExpr e = ((RsIncludeMacroArgument) argument).getExpr();
            if (e != null) walkExpr(e);
        } else if (argument instanceof RsConcatMacroArgument) {
            for (RsExpr e : ((RsConcatMacroArgument) argument).getExprList()) walkExpr(e);
        } else if (argument instanceof RsEnvMacroArgument) {
            for (RsExpr e : ((RsEnvMacroArgument) argument).getExprList()) walkExpr(e);
        } else if (argument instanceof RsVecMacroArgument) {
            for (RsExpr e : ((RsVecMacroArgument) argument).getExprList()) walkExpr(e);
        } else if (argument instanceof RsFormatMacroArgument) {
            Object expansion = RsMacroCallUtil.getExpansion(macroCall);
            if (expansion != null) {
                for (RsElement el : ((org.rust.lang.core.macros.MacroExpansion) expansion).getElements()) walk(el);
            } else {
                for (RsFormatMacroArg a : ((RsFormatMacroArgument) argument).getFormatMacroArgList()) walkExpr(a.getExpr());
            }
        } else if (argument instanceof RsAssertMacroArgument) {
            RsExpr e = ((RsAssertMacroArgument) argument).getExpr();
            if (e != null) walkExpr(e);
            for (RsFormatMacroArg a : ((RsAssertMacroArgument) argument).getFormatMacroArgList()) walkExpr(a.getExpr());
        } else if (argument instanceof RsAsmMacroArgument) {
            // TODO: Handle this case
        } else if (argument instanceof RsMacroArgument) {
            Object expansion = RsMacroCallUtil.getExpansion(macroCall);
            if (expansion != null) {
                for (RsElement el : ((org.rust.lang.core.macros.MacroExpansion) expansion).getElements()) walk(el);
            } else {
                useAllPaths(macroCall);
                useAllMacroBodyIdents(macroCall);
            }
        } else if (argument == null) {
            useAllMacroBodyIdents(macroCall);
        }
    }

    private void walkStmt(@Nonnull RsStmt stmt) {
        if (stmt instanceof RsLetDecl) walkLet((RsLetDecl) stmt);
        else if (stmt instanceof RsExprStmt) consumeExpr(((RsExprStmt) stmt).getExpr());
    }

    private void walkLet(@Nonnull RsLetDecl declaration) {
        RsExpr init = declaration.getExpr();
        RsPat pat = declaration.getPat();
        if (pat != null) {
            if (init != null) {
                walkExpr(init);
                Cmt initCmt = mc.processExpr(init);
                walkIrrefutablePat(initCmt, pat);
            } else {
                for (RsPatBinding binding : RsElementUtil.descendantsOfType(pat, RsPatBinding.class)) {
                    delegate.declarationWithoutInit(binding);
                }
            }
        }
        RsLetElseBranch elseBranch = declaration.getLetElseBranch();
        if (elseBranch != null && elseBranch.getBlock() != null) {
            walkBlock(elseBranch.getBlock());
        }
    }

    private void walkLetExpr(@Nonnull RsLetExpr letExpr) {
        RsExpr init = letExpr.getExpr();
        if (init == null) return;
        walkExpr(init);
        Cmt initCmt = mc.processExpr(init);
        List<RsPat> pats = RsLetExprUtil.getPatList(letExpr);
        if (pats != null) {
            for (RsPat pat : pats) {
                walkIrrefutablePat(initCmt, pat);
            }
        }
    }

    private void walkBlock(@Nonnull RsBlock block) {
        RsBlockUtil.ExpandedStmtsAndTailExpr expanded = RsBlockUtil.getExpandedStmtsAndTailExpr(block);
        for (RsElement element : expanded.getStmts()) {
            walk(element);
        }
        RsExpr tailExpr = expanded.getTailExpr();
        if (tailExpr != null) {
            consumeExpr(tailExpr);
        }
    }

    private void walk(@Nonnull RsElement element) {
        if (element instanceof RsStmt) walkStmt((RsStmt) element);
        else if (element instanceof RsExpr) walkExpr((RsExpr) element);
        else if (element instanceof RsMacroCall) walkMacroCall((RsMacroCall) element);
    }

    private void walkStructExpr(@Nonnull List<RsStructLiteralField> fields, @Nullable RsExpr withExpr) {
        for (RsStructLiteralField field : fields) {
            RsExpr fieldExpr = field.getExpr();
            if (fieldExpr != null) {
                consumeExpr(fieldExpr);
            } else if (field.getIdentifier() != null) {
                RsPatBinding binding = RsStructLiteralFieldUtil.resolveToBinding(field);
                if (binding == null) continue;
                Mutability mutability = RsPatBindingUtil.getMutability(binding);
                Ty type = RsTypesUtil.getType(binding);
                Cmt cmt = new Cmt(field, new Categorization.Local(binding), MutabilityCategory.from(mutability), type);
                delegateConsume(field, cmt);
            }
        }

        if (withExpr != null) {
            Cmt withCmt = mc.processExpr(withExpr);
            Ty withType = withCmt.ty;
            if (withType instanceof TyAdt) {
                TyAdt adt = (TyAdt) withType;
                RsStructItem structItem = adt.getItem() instanceof RsStructItem ? (RsStructItem) adt.getItem() : null;
                if (structItem != null) {
                    List<RsNamedFieldDecl> withFields = RsFieldsOwnerUtil.getNamedFields(structItem);
                    for (RsNamedFieldDecl withField : withFields) {
                        boolean isMentioned = false;
                        for (RsStructLiteralField f : fields) {
                            if (f.getReferenceName() != null && f.getReferenceName().equals(withField.getName())) {
                                isMentioned = true;
                                break;
                            }
                        }
                        if (!isMentioned) {
                            Ty rawWithFieldType = withField.getTypeReference() != null
                                ? ExtensionsUtil.normType(withField.getTypeReference(), mc.lookup)
                                : TyUnknown.INSTANCE;
                            Ty withFieldType = org.rust.lang.core.types.infer.FoldUtil.substituteOrUnknown(rawWithFieldType, adt.getTypeParameterValues());
                            Categorization.Interior.Field interior = new Categorization.Interior.Field(withCmt, withField.getName());
                            Cmt fieldCmt = new Cmt(withExpr, interior, withCmt.mutabilityCategory.inherit(), withFieldType);
                            delegateConsume(withExpr, fieldCmt);
                        }
                    }
                }
            }
            walkExpr(withExpr);
        }
    }

    @Nonnull
    private TrackMatchMode armMoveMode(@Nonnull Cmt discriminantCmt, @Nonnull RsMatchArm arm) {
        TrackMatchMode mode = TrackMatchMode.Unknown;
        for (RsPat pat : RsMatchArmUtil.getPatList(arm)) {
            mode = determinePatMoveMode(discriminantCmt, pat, mode);
        }
        return mode;
    }

    private void walkArm(@Nonnull Cmt discriminantCmt, @Nonnull RsMatchArm arm, @Nonnull MatchMode mode) {
        for (RsPat pat : RsMatchArmUtil.getPatList(arm)) {
            walkPat(discriminantCmt, pat, mode);
        }
        RsMatchArmGuard guard = arm.getMatchArmGuard();
        if (guard != null && guard.getExpr() != null) consumeExpr(guard.getExpr());
        if (arm.getExpr() != null) consumeExpr(arm.getExpr());
    }

    private void walkIrrefutablePat(@Nonnull Cmt discriminantCmt, @Nonnull RsPat pat) {
        TrackMatchMode mode = determinePatMoveMode(discriminantCmt, pat, TrackMatchMode.Unknown);
        walkPat(discriminantCmt, pat, mode.getMatchMode());
    }

    @Nonnull
    private TrackMatchMode determinePatMoveMode(@Nonnull Cmt discriminantCmt, @Nonnull RsPat pat, @Nonnull TrackMatchMode mode) {
        final TrackMatchMode[] newMode = {mode};
        mc.walkPat(discriminantCmt, pat, (subPatCmt, subPat, binding) -> {
            RsBindingModeKind kind = RsPatBindingUtil.getKind(binding);
            if (kind instanceof RsBindingModeKind.BindByReference) {
                newMode[0] = newMode[0].leastUpperBound(MatchMode.BorrowingMatch);
            } else {
                newMode[0] = newMode[0].leastUpperBound(copyOrMove(mc, subPatCmt, MoveReason.PatBindingMove).getMatchMode());
            }
        });
        return newMode[0];
    }

    private void walkPat(@Nonnull Cmt discriminantCmt, @Nonnull RsPat pat, @Nonnull MatchMode matchMode) {
        mc.walkPat(discriminantCmt, pat, (subPatCmt, subPat, binding) -> {
            MutabilityCategory mutabilityCategory = MutabilityCategory.from(RsPatBindingUtil.getMutability(binding));
            Cmt bindingCmt = new Cmt(binding, new Categorization.Local(binding), mutabilityCategory, RsTypesUtil.getType(binding));

            delegate.mutate(subPat, bindingCmt, MutateMode.Init);

            if (RsPatBindingUtil.getKind(binding) instanceof RsBindingModeKind.BindByValue) {
                if (matchMode != MatchMode.NonConsumingMatch || delegate instanceof Liveness.GatherLivenessContext) {
                    delegate.consumePat(subPat, subPatCmt, copyOrMove(mc, subPatCmt, MoveReason.PatBindingMove));
                }
            }
        });
    }
}
