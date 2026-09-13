/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;


import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.fixes.SubstituteTextFix;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.impl.RsPsiElementExtUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.dfa.ControlFlowGraph;
import org.rust.openapiext.DocumentExtUtil;

import java.util.*;
import org.rust.lang.core.injected.RsDoctestLanguageInjector;
import org.rust.stdext.Utils;
import consulo.localize.LocalizeValue;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class RsUnreachableCodeInspection extends RsLintInspection {

    @Nonnull
    @Override
    protected RsLint getLint(@Nonnull PsiElement element) {
        return RsLint.UnreachableCode;
    }

    @Nonnull
    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @SuppressWarnings("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            @Override
            public void visitFunction2(@Nonnull RsFunction func) {
                if (RsDoctestLanguageInjector.isDoctestInjection(func)) return;
                ControlFlowGraph controlFlowGraph = RsTypesUtil.getControlFlowGraph(func);
                if (controlFlowGraph == null) return;

                Set<PsiElement> elementsToReport = new java.util.HashSet<>(controlFlowGraph.unreachableElements);
                if (elementsToReport.isEmpty()) return;

                // Collect text ranges of unreachable elements and merge them
                List<TextRange> sortedRanges = new ArrayList<>();
                for (PsiElement el : elementsToReport) {
                    if (el.isPhysical()) {
                        sortedRanges.add(RsPsiElementExtUtil.getRangeWithPrevSpace(el));
                    }
                }
                if (sortedRanges.isEmpty()) return;
                sortedRanges.sort(Segment.BY_START_OFFSET_THEN_END_OFFSET);

                Collection<TextRange> mergedRanges = mergeRanges(sortedRanges);
                for (TextRange range : mergedRanges) {
                    registerUnreachableProblem(holder, func, range);
                }
            }
        };
    }

    /** Merges intersecting (including adjacent) text ranges into one */
    @Nonnull
    private Collection<TextRange> mergeRanges(@Nonnull List<TextRange> sortedRanges) {
        ArrayDeque<TextRange> mergedRanges = new ArrayDeque<>();
        mergedRanges.push(sortedRanges.get(0));
        for (int i = 1; i < sortedRanges.size(); i++) {
            TextRange range = sortedRanges.get(i);
            TextRange leftNeighbour = mergedRanges.peek();
            if (leftNeighbour.intersects(range)) {
                mergedRanges.pop();
                mergedRanges.push(leftNeighbour.union(range));
            } else {
                mergedRanges.push(range);
            }
        }
        return mergedRanges;
    }

    private void registerUnreachableProblem(
        @Nonnull RsProblemsHolder holder,
        @Nonnull RsFunction func,
        @Nonnull TextRange range
    ) {
        CharSequence chars = func.getContainingFile().getViewProvider().getDocument() != null
            ? func.getContainingFile().getViewProvider().getDocument().getImmutableCharSequence()
            : null;
        if (chars == null) return;
        TextRange strippedRange = Utils.stripWhitespace(range, chars);
        TextRange strippedRangeInFunction = strippedRange.shiftLeft(func.getTextOffset());

        registerLintProblem(
            holder,
            func,
            RsBundle.message("inspection.message.unreachable.code"),
            strippedRangeInFunction,
            RsLintHighlightingType.UNUSED_SYMBOL,
            Collections.singletonList(SubstituteTextFix.delete(
                RsBundle.message("intention.name.remove.unreachable.code"),
                func.getContainingFile(),
                range
            ))
        );
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.unreachable.code.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("lints"));
    }

    @Nonnull
    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{LocalizeValue.of(RsBundle.message("rust"))};
    }
}
