/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.experiments.RsExperiments;
import org.rust.ide.fixes.AddMutableFix;
import org.rust.ide.fixes.DeriveCopyFix;
import org.rust.ide.fixes.InitializeWithDefaultValueFix;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.dfa.borrowck.BorrowChecker.BorrowCheckResult;
import org.rust.lang.core.dfa.borrowck.BorrowChecker.MoveError;
import org.rust.lang.core.dfa.borrowck.BorrowChecker.UseOfMovedValueError;
import org.rust.lang.core.dfa.borrowck.BorrowChecker.UseOfUninitializedVariable;
import org.rust.lang.core.mir.borrowck.MirBorrowCheckResult;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyReference;
import org.rust.lang.utils.RsDiagnostic;
import org.rust.openapiext.OpenApiUtil;
import org.rust.lang.core.psi.ext.RsSelfParameterUtil;
import org.rust.lang.core.psi.ext.RsMethodCallUtil;
import org.rust.lang.core.psi.ext.RsUnaryExprUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsElementExtUtil;
import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.rust.lang.core.types.ExtensionsUtil;

public class RsBorrowCheckerInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMethodCall(@NotNull RsMethodCall o) {
                RsFunction fn = o.getReference().resolve() instanceof RsFunction ? (RsFunction) o.getReference().resolve() : null;
                if (fn == null) return;
                RsExpr receiver = RsMethodCallUtil.getReceiver(o);
                if (checkMethodRequiresMutable(receiver, fn)) {
                    registerProblem(holder, receiver, receiver);
                }
            }

            @Override
            public void visitUnaryExpr(@NotNull RsUnaryExpr unaryExpr) {
                RsExpr expr = unaryExpr.getExpr();
                if (expr == null || !ExtensionsUtil.isImmutable(expr)) return;

                if (RsUnaryExprUtil.getOperatorType(unaryExpr) == UnaryOperator.REF_MUT) {
                    registerProblem(holder, expr, expr);
                }
            }

            @Override
            public void visitFunction2(@NotNull RsFunction func) {
                MirAnalysisStatus usedMir = visitFunctionUsingMir(func);

                BorrowCheckResult borrowCheckResult = ExtensionsUtil.getBorrowCheckResult(func);
                if (borrowCheckResult == null) return;

                if (!RsElementExtUtil.descendantsWithMacrosOfType(func, RsAsmMacroArgument.class).isEmpty()) return;

                if (!usedMir.analyzedMoves) {
                    for (UseOfMovedValueError use : borrowCheckResult.usesOfMovedValue) {
                        registerUseOfMovedValueProblem(holder, use.use);
                    }
                }

                if (!usedMir.analyzedUninit) {
                    for (UseOfUninitializedVariable use : borrowCheckResult.usesOfUninitializedVariable) {
                        registerUseOfUninitializedVariableProblem(holder, use.use);
                    }
                }

                for (MoveError error : borrowCheckResult.moveErrors) {
                    RsExpr move = PsiElementUtil.ancestorOrSelf(error.from.element, RsExpr.class);
                    if (move != null) registerMoveProblem(holder, move);
                }
            }

            private MirAnalysisStatus visitFunctionUsingMir(@NotNull RsFunction function) {
                boolean useMirMoveAnalysis = OpenApiUtil.isFeatureEnabled(RsExperiments.MIR_MOVE_ANALYSIS);
                boolean useMirBorrowChecker = OpenApiUtil.isFeatureEnabled(RsExperiments.MIR_BORROW_CHECK);
                if (!useMirBorrowChecker && !useMirMoveAnalysis) return MirAnalysisStatus.NONE;
                MirBorrowCheckResult result = ExtensionsUtil.getMirBorrowCheckResult(function);
                if (result == null) return MirAnalysisStatus.NONE;
                if (useMirMoveAnalysis) {
                    for (RsElement element : result.getUsesOfMovedValue()) {
                        if (!element.isPhysical()) continue;
                        DeriveCopyFix fix = DeriveCopyFix.createIfCompatible(element);
                        RsDiagnostic.addToHolder(new RsDiagnostic.UseOfMovedValueError(element, fix), holder);
                    }
                }
                if (useMirBorrowChecker) {
                    for (RsElement element : result.getUsesOfUninitializedVariable()) {
                        if (!element.isPhysical()) continue;
                        InitializeWithDefaultValueFix fix = InitializeWithDefaultValueFix.createIfCompatible(element);
                        RsDiagnostic.addToHolder(new RsDiagnostic.UseOfUninitializedVariableError(element, fix), holder);
                    }
                    for (RsElement element : result.getMoveOutWhileBorrowedValues()) {
                        if (!element.isPhysical()) continue;
                        RsDiagnostic.addToHolder(new RsDiagnostic.MoveOutWhileBorrowedError(element), holder);
                    }
                }
                return new MirAnalysisStatus(useMirMoveAnalysis, useMirBorrowChecker);
            }
        };
    }

    private static class MirAnalysisStatus {
        final boolean analyzedMoves;
        final boolean analyzedUninit;

        static final MirAnalysisStatus NONE = new MirAnalysisStatus(false, false);

        MirAnalysisStatus(boolean analyzedMoves, boolean analyzedUninit) {
            this.analyzedMoves = analyzedMoves;
            this.analyzedUninit = analyzedUninit;
        }
    }

    private void registerProblem(@NotNull RsProblemsHolder holder, @NotNull RsExpr expr, @NotNull RsExpr nameExpr) {
        if (expr.isPhysical() && nameExpr.isPhysical()) {
            AddMutableFix fix = AddMutableFix.createIfCompatible(nameExpr);
            holder.registerProblem(expr, RsBundle.message("inspection.message.cannot.borrow.immutable.local.variable.as.mutable", nameExpr.getText()),
                fix != null ? new LocalQuickFix[]{fix} : LocalQuickFix.EMPTY_ARRAY);
        }
    }

    private void registerUseOfMovedValueProblem(@NotNull RsProblemsHolder holder, @NotNull RsElement use) {
        if (use.isPhysical()) {
            DeriveCopyFix fix = DeriveCopyFix.createIfCompatible(use);
            holder.registerProblem(use, RsBundle.message("inspection.message.use.moved.value"),
                fix != null ? new LocalQuickFix[]{fix} : LocalQuickFix.EMPTY_ARRAY);
        }
    }

    private void registerMoveProblem(@NotNull RsProblemsHolder holder, @NotNull RsElement element) {
        if (element.isPhysical()) {
            holder.registerProblem(element, RsBundle.message("inspection.message.cannot.move"));
        }
    }

    private void registerUseOfUninitializedVariableProblem(@NotNull RsProblemsHolder holder, @NotNull RsElement use) {
        if (use.isPhysical()) {
            InitializeWithDefaultValueFix fix = InitializeWithDefaultValueFix.createIfCompatible(use);
            holder.registerProblem(use, RsBundle.message("inspection.message.use.possibly.uninitialized.variable"),
                fix != null ? new LocalQuickFix[]{fix} : LocalQuickFix.EMPTY_ARRAY);
        }
    }

    private boolean checkMethodRequiresMutable(@NotNull RsExpr receiver, @NotNull RsFunction fn) {
        RsSelfParameter selfParameter = fn.getSelfParameter();
        if (selfParameter == null) return false;
        if (ExtensionsUtil.isImmutable(receiver) && RsSelfParameterUtil.getMutability(selfParameter).isMut() && RsSelfParameterUtil.isRef(selfParameter)) {
            Ty type = RsTypesUtil.getType(receiver);
            return !(type instanceof TyReference) || !((TyReference) type).getMutability().isMut();
        }
        return false;
    }
}
