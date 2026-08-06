/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.language.editor.refactoring.RefactoringFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.openapiext.NonBlockingUtil;

/**
 * Fix that renames the given element.
 */
public class RenameFix extends RsQuickFixBase<PsiNamedElement> {

    private final String newName;
    
    private final String fixName;

    public RenameFix(@Nonnull PsiNamedElement element, @Nonnull String newName) {
        this(element, newName, RsBundle.message("intention.name.rename.to", newName));
    }

    public RenameFix(@Nonnull PsiNamedElement element, @Nonnull String newName, @Nonnull  String fixName) {
        super(element);
        this.newName = newName;
        this.fixName = fixName;
    }

    @Nonnull
    public String getNewName() {
        return newName;
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(fixName);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.rename.element"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiNamedElement element) {
        NonBlockingUtil.nonBlocking(
            project,
            () -> {
                if (element instanceof RsModDeclItem) {
                    PsiElement resolved = ((RsModDeclItem) element).getReference() != null
                        ? ((RsModDeclItem) element).getReference().resolve()
                        : null;
                    return resolved != null ? resolved : element;
                }
                return element;
            },
            (resolvedElement) -> {
                RefactoringFactory.getInstance(project).createRename(resolvedElement, newName).run();
            }
        );
    }

    @Nonnull
    public IntentionPreviewInfo generatePreview(@Nonnull Project project, @Nonnull ProblemDescriptor previewDescriptor) {
        return IntentionPreviewInfo.EMPTY;
    }

    @Nonnull
    public IntentionPreviewInfo generatePreview(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }
}
