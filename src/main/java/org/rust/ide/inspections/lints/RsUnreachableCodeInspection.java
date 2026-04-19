/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.fixes.SubstituteTextFix;
import org.rust.ide.injected.DoctestUtils;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.RsPsiElementExtUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.dfa.ControlFlowGraph;
import org.rust.openapiext.DocumentExtUtil;

import java.util.*;
import org.rust.ide.injected.RsDoctestLanguageInjector;
import org.rust.stdext.Utils;

public class RsUnreachableCodeInspection extends RsLintInspection {

    @NotNull
    @Override
    public String getDisplayName() {
        return RsBundle.message("inspection.message.unreachable.code");
    }

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.UnreachableCode;
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @SuppressWarnings("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            @Override
            public void visitFunction2(@NotNull RsFunction func) {
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
    @NotNull
    private Collection<TextRange> mergeRanges(@NotNull List<TextRange> sortedRanges) {
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
        @NotNull RsProblemsHolder holder,
        @NotNull RsFunction func,
        @NotNull TextRange range
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
}
