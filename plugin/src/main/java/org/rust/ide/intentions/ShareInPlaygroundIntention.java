/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.language.editor.intention.LowPriorityAction;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.actions.ShareInPlaygroundAction;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.psi.impl.RsFile;
import consulo.localize.LocalizeValue;

public class ShareInPlaygroundIntention extends RsElementBaseIntentionAction<ShareInPlaygroundAction.Context> implements LowPriorityAction {
    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return getFamilyName();
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("action.Rust.ShareInPlayground.text"));
        }

    @Nonnull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Nonnull
    @Override
    public InvokeInside getFunctionLikeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Nullable
    @Override
    public ShareInPlaygroundAction.Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        if (!(element.getContainingFile() instanceof RsFile)) return null;
        RsFile file = (RsFile) element.getContainingFile();
        String selectedText = editor.getSelectionModel().getSelectedText();
        if (selectedText == null) return null;
        return new ShareInPlaygroundAction.Context(file, selectedText, true);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull ShareInPlaygroundAction.Context ctx) {
        ShareInPlaygroundAction.performAction(project, ctx);
    }
}
