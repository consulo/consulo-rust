/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.toolchain.impl.RustcMessage;
import org.rust.lang.core.psi.ext.RsElement;

public class ApplySuggestionFix extends LocalQuickFixAndIntentionActionOnPsiElement {
    private final String myMessage;
    private final String myReplacement;
    private final RustcMessage.Applicability myApplicability;
    private final TextRange myTextRange;

    public ApplySuggestionFix(
        @NotNull String message,
        @NotNull String replacement,
        @NotNull RustcMessage.Applicability applicability,
        @NotNull PsiElement startElement,
        @NotNull PsiElement endElement,
        @NotNull TextRange textRange
    ) {
        super(startElement, endElement);
        this.myMessage = message;
        this.myReplacement = replacement;
        this.myApplicability = applicability;
        this.myTextRange = textRange;
    }

    @NotNull
    public RustcMessage.Applicability getApplicability() {
        return myApplicability;
    }

    @NotNull
    public TextRange getTextRange() {
        return myTextRange;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.apply.suggested.replacement.made.by.external.linter");
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.external.linter", myMessage);
    }

    @Override
    public void invoke(
        @NotNull Project project,
        @NotNull PsiFile file,
        @Nullable Editor editor,
        @NotNull PsiElement startElement,
        @NotNull PsiElement endElement
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
