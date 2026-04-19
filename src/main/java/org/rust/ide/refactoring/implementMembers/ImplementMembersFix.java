/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.implementMembers;

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.ide.refactoring.implementMembers.ImplementMembersImpl;

/**
 * Adds unimplemented methods and associated types to an impl block
 */
public class ImplementMembersFix extends LocalQuickFixAndIntentionActionOnPsiElement {

    public ImplementMembersFix(@NotNull RsImplItem implBody) {
        super(implBody);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.implement.members");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @NotNull
    @Override
    public PsiElement getElementToMakeWritable(@NotNull PsiFile currentFile) {
        return currentFile;
    }

    @Override
    public void invoke(
        @NotNull Project project,
        @NotNull PsiFile file,
        @Nullable Editor editor,
        @NotNull PsiElement startElement,
        @NotNull PsiElement endElement
    ) {
        RsImplItem impl = (RsImplItem) startElement;
        ImplementMembersImpl.generateTraitMembers(impl, editor);
    }
}
