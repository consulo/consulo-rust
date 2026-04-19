/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.intentions.util.macros.IntentionInMacroUtil;
import org.rust.lang.core.macros.RsExpandedElementUtil;

/**
 * A base class for implementing quick fixes.
 *
 * @see org.rust.ide.intentions.RsElementBaseIntentionAction
 */
public abstract class RsQuickFixBase<E extends PsiElement> extends LocalQuickFixAndIntentionActionOnPsiElement
    implements LocalQuickFix {

    public RsQuickFixBase(@NotNull E element) {
        super(element);
    }

    @NotNull
    public abstract String getFamilyName();

    @NotNull
    public abstract String getText();

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    @Override
    public boolean availableInBatchMode() {
        return true;
    }

    public abstract void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull E element);

    @Override
    public final void invoke(
        @NotNull Project project,
        @NotNull PsiFile file,
        @Nullable Editor editor,
        @NotNull PsiElement startElement,
        @NotNull PsiElement endElement
    ) {
        if (RsExpandedElementUtil.isExpandedFromMacro(startElement)) {
            invokeInsideMacroExpansion(project, editor, file, startElement);
        } else {
            @SuppressWarnings("unchecked")
            E element = (E) startElement;
            invoke(project, editor, element);
        }
    }

    private void invokeInsideMacroExpansion(
        @NotNull Project project,
        @Nullable Editor originalEditor,
        @NotNull PsiFile originalFile,
        @NotNull PsiElement expandedElement
    ) {
        IntentionInMacroUtil.runActionInsideMacroExpansionCopy(
            project,
            originalEditor,
            originalFile,
            expandedElement,
            (editorCopy, expandedElementCopy) -> {
                @SuppressWarnings("unchecked")
                E element = (E) expandedElementCopy;
                invoke(project, editorCopy, element);
                return true;
            }
        );
    }

    @Override
    public final boolean isAvailable(@NotNull Project project, @NotNull PsiFile file, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
        return super.isAvailable(project, file, startElement, endElement);
    }

    @Override
    public final void invoke(@NotNull Project project, @NotNull PsiFile file, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
        super.invoke(project, file, startElement, endElement);
    }

    @Override
    public final boolean isAvailable(@NotNull Project project, @NotNull PsiFile file, @Nullable Editor editor, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
        return super.isAvailable(project, file, editor, startElement, endElement);
    }

    @Override
    public final void applyFix() {
        super.applyFix();
    }

    @Override
    @Nullable
    public PsiElement getElementToMakeWritable(@NotNull PsiFile currentFile) {
        if (!startInWriteAction()) return null;
        PsiElement element = super.getStartElement();
        PsiElement macroCall = RsExpandedElementUtil.findMacroCallExpandedFrom(element);
        PsiFile originalContainingFile;
        if (macroCall != null) {
            originalContainingFile = macroCall.getContainingFile();
        } else {
            originalContainingFile = element.getContainingFile();
        }

        if (originalContainingFile == currentFile.getOriginalFile()) {
            // Intention preview
            return currentFile;
        } else {
            return originalContainingFile;
        }
    }

    @Override
    @Nullable
    public FileModifier getFileModifierForPreview(@NotNull PsiFile target) {
        if (!RsExpandedElementUtil.isExpandedFromMacro(super.getStartElement())) {
            return super.getFileModifierForPreview(target);
        } else {
            // Check field safety in subclass
            FileModifier localQuickFixModifier = super.getFileModifierForPreview(target);
            if (localQuickFixModifier != this) return null;
            return this;
        }
    }

    /**
     * @deprecated In the case of a macro, this method returns a wrong PSI element. Use element instead.
     */
    @Deprecated
    @Override
    @Nullable
    public final PsiElement getStartElement() {
        return super.getStartElement();
    }

    /**
     * @deprecated It is always null.
     */
    @Deprecated
    @Override
    @Nullable
    public final PsiElement getEndElement() {
        return null;
    }
}
