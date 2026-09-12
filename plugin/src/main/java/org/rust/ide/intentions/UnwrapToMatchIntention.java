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
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsMethodCallUtil;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.lang.core.types.RsTypesUtil;
import consulo.localize.LocalizeValue;

public class UnwrapToMatchIntention extends RsElementBaseIntentionAction<UnwrapToMatchIntention.Context> {
    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.replace.unwrap.with.match"));
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    public enum ReceiverType {
        OPTION("Some(x) => x", "None"),
        RESULT("Ok(x) => x", "Err(_)");

        private final String myValueBranch;
        private final String myNonValueMatcher;

        ReceiverType(@Nonnull String valueBranch, @Nonnull String nonValueMatcher) {
            myValueBranch = valueBranch;
            myNonValueMatcher = nonValueMatcher;
        }

        @Nonnull
        public String getValueBranch() {
            return myValueBranch;
        }

        @Nonnull
        public String getNonValueMatcher() {
            return myNonValueMatcher;
        }
    }

    public static class Context {
        private final RsMethodCall myMethodCall;
        private final ReceiverType myReceiverType;

        public Context(@Nonnull RsMethodCall methodCall, @Nonnull ReceiverType receiverType) {
            myMethodCall = methodCall;
            myReceiverType = receiverType;
        }

        @Nonnull
        public RsMethodCall getMethodCall() {
            return myMethodCall;
        }

        @Nonnull
        public ReceiverType getReceiverType() {
            return myReceiverType;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        RsMethodCall methodCall = PsiElementExt.ancestorOrSelf(element, RsMethodCall.class);
        if (methodCall == null) return null;
        RsExpr receiver = RsMethodCallUtil.getReceiver(methodCall);
        if (!(RsTypesUtil.getType(receiver) instanceof TyAdt)) return null;
        RsEnumItem itemType = (RsEnumItem) ((TyAdt) RsTypesUtil.getType(receiver)).getItem();
        if (!(itemType instanceof RsEnumItem)) return null;
        ReceiverType enumType = getReceiverType(itemType);
        if (enumType == null) return null;

        boolean isAppropriateMethod = "unwrap".equals(methodCall.getReferenceName())
            && methodCall.getTypeArgumentList() == null
            && methodCall.getValueArgumentList().getExprList().isEmpty()
            && PsiModificationUtil.canReplace(RsMethodCallUtil.getParentDotExpr(methodCall));

        if (!isAppropriateMethod) return null;

        return new Context(methodCall, enumType);
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
        RsMethodCall methodCall = ctx.getMethodCall();
        ReceiverType enumType = ctx.getReceiverType();
        String generatedCode = "match " + RsMethodCallUtil.getReceiver(methodCall).getText() + " {"
            + enumType.getValueBranch() + ","
            + enumType.getNonValueMatcher() + " => todo!(),"
            + "}";

        RsMatchExpr matchExpression = (RsMatchExpr) new RsPsiFactory(project).createExpression(generatedCode);
        RsMethodCallUtil.getParentDotExpr(methodCall).replace(matchExpression);
    }

    @Nullable
    private ReceiverType getReceiverType(@Nonnull RsEnumItem item) {
        KnownItems knownItems = KnownItems.getKnownItems(item);
        if (item.equals(knownItems.getOption())) return ReceiverType.OPTION;
        if (item.equals(knownItems.getResult())) return ReceiverType.RESULT;
        return null;
    }
}
