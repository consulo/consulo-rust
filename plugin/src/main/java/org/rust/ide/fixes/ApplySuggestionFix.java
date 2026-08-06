/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.toolchain.impl.RustcMessage;
import org.rust.lang.core.psi.ext.RsElement;

public class ApplySuggestionFix extends LocalQuickFixAndIntentionActionOnPsiElement {
    private final String myMessage;
    private final String myReplacement;
    private final RustcMessage.Applicability myApplicability;
    private final TextRange myTextRange;

    public ApplySuggestionFix(
        @Nonnull String message,
        @Nonnull String replacement,
        @Nonnull RustcMessage.Applicability applicability,
        @Nonnull PsiElement startElement,
        @Nonnull PsiElement endElement,
        @Nonnull TextRange textRange
    ) {
        super(startElement, endElement);
        this.myMessage = message;
        this.myReplacement = replacement;
        this.myApplicability = applicability;
        this.myTextRange = textRange;
    }

    @Nonnull
    public RustcMessage.Applicability getApplicability() {
        return myApplicability;
    }

    @Nonnull
    public TextRange getTextRange() {
        return myTextRange;
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.apply.suggested.replacement.made.by.external.linter"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.external.linter", myMessage));
    }

    @Override
    public void invoke(
        @Nonnull Project project,
        @Nonnull PsiFile file,
        @Nullable Editor editor,
        @Nonnull PsiElement startElement,
        @Nonnull PsiElement endElement
    ) {
        Document document = editor != null ? editor.getDocument() : file.getViewProvider().getDocument();
        if (document == null) return;
        document.replaceString(startElement.getTextOffset(), RsElementUtil.getEndOffset(endElement), myReplacement);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        ApplySuggestionFix that = (ApplySuggestionFix) other;

        if (!myMessage.equals(that.myMessage)) return false;
        if (!myReplacement.equals(that.myReplacement)) return false;
        if (myStartElement != null ? !myStartElement.equals(that.myStartElement) : that.myStartElement != null) return false;
        return myEndElement != null ? myEndElement.equals(that.myEndElement) : that.myEndElement == null;
    }

    @Override
    public int hashCode() {
        int result = myMessage.hashCode();
        result = 31 * result + myReplacement.hashCode();
        result = 31 * result + (myStartElement != null ? myStartElement.hashCode() : 0);
        result = 31 * result + (myEndElement != null ? myEndElement.hashCode() : 0);
        return result;
    }
}
