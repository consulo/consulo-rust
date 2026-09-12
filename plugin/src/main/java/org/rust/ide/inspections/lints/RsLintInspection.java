/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;
import consulo.language.editor.inspection.SuppressQuickFix;

import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.LocalQuickFixOnPsiElement;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.inspections.RsLocalInspectionTool;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;

public abstract class RsLintInspection extends RsLocalInspectionTool {

    @Nullable
    protected abstract RsLint getLint(@Nonnull PsiElement element);

    protected void registerLintProblem(
        @Nonnull RsProblemsHolder holder,
        @Nonnull PsiElement element,
        @Nonnull  String descriptionTemplate,
        @Nonnull RsLintHighlightingType lintHighlightingType,
        @Nonnull List<LocalQuickFix> fixes
    ) {
        ProblemHighlightType highlightType = getProblemHighlightType(element, lintHighlightingType);
        holder.registerProblem(element, descriptionTemplate, highlightType, fixes.toArray(LocalQuickFix.EMPTY_ARRAY));
    }

    protected void registerLintProblem(
        @Nonnull RsProblemsHolder holder,
        @Nonnull PsiElement element,
        @Nonnull  String descriptionTemplate
    ) {
        registerLintProblem(holder, element, descriptionTemplate, RsLintHighlightingType.DEFAULT, Collections.emptyList());
    }

    protected void registerLintProblem(
        @Nonnull RsProblemsHolder holder,
        @Nonnull PsiElement element,
        @Nonnull  String descriptionTemplate,
        @Nonnull RsLintHighlightingType lintHighlightingType
    ) {
        registerLintProblem(holder, element, descriptionTemplate, lintHighlightingType, Collections.emptyList());
    }

    protected void registerLintProblem(
        @Nonnull RsProblemsHolder holder,
        @Nonnull PsiElement element,
        @Nonnull  String descriptionTemplate,
        @Nonnull TextRange rangeInElement,
        @Nonnull RsLintHighlightingType lintHighlightingType,
        @Nonnull List<LocalQuickFix> fixes
    ) {
        ProblemHighlightType highlightType = getProblemHighlightType(element, lintHighlightingType);
        holder.registerProblem(element, descriptionTemplate, highlightType, rangeInElement, fixes.toArray(LocalQuickFix.EMPTY_ARRAY));
    }

    protected void registerLintProblem(
        @Nonnull RsProblemsHolder holder,
        @Nonnull PsiElement element,
        @Nonnull  String descriptionTemplate,
        @Nonnull TextRange rangeInElement,
        @Nonnull RsLintHighlightingType lintHighlightingType
    ) {
        registerLintProblem(holder, element, descriptionTemplate, rangeInElement, lintHighlightingType, Collections.emptyList());
    }

    @Nonnull
    private ProblemHighlightType getProblemHighlightType(
        @Nonnull PsiElement element,
        @Nonnull RsLintHighlightingType lintHighlightingType
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
    public boolean isSuppressedFor(@Nonnull PsiElement element) {
        if (super.isSuppressedFor(element)) return true;
        RsLint lint = getLint(element);
        return lint != null && lint.levelFor(element) == RsLintLevel.ALLOW;
    }

    // TODO: fix quick fix order in UI
    @Nonnull
    @Override
    public SuppressQuickFix [] getBatchSuppressActions(@Nullable PsiElement element) {
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

    @Override
    @jakarta.annotation.Nonnull
    public consulo.language.editor.rawHighlight.HighlightDisplayLevel getDefaultLevel() {
        return consulo.language.editor.rawHighlight.HighlightDisplayLevel.WARNING;
    }
}
