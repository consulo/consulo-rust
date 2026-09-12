/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.implementMembers;

import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.ide.refactoring.implementMembers.ImplementMembersImpl;
import consulo.localize.LocalizeValue;

/**
 * Adds unimplemented methods and associated types to an impl block
 */
public class ImplementMembersFix extends LocalQuickFixAndIntentionActionOnPsiElement {

    public ImplementMembersFix(@Nonnull RsImplItem implBody) {
        super(implBody);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.implement.members"));
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Nonnull
    @Override
    public PsiElement getElementToMakeWritable(@Nonnull PsiFile currentFile) {
        return currentFile;
    }

    @Override
    public void invoke(
        @Nonnull Project project,
        @Nonnull PsiFile file,
        @Nullable Editor editor,
        @Nonnull PsiElement startElement,
        @Nonnull PsiElement endElement
    ) {
        RsImplItem impl = (RsImplItem) startElement;
        ImplementMembersImpl.generateTraitMembers(impl, editor);
    }
}
