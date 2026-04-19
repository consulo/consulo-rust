/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.fixes.RemoveElementFix;
import org.rust.ide.injected.DoctestUtils;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.Collection;
import java.util.Collections;
import org.rust.lang.core.psi.ext.RsSelfParameterUtil;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.RsPatBindingUtil;
import org.rust.ide.injected.RsDoctestLanguageInjector;

public class RsUnusedMutInspection extends RsLintInspection {

    @NotNull
    @Override
    public String getDisplayName() {
        return RsBundle.message("no.mutable.required");
    }

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.UnusedMut;
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitBindingMode(@NotNull RsBindingMode o) {
                PsiElement parent = o.getParent();
                if (!(parent instanceof RsPatBinding)) return;
                RsPatBinding patBinding = (RsPatBinding) parent;
                if (!RsPatBindingUtil.getMutability(patBinding).isMut()) return;
                if (RsDoctestLanguageInjector.isDoctestInjection(patBinding)) return;

                Collection<PsiReference> references = RsSearchableUtil.searchReferences(patBinding, patBinding.getUseScope());
                for (PsiReference ref : references) {
                    if (checkOccurrenceNeedMutable(ref.getElement().getParent())) return;
                }

                PsiElement mut = o.getMut();
                if (mut == null) return;
                registerLintProblem(
                    holder,
                    mut,
                    RsBundle.message("inspection.message.unused.mut"),
                    RsLintHighlightingType.UNUSED_SYMBOL,
                    Collections.singletonList(new RemoveElementFix(mut))
                );
            }
        };
    }

    public boolean checkOccurrenceNeedMutable(@NotNull PsiElement occurrence) {
        PsiElement parent = occurrence.getParent();
        if (parent instanceof RsUnaryExpr) {
            RsUnaryExpr unaryExpr = (RsUnaryExpr) parent;
            return unaryExpr.getMut() != null || unaryExpr.getMul() != null;
        }
        if (parent instanceof RsBinaryExpr) {
            return ((RsBinaryExpr) parent).getLeft() == occurrence;
        }
        if (parent instanceof RsMethodCall) {
            PsiElement resolved = ((RsMethodCall) parent).getReference().resolve();
            if (!(resolved instanceof RsFunction)) return true;
            RsFunction ref = (RsFunction) resolved;
            RsSelfParameter self = ref.getSelfParameter();
            if (self == null) return true;
            return RsSelfParameterUtil.getMutability(self).isMut();
        }
        if (parent instanceof RsTupleExpr) {
            PsiElement grandParent = parent.getParent();
            if (!(grandParent instanceof RsUnaryExpr)) return true;
            return ((RsUnaryExpr) grandParent).getMut() != null;
        }
        if (parent instanceof RsValueArgumentList) return false;
        if (parent instanceof RsExprMacroArgument || parent instanceof RsVecMacroArgument
            || parent instanceof RsAssertMacroArgument || parent instanceof RsConcatMacroArgument
            || parent instanceof RsEnvMacroArgument) {
            return false;
        }
        if (parent instanceof RsFormatMacroArg) {
            RsFormatMacroArgument argList = PsiTreeUtil.getContextOfType(parent, RsFormatMacroArgument.class);
            if (argList == null) return true;
            RsMacroCall macroCall = PsiTreeUtil.getContextOfType(parent, RsMacroCall.class);
            if (macroCall == null) return true;
            String macroName = RsMacroCallUtil.getMacroName(macroCall);
            boolean isWriteMacro = "write".equals(macroName) || "writeln".equals(macroName);
            if (!isWriteMacro) return false;
            java.util.List<RsFormatMacroArg> formatArgs = argList.getFormatMacroArgList();
            return !formatArgs.isEmpty() && formatArgs.get(0) == parent;
        }
        return true;
    }
}
