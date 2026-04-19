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
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsMethodCallUtil;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.lang.core.types.RsTypesUtil;

public class UnwrapToMatchIntention extends RsElementBaseIntentionAction<UnwrapToMatchIntention.Context> {
    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.replace.unwrap.with.match");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    public enum ReceiverType {
        OPTION("Some(x) => x", "None"),
        RESULT("Ok(x) => x", "Err(_)");

        private final String myValueBranch;
        private final String myNonValueMatcher;

        ReceiverType(@NotNull String valueBranch, @NotNull String nonValueMatcher) {
            myValueBranch = valueBranch;
            myNonValueMatcher = nonValueMatcher;
        }

        @NotNull
        public String getValueBranch() {
            return myValueBranch;
        }

        @NotNull
        public String getNonValueMatcher() {
            return myNonValueMatcher;
        }
    }

    public static class Context {
        private final RsMethodCall myMethodCall;
        private final ReceiverType myReceiverType;

        public Context(@NotNull RsMethodCall methodCall, @NotNull ReceiverType receiverType) {
            myMethodCall = methodCall;
            myReceiverType = receiverType;
        }

        @NotNull
        public RsMethodCall getMethodCall() {
            return myMethodCall;
        }

        @NotNull
        public ReceiverType getReceiverType() {
            return myReceiverType;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
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
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
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
    private ReceiverType getReceiverType(@NotNull RsEnumItem item) {
        KnownItems knownItems = KnownItems.getKnownItems(item);
        if (item.equals(knownItems.getOption())) return ReceiverType.OPTION;
        if (item.equals(knownItems.getResult())) return ReceiverType.RESULT;
        return null;
    }
}
