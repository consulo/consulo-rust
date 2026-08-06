/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.FileModifier;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.intentions.util.macros.IntentionInMacroUtil;
import org.rust.lang.core.macros.RsExpandedElementUtil;

/**
 * A base class for implementing quick fixes.
 *
 * @see org.rust.ide.intentions.RsElementBaseIntentionAction
 */
public abstract class RsQuickFixBase<E extends PsiElement> extends LocalQuickFixAndIntentionActionOnPsiElement
    implements LocalQuickFix {

    public RsQuickFixBase(@Nonnull E element) {
        super(element);
    }

    @Nonnull
    public abstract consulo.localize.LocalizeValue getFamilyName();

    @Nonnull
    public abstract consulo.localize.LocalizeValue getText();

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    public boolean availableInBatchMode() {
        return true;
    }

    public abstract void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull E element);

    @Override
    public final void invoke(
        @Nonnull Project project,
        @Nonnull PsiFile file,
        @Nullable Editor editor,
        @Nonnull PsiElement startElement,
        @Nonnull PsiElement endElement
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
        @Nonnull Project project,
        @Nullable Editor originalEditor,
        @Nonnull PsiFile originalFile,
        @Nonnull PsiElement expandedElement
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
    public final boolean isAvailable(@Nonnull Project project, @Nonnull PsiFile file, @Nonnull PsiElement startElement, @Nonnull PsiElement endElement) {
        return super.isAvailable(project, file, startElement, endElement);
    }

    @Override
    public final void invoke(@Nonnull Project project, @Nonnull PsiFile file, @Nonnull PsiElement startElement, @Nonnull PsiElement endElement) {
        super.invoke(project, file, startElement, endElement);
    }

    public final boolean isAvailable(@Nonnull Project project, @Nonnull PsiFile file, @Nullable Editor editor, @Nonnull PsiElement startElement, @Nonnull PsiElement endElement) {
        return super.isAvailable(project, file, startElement, endElement);
    }

    @Nullable
    public PsiElement getElementToMakeWritable(@Nonnull PsiFile currentFile) {
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
    public FileModifier getFileModifierForPreview(@Nonnull PsiFile target) {
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
