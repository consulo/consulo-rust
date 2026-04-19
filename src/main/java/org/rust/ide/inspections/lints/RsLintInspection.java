/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.codeInspection.*;
import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.inspections.RsLocalInspectionTool;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class RsLintInspection extends RsLocalInspectionTool {

    @Nullable
    protected abstract RsLint getLint(@NotNull PsiElement element);

    protected void registerLintProblem(
        @NotNull RsProblemsHolder holder,
        @NotNull PsiElement element,
        @NotNull @InspectionMessage String descriptionTemplate,
        @NotNull RsLintHighlightingType lintHighlightingType,
        @NotNull List<LocalQuickFix> fixes
    ) {
        ProblemHighlightType highlightType = getProblemHighlightType(element, lintHighlightingType);
        holder.registerProblem(element, descriptionTemplate, highlightType, fixes.toArray(LocalQuickFix.EMPTY_ARRAY));
    }

    protected void registerLintProblem(
        @NotNull RsProblemsHolder holder,
        @NotNull PsiElement element,
        @NotNull @InspectionMessage String descriptionTemplate
    ) {
        registerLintProblem(holder, element, descriptionTemplate, RsLintHighlightingType.DEFAULT, Collections.emptyList());
    }

    protected void registerLintProblem(
        @NotNull RsProblemsHolder holder,
        @NotNull PsiElement element,
        @NotNull @InspectionMessage String descriptionTemplate,
        @NotNull RsLintHighlightingType lintHighlightingType
    ) {
        registerLintProblem(holder, element, descriptionTemplate, lintHighlightingType, Collections.emptyList());
    }

    protected void registerLintProblem(
        @NotNull RsProblemsHolder holder,
        @NotNull PsiElement element,
        @NotNull @InspectionMessage String descriptionTemplate,
        @NotNull TextRange rangeInElement,
        @NotNull RsLintHighlightingType lintHighlightingType,
        @NotNull List<LocalQuickFix> fixes
    ) {
        ProblemHighlightType highlightType = getProblemHighlightType(element, lintHighlightingType);
        holder.registerProblem(element, descriptionTemplate, highlightType, rangeInElement, fixes.toArray(LocalQuickFix.EMPTY_ARRAY));
    }

    protected void registerLintProblem(
        @NotNull RsProblemsHolder holder,
        @NotNull PsiElement element,
        @NotNull @InspectionMessage String descriptionTemplate,
        @NotNull TextRange rangeInElement,
        @NotNull RsLintHighlightingType lintHighlightingType
    ) {
        registerLintProblem(holder, element, descriptionTemplate, rangeInElement, lintHighlightingType, Collections.emptyList());
    }

    @NotNull
    private ProblemHighlightType getProblemHighlightType(
        @NotNull PsiElement element,
        @NotNull RsLintHighlightingType lintHighlightingType
    ) {
        RsLint lint = getLint(element);
        if (lint == null) return ProblemHighlightType.WARNING;
        switch (lint.levelFor(element)) {
            case ALLOW:
                return lintHighlightingType.getAllow();
            case WARN:
                return lintHighlightingType.getWarn();
            case DENY:
                return lintHighlightingType.getDeny();
            case FORBID:
                return lintHighlightingType.getForbid();
            default:
                return ProblemHighlightType.WARNING;
        }
    }

    @Override
    public boolean isSuppressedFor(@NotNull PsiElement element) {
        if (super.isSuppressedFor(element)) return true;
        RsLint lint = getLint(element);
        return lint != null && lint.levelFor(element) == RsLintLevel.ALLOW;
    }

    // TODO: fix quick fix order in UI
    @NotNull
    @Override
    public SuppressQuickFix @NotNull [] getBatchSuppressActions(@Nullable PsiElement element) {
        SuppressQuickFix[] fixes = super.getBatchSuppressActions(element);
        if (element == null) return fixes;
        RsLint lint = getLint(element);
        if (lint == null) return fixes;
        RsSuppressQuickFix[] suppressFixes = RsSuppressQuickFix.createSuppressFixes(element, lint);
        SuppressQuickFix[] result = new SuppressQuickFix[fixes.length + suppressFixes.length];
        System.arraycopy(fixes, 0, result, 0, fixes.length);
        System.arraycopy(suppressFixes, 0, result, fixes.length, suppressFixes.length);
        return result;
    }
}
