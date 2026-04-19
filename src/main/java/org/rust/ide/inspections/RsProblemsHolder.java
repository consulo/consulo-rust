/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.codeInspection.*;
import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.macros.RangeMap;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsElement;
import com.intellij.openapi.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class RsProblemsHolder {
    private final ProblemsHolder myHolder;

    public RsProblemsHolder(@NotNull ProblemsHolder holder) {
        this.myHolder = holder;
    }

    @NotNull
    public InspectionManager getManager() {
        return myHolder.getManager();
    }

    @NotNull
    public PsiFile getFile() {
        return myHolder.getFile();
    }

    @NotNull
    public Project getProject() {
        return myHolder.getProject();
    }

    public boolean isOnTheFly() {
        return myHolder.isOnTheFly();
    }

    public void registerProblem(
        @NotNull PsiElement element,
        @NotNull @InspectionMessage String descriptionTemplate,
        @NotNull LocalQuickFix... fixes
    ) {
        if (PsiElementExt.getExistsAfterExpansion(element)) {
            if (element.getContainingFile() == getFile()) {
                myHolder.registerProblem(element, descriptionTemplate, fixes);
            } else {
                // The element is expanded from a macro
                Pair<PsiElement, TextRange> result = findCorrespondingElementAndRangeExpandedFrom(element, new TextRange(0, element.getTextLength()));
                if (result == null) return;
                myHolder.registerProblem(
                    result.getFirst(),
                    result.getSecond(),
                    descriptionTemplate,
                    filterSupportingMacros(fixes)
                );
            }
        }
    }

    public void registerProblem(
        @NotNull PsiElement element,
        @NotNull @InspectionMessage String descriptionTemplate,
        @NotNull ProblemHighlightType highlightType,
        @NotNull LocalQuickFix... fixes
    ) {
        if (PsiElementExt.getExistsAfterExpansion(element) && isProblemWithTypeAllowed(highlightType)) {
            if (element.getContainingFile() == getFile()) {
                myHolder.registerProblem(element, descriptionTemplate, highlightType, fixes);
            } else {
                // The element is expanded from a macro
                Pair<PsiElement, TextRange> result = findCorrespondingElementAndRangeExpandedFrom(element, new TextRange(0, element.getTextLength()));
                if (result == null) return;
                myHolder.registerProblem(
                    result.getFirst(),
                    descriptionTemplate,
                    highlightType,
                    result.getSecond(),
                    filterSupportingMacros(fixes)
                );
            }
        }
    }

    public void registerProblem(
        @NotNull PsiElement startElement,
        @NotNull PsiElement endElement,
        @NotNull @InspectionMessage String descriptionTemplate,
        @NotNull ProblemHighlightType highlightType,
        @NotNull LocalQuickFix... fixes
    ) {
        if (startElement == endElement) {
            registerProblem(startElement, descriptionTemplate, highlightType, fixes);
            return;
        }
        if (PsiElementExt.getExistsAfterExpansion(startElement) && isProblemWithTypeAllowed(highlightType)) {
            ProblemDescriptor descriptor;
            if (startElement.getContainingFile() == getFile()) {
                descriptor = myHolder.getManager().createProblemDescriptor(
                    startElement,
                    endElement,
                    descriptionTemplate,
                    highlightType,
                    myHolder.isOnTheFly(),
                    fixes
                );
            } else {
                // The element is expanded from a macro
                PsiElement sourceStartElement = findCorrespondingElementExpandedFrom(startElement);
                if (sourceStartElement == null) return;
                PsiElement sourceEndElement = findCorrespondingElementExpandedFrom(endElement);
                if (sourceEndElement == null) return;
                descriptor = myHolder.getManager().createProblemDescriptor(
                    sourceStartElement,
                    sourceEndElement,
                    descriptionTemplate,
                    highlightType,
                    myHolder.isOnTheFly(),
                    filterSupportingMacros(fixes)
                );
            }
            myHolder.registerProblem(descriptor);
        }
    }

    public void registerProblem(
        @NotNull PsiElement element,
        @NotNull TextRange rangeInElement,
        @NotNull @InspectionMessage String message,
        @NotNull LocalQuickFix... fixes
    ) {
        registerProblem(element, rangeInElement, message, false, fixes);
    }

    public void registerProblem(
        @NotNull PsiElement element,
        @NotNull TextRange rangeInElement,
        @NotNull @InspectionMessage String message,
        boolean alwaysShowInMacros,
        @NotNull LocalQuickFix... fixes
    ) {
        if (PsiElementExt.getExistsAfterExpansion(element)) {
            if (element.getContainingFile() == getFile()) {
                myHolder.registerProblem(element, rangeInElement, message, fixes);
            } else {
                // The element is expanded from a macro
                Pair<PsiElement, TextRange> result = findCorrespondingElementAndRangeExpandedFrom(element, rangeInElement);
                if (result == null) {
                    if (alwaysShowInMacros) {
                        com.intellij.psi.PsiElement macroCall = RsExpandedElementUtil.findMacroCallExpandedFromNonRecursive(element);
                        if (macroCall instanceof org.rust.lang.core.psi.RsMacroCall) {
                            PsiElement macroPath = ((org.rust.lang.core.psi.RsMacroCall) macroCall).getPath();
                            if (macroPath != null) {
                                registerProblem(
                                    macroPath,
                                    new TextRange(0, macroPath.getTextLength()),
                                    message,
                                    true,
                                    fixes
                                );
                            }
                        }
                    }
                    return;
                }
                myHolder.registerProblem(result.getFirst(), result.getSecond(), message, filterSupportingMacros(fixes));
            }
        }
    }

    public void registerProblem(
        @NotNull PsiElement element,
        @NotNull @InspectionMessage String message,
        @NotNull ProblemHighlightType highlightType,
        @NotNull TextRange rangeInElement,
        @NotNull LocalQuickFix... fixes
    ) {
        if (PsiElementExt.getExistsAfterExpansion(element) && isProblemWithTypeAllowed(highlightType)) {
            if (element.getContainingFile() == getFile()) {
                myHolder.registerProblem(element, message, highlightType, rangeInElement, fixes);
            } else {
                // The element is expanded from a macro
                Pair<PsiElement, TextRange> result = findCorrespondingElementAndRangeExpandedFrom(element, rangeInElement);
                if (result == null) return;
                myHolder.registerProblem(
                    result.getFirst(),
                    message,
                    highlightType,
                    result.getSecond(),
                    filterSupportingMacros(fixes)
                );
            }
        }
    }

    private boolean isProblemWithTypeAllowed(@NotNull ProblemHighlightType highlightType) {
        return highlightType != ProblemHighlightType.INFORMATION || myHolder.isOnTheFly();
    }

    @Nullable
    private PsiElement findCorrespondingElementExpandedFrom(@NotNull PsiElement element) {
        PsiElement leaf = RsExpandedElementUtil.findElementExpandedFrom(element);
        if (leaf == null) return null;
        int textLength = element.getTextLength();
        com.intellij.psi.tree.IElementType elementType = element.getNode().getElementType();
        PsiElement current = leaf;
        while (current != null) {
            if (current.getTextLength() == textLength && current.getNode().getElementType() == elementType) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    @Nullable
    private Pair<PsiElement, TextRange> findCorrespondingElementAndRangeExpandedFrom(
        @NotNull PsiElement element,
        @NotNull TextRange rangeInElement
    ) {
        // TODO simplify: map the leaf and the text range at once
        com.intellij.psi.PsiElement macroCall = RsExpandedElementUtil.findMacroCallExpandedFromNonRecursive(element);
        if (macroCall == null) return null;
        PsiElement leaf = RsExpandedElementUtil.findElementExpandedFrom(element);
        if (leaf == null) return null;
        List<TextRange> sourceRanges = RsExpandedElementUtil.mapRangeFromExpansionToCallBody(
            (org.rust.lang.core.psi.ext.RsPossibleMacroCall) macroCall,
            rangeInElement.shiftRight(element.getTextOffset())
        );
        if (sourceRanges.size() != 1) return null;
        TextRange sourceRange = sourceRanges.get(0);
        PsiElement sourceElement = null;
        PsiElement current = leaf;
        while (current != null) {
            if (current.getTextRange().contains(sourceRange)) {
                sourceElement = current;
                break;
            }
            current = current.getParent();
        }
        if (sourceElement == null) return null;
        return new Pair<>(sourceElement, sourceRange.shiftLeft(sourceElement.getTextOffset()));
    }

    @NotNull
    private LocalQuickFix[] filterSupportingMacros(@NotNull LocalQuickFix[] fixes) {
        if (!isOnTheFly()) {
            // Quick fixes in macros does not allowed in batch mode for now
            return LocalQuickFix.EMPTY_ARRAY;
        }
        List<LocalQuickFix> filtered = new ArrayList<>();
        for (LocalQuickFix fix : fixes) {
            if (fix instanceof RsQuickFixBase) {
                filtered.add(fix);
            }
        }
        return filtered.toArray(LocalQuickFix.EMPTY_ARRAY);
    }
}
