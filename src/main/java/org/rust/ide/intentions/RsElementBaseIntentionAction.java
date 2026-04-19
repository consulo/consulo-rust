/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.experiments.RsExperiments;
import org.rust.ide.intentions.util.macros.IntentionInMacroUtil;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.RsMacroArgument;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.openapiext.OpenApiUtil;

import java.util.List;

/**
 * A base class for implementing intentions: actions available via "light bulb" / Alt+Enter.
 *
 * The cool thing about intentions is their UX: there is a huge number of intentions,
 * and they all can be invoked with a single Alt + Enter shortcut. This is possible
 * because at the position of the cursor only small number of intentions is applicable.
 *
 * So, intentions consists of two functions: {@link #findApplicableContext} functions determines
 * if the intention can be applied at the given position, it is used to populate "light bulb" list.
 * {@link #invoke(Project, Editor, Object)} is called when the user selects the intention from the list and must apply the changes.
 *
 * The context collected by {@link #findApplicableContext} is gathered into Ctx object and is passed to
 * {@link #invoke(Project, Editor, Object)}. In general, invoke should be infallible: if you need to check if some element is not
 * null, do this in {@link #findApplicableContext} and pass the element via the context.
 *
 * {@link #findApplicableContext} is executed under a read action, and {@link #invoke(Project, Editor, Object)} under a write action.
 */
public abstract class RsElementBaseIntentionAction<Ctx> extends BaseElementAtCaretIntentionAction {

    /**
     * Return null if the intention is not applicable, otherwise collect and return
     * all the necessary info to actually apply the intention.
     */
    @Nullable
    public abstract Ctx findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element);

    public abstract void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Ctx ctx);

    @Override
    public final void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        if (startInWriteAction() && !RsElementUtil.isIntentionPreviewElement(element)) {
            OpenApiUtil.checkWriteAccessAllowed();
        }

        PsiElement possiblyExpandedElement = findTargetElement(element);
        if (possiblyExpandedElement == null) return;
        if (possiblyExpandedElement != element) {
            invokeInsideMacroExpansion(project, editor, element.getContainingFile(), possiblyExpandedElement);
        } else {
            invokeInner(project, editor, element);
        }
    }

    @Nullable
    private PsiElement findTargetElement(@NotNull PsiElement element) {
        List<PsiElement> expandedElements = RsExpandedElementUtil.findExpansionElements(element);
        if (expandedElements == null) {
            // The element is NOT inside a macro call - use the original element
            return element;
        } else {
            // The element is inside a macro call
            boolean isFunctionLike = PsiElementExt.ancestorOrSelf(element, RsMacroArgument.class) != null;
            InvokeInside strategy = isFunctionLike ? getFunctionLikeMacroHandlingStrategy() : getAttributeMacroHandlingStrategy();
            if (strategy == InvokeInside.MACRO_CALL) return element;
            if (isFunctionLike && !OpenApiUtil.isFeatureEnabled(RsExperiments.INTENTIONS_IN_FN_LIKE_MACROS)) return element;
            // Use the expanded element if it is single; The intention is unavailable if
            // there are multiple or none expanded elements (e.g. if the macro expansion is failed)
            if (expandedElements.size() == 1) {
                return expandedElements.get(0);
            }
            return null;
        }
    }

    private void invokeInner(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        Ctx ctx = findApplicableContext(project, editor, element);
        if (ctx == null) return;
        invoke(project, editor, ctx);
    }

    private void invokeInsideMacroExpansion(
        @NotNull Project project,
        @NotNull Editor originalEditor,
        @NotNull PsiFile originalFile,
        @NotNull PsiElement expandedElement
    ) {
        IntentionInMacroUtil.runActionInsideMacroExpansionCopy(
            project,
            originalEditor,
            originalFile,
            expandedElement,
            (editorCopy, expandedElementCopy) -> {
                invokeInner(project, editorCopy, expandedElementCopy);
                return true;
            }
        );
    }

    @Override
    public final boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        OpenApiUtil.checkReadAccessAllowed();
        PsiElement possiblyExpandedElement = findTargetElement(element);
        if (possiblyExpandedElement == null) return false;
        if (possiblyExpandedElement != element) {
            return IntentionInMacroUtil.doActionAvailabilityCheckInsideMacroExpansion(
                editor,
                element.getContainingFile(),
                possiblyExpandedElement,
                PsiElementExt.getStartOffset(possiblyExpandedElement) + (editor.getCaretModel().getOffset() - PsiElementExt.getStartOffset(element)),
                fakeEditor -> findApplicableContext(project, fakeEditor, possiblyExpandedElement) != null
            );
        } else {
            return findApplicableContext(project, editor, possiblyExpandedElement) != null;
        }
    }

    /**
     * Controls how the intention action behaves inside an attribute macro call.
     *
     * Choose {@link InvokeInside#MACRO_CALL} if the intention is syntax-based and does not involve
     * name resolution or type inference, or if the intention handles macros by itself
     */
    @NotNull
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_EXPANSION;
    }

    /**
     * Controls how the intention action behaves inside a function-like macro call.
     *
     * Choose {@link InvokeInside#MACRO_CALL} if the intention is text-based and does not need a rich PSI
     * structure, or if the intention handles macros by itself
     */
    @NotNull
    public InvokeInside getFunctionLikeMacroHandlingStrategy() {
        return InvokeInside.MACRO_EXPANSION;
    }
}
