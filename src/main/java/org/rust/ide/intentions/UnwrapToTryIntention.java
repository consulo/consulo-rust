/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.RsMethodCall;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsMethodCallUtil;

public class UnwrapToTryIntention extends RsElementBaseIntentionAction<RsMethodCall> {
    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.replace.unwrap.with.try");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Nullable
    @Override
    public RsMethodCall findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        RsMethodCall methodCall = PsiElementExt.ancestorOrSelf(element, RsMethodCall.class);
        if (methodCall == null) return null;
        boolean isAppropriateMethod = "unwrap".equals(methodCall.getReferenceName())
            && methodCall.getTypeArgumentList() == null
            && methodCall.getValueArgumentList().getExprList().isEmpty()
            && PsiModificationUtil.canReplace(RsMethodCallUtil.getParentDotExpr(methodCall));

        if (!isAppropriateMethod) return null;
        return methodCall;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull RsMethodCall ctx) {
        PsiElement tryElement = new RsPsiFactory(project).createTryExpression(RsMethodCallUtil.getReceiver(ctx));
        RsMethodCallUtil.getParentDotExpr(ctx).replace(tryElement);
    }
}
