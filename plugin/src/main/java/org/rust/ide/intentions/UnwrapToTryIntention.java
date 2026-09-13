/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.RsMethodCall;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.ext.impl.PsiElementExt;
import org.rust.lang.core.psi.ext.impl.RsMethodCallUtil;
import consulo.localize.LocalizeValue;

public class UnwrapToTryIntention extends RsElementBaseIntentionAction<RsMethodCall> {
    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.replace.unwrap.with.try"));
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    @Nullable
    @Override
    public RsMethodCall findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
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
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull RsMethodCall ctx) {
        PsiElement tryElement = new RsPsiFactory(project).createTryExpression(RsMethodCallUtil.getReceiver(ctx));
        RsMethodCallUtil.getParentDotExpr(ctx).replace(tryElement);
    }
}
