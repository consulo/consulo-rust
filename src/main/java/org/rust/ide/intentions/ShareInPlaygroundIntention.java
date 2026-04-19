/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.codeInsight.intention.LowPriorityAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.actions.ShareInPlaygroundAction;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.psi.RsFile;

public class ShareInPlaygroundIntention extends RsElementBaseIntentionAction<ShareInPlaygroundAction.Context> implements LowPriorityAction {
    @NotNull
    @Override
    public String getText() {
        return getFamilyName();
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("action.Rust.ShareInPlayground.text");
    }

    @NotNull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @NotNull
    @Override
    public InvokeInside getFunctionLikeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Nullable
    @Override
    public ShareInPlaygroundAction.Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
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
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull ShareInPlaygroundAction.Context ctx) {
        ShareInPlaygroundAction.performAction(project, ctx);
    }
}
