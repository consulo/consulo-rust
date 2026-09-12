/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.FileModifier.SafeFieldForPreview;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiFileRange;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.openapiext.DocumentExtUtil;

import consulo.document.Document;
import consulo.document.util.Segment;
import consulo.localize.LocalizeValue;

/**
 * Fix that removes the given range from the document and places a text onto its place.
 */
public class SubstituteTextFix extends RsQuickFixBase<PsiElement> {

    
    private final String fixName;
    @Nullable
    private final String substitution;
    @SafeFieldForPreview
    private final SmartPsiFileRange fileWithRange;

    private SubstituteTextFix(
        @Nonnull  String fixName,
        @Nonnull PsiElement element,
        @Nonnull TextRange range,
        @Nullable String substitution
    ) {
        super(element);
        this.fixName = fixName;
        this.substitution = substitution;
        this.fileWithRange = SmartPointerManager.getInstance(element.getProject())
            .createSmartPsiFileRangePointer(element.getContainingFile(), range);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(fixName);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.substitute.one.text.to.another"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement element) {
        consulo.document.util.Segment segment = fileWithRange.getRange();
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

    @Nonnull
    public static SubstituteTextFix delete(@Nonnull  String fixName, @Nonnull PsiFile file, @Nonnull TextRange range) {
        return new SubstituteTextFix(fixName, file.findElementAt(range.getStartOffset()), range, null);
    }

    @Nonnull
    public static SubstituteTextFix insert(@Nonnull  String fixName, @Nonnull PsiFile file, int offset, @Nonnull String text) {
        return new SubstituteTextFix(fixName, file.findElementAt(offset), new TextRange(offset, offset), text);
    }

    @Nonnull
    public static SubstituteTextFix replace(@Nonnull  String fixName, @Nonnull PsiFile file, @Nonnull TextRange range, @Nonnull String text) {
        return new SubstituteTextFix(fixName, file.findElementAt(range.getStartOffset()), range, text);
    }
}
