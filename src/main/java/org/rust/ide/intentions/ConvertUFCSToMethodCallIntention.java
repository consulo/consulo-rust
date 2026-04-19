/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.rust.RsBundle;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

import java.util.List;
import org.rust.lang.core.psi.ext.RsSelfParameterUtil;

public class ConvertUFCSToMethodCallIntention extends RsElementBaseIntentionAction<ConvertUFCSToMethodCallIntention.Context> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.convert.to.method.call");
    }

    @Override
    public String getFamilyName() {
        return getText();
    }

    public static class Context {
        public final RsCallExpr callExpr;
        public final RsFunction function;

        public Context(RsCallExpr callExpr, RsFunction function) {
            this.callExpr = callExpr;
            this.function = function;
        }
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        RsCallExpr callExpr = RsPsiJavaUtil.ancestorStrict(element, RsCallExpr.class, RsValueArgumentList.class);
        if (callExpr == null) return null;
        PsiElement expr = callExpr.getExpr();
        if (!(expr instanceof RsPathExpr)) return null;
        RsPath path = ((RsPathExpr) expr).getPath();
        if (path == null) return null;
        PsiElement resolved = path.getReference() != null ? path.getReference().resolve() : null;
        if (!(resolved instanceof RsFunction)) return null;
        RsFunction function = (RsFunction) resolved;
        if (!function.isMethod()) return null;
        if (callExpr.getValueArgumentList().getExprList().isEmpty()) return null;
        if (!PsiModificationUtil.canReplace(callExpr)) return null;

        return new Context(callExpr, function);
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        List<RsExpr> arguments = ctx.callExpr.getValueArgumentList().getExprList();
        RsExpr selfArgument = arguments.get(0);

        String name = ctx.function.getName();
        if (name == null) return;
        List<RsExpr> restArguments = arguments.subList(1, arguments.size());
        PsiElement methodCall = createMethodCall(ctx.function, selfArgument, name, restArguments);
        if (methodCall == null) return;
        ctx.callExpr.replace(methodCall);
    }

    private static PsiElement createMethodCall(
        RsFunction function,
        RsExpr selfArgument,
        String name,
        List<RsExpr> restArguments
    ) {
        RsExpr self = normalizeSelf(selfArgument, function);

        RsPsiFactory factory = new RsPsiFactory(selfArgument.getProject());
        PsiElement call = factory.tryCreateMethodCall(self, name, restArguments);
        if (call != null) return call;

        RsExpr parenthesesExpr = factory.tryCreateExpression("(" + self.getText() + ")");
        if (parenthesesExpr == null) return null;
        return factory.tryCreateMethodCall(parenthesesExpr, name, restArguments);
    }

    private static RsExpr normalizeSelf(RsExpr selfArgument, RsFunction function) {
        RsSelfParameter selfParam = function.getSelfParameter();
        boolean isRef = selfParam != null && RsSelfParameterUtil.isRef(selfParam);
        if (!isRef) return selfArgument;
        return RsPsiJavaUtil.unwrapReference(selfArgument);
    }
}
