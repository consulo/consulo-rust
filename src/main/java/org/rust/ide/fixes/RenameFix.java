/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.RefactoringFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.openapiext.NonBlockingUtil;

/**
 * Fix that renames the given element.
 */
public class RenameFix extends RsQuickFixBase<PsiNamedElement> {

    private final String newName;
    @IntentionName
    private final String fixName;

    public RenameFix(@NotNull PsiNamedElement element, @NotNull String newName) {
        this(element, newName, RsBundle.message("intention.name.rename.to", newName));
    }

    public RenameFix(@NotNull PsiNamedElement element, @NotNull String newName, @NotNull @IntentionName String fixName) {
        super(element);
        this.newName = newName;
        this.fixName = fixName;
    }

    @NotNull
    public String getNewName() {
        return newName;
    }

    @NotNull
    @Override
    public String getText() {
        return fixName;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.rename.element");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiNamedElement element) {
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

    @NotNull
    @Override
    public IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull ProblemDescriptor previewDescriptor) {
        return IntentionPreviewInfo.EMPTY;
    }

    @NotNull
    @Override
    public IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }
}
