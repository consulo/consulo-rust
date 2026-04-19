/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiFileRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.openapiext.DocumentExtUtil;

import com.intellij.openapi.editor.Document;

/**
 * Fix that removes the given range from the document and places a text onto its place.
 */
public class SubstituteTextFix extends RsQuickFixBase<PsiElement> {

    @IntentionName
    private final String fixName;
    @Nullable
    private final String substitution;
    @SafeFieldForPreview
    private final SmartPsiFileRange fileWithRange;

    private SubstituteTextFix(
        @NotNull @IntentionName String fixName,
        @NotNull PsiElement element,
        @NotNull TextRange range,
        @Nullable String substitution
    ) {
        super(element);
        this.fixName = fixName;
        this.substitution = substitution;
        this.fileWithRange = SmartPointerManager.getInstance(element.getProject())
            .createSmartPsiFileRangePointer(element.getContainingFile(), range);
    }

    @NotNull
    @Override
    public String getText() {
        return fixName;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.substitute.one.text.to.another");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
        com.intellij.openapi.util.Segment segment = fileWithRange.getRange();
        if (segment == null) return;
        TextRange range = TextRange.create(segment.getStartOffset(), segment.getEndOffset());
        Document document = DocumentExtUtil.getDocument(element.getContainingFile());
        if (document == null) return;
        if (substitution != null) {
            document.replaceString(range.getStartOffset(), range.getEndOffset(), substitution);
        } else {
            document.deleteString(range.getStartOffset(), range.getEndOffset());
        }
    }

    @NotNull
    public static SubstituteTextFix delete(@NotNull @IntentionName String fixName, @NotNull PsiFile file, @NotNull TextRange range) {
        return new SubstituteTextFix(fixName, file.findElementAt(range.getStartOffset()), range, null);
    }

    @NotNull
    public static SubstituteTextFix insert(@NotNull @IntentionName String fixName, @NotNull PsiFile file, int offset, @NotNull String text) {
        return new SubstituteTextFix(fixName, file.findElementAt(offset), new TextRange(offset, offset), text);
    }

    @NotNull
    public static SubstituteTextFix replace(@NotNull @IntentionName String fixName, @NotNull PsiFile file, @NotNull TextRange range, @NotNull String text) {
        return new SubstituteTextFix(fixName, file.findElementAt(range.getStartOffset()), range, text);
    }
}
