/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.rust.RsBundle;
import org.rust.ide.inspections.imports.AutoImportFixFactory;
import org.rust.ide.presentation.TypeRendering;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.core.resolve.TraitImplSource;
import org.rust.lang.core.resolve.ref.MethodResolveVariant;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.*;

import java.util.ArrayList;
import java.util.List;
import org.rust.lang.core.psi.ext.RsMethodCallUtil;
import org.rust.lang.core.psi.ext.RsSelfParameterUtil;

public class ConvertMethodCallToUFCSIntention extends RsElementBaseIntentionAction<ConvertMethodCallToUFCSIntention.Context> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.convert.to.ufcs");
    }

    @Override
    public String getFamilyName() {
        return getText();
    }

    public static class Context {
        public final RsMethodCall methodCall;
        public final RsFunction function;
        public final List<MethodResolveVariant> methodVariants;

        public Context(RsMethodCall methodCall, RsFunction function, List<MethodResolveVariant> methodVariants) {
            this.methodCall = methodCall;
            this.function = function;
            this.methodVariants = methodVariants;
        }
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        PsiElement parent = element.getParent();
        if (!(parent instanceof RsMethodCall)) return null;
        RsMethodCall methodCall = (RsMethodCall) parent;
        PsiElement resolved = methodCall.getReference().resolve();
        if (!(resolved instanceof RsFunction)) return null;
        RsFunction function = (RsFunction) resolved;
        List<MethodResolveVariant> methodVariants = RsPsiJavaUtil.getResolvedMethod(methodCall);
        if (methodVariants == null || methodVariants.isEmpty()) return null;
        if (!PsiModificationUtil.canReplace(RsMethodCallUtil.getParentDotExpr(methodCall))) return null;
        return new Context(methodCall, function, methodVariants);
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        RsMethodCall methodCall = ctx.methodCall;
        RsFunction function = ctx.function;
        String functionName = function.getName();
        if (functionName == null) return;

        RsPsiFactory factory = new RsPsiFactory(project);

        SelfType selfType = getSelfType(function);
        if (selfType == null) return;
        RsExpr receiver = RsMethodCallUtil.getReceiver(methodCall);
        String prefix = getSelfArgumentPrefix(selfType, receiver);
        RsExpr selfArgument = factory.createExpression(prefix + receiver.getText());

        List<RsExpr> arguments = new ArrayList<>();
        arguments.add(selfArgument);
        arguments.addAll(methodCall.getValueArgumentList().getExprList());
        String ownerName = getOwnerName(ctx.methodVariants);
        RsCallExpr ufcs = factory.createAssocFunctionCall(ownerName, functionName, arguments);

        RsDotExpr parentDot = RsMethodCallUtil.getParentDotExpr(methodCall);
        RsCallExpr inserted = (RsCallExpr) parentDot.replace(ufcs);
        RsPathExpr pathExpr = inserted.getExpr() instanceof RsPathExpr ? (RsPathExpr) inserted.getExpr() : null;
        if (pathExpr == null) return;
        RsPath path = pathExpr.getPath();
        if (path == null) return;

        var importCtx = AutoImportFixFactory.findApplicableContext(path);
        if (importCtx != null) {
            var candidates = AutoImportFixFactory.getCandidates(importCtx);
            if (!candidates.isEmpty()) {
                AutoImportFixFactory.importCandidate(candidates.get(0), inserted);
            }
        }
    }

    private static String getOwnerName(List<MethodResolveVariant> methodVariants) {
        MethodResolveVariant variant = null;
        int minPriority = Integer.MAX_VALUE;
        for (MethodResolveVariant v : methodVariants) {
            int priority = v.getSource() instanceof TraitImplSource.ExplicitImpl ? 0 : 1;
            if (priority < minPriority) {
                minPriority = priority;
                variant = v;
            }
        }
        if (variant == null) throw new IllegalStateException("Method not resolved to any variant");

        Ty type = variant.getSelfTy();
        if (type instanceof TyAnon || type instanceof TyTraitObject) {
            PsiElement sourceValue = variant.getSource().getValue();
            if (sourceValue instanceof RsTraitItem) {
                String name = ((RsTraitItem) sourceValue).getName();
                if (name != null) return name;
            }
        }
        return TypeRendering.renderInsertionSafe(type, false, false);
    }

    private enum SelfType {
        Move,
        Ref,
        RefMut
    }

    private static SelfType getSelfType(RsFunction function) {
        RsSelfParameter self = function.getSelfParameter();
        if (self == null) return null;
        boolean ref = RsSelfParameterUtil.isRef(self);

        if (!ref) return SelfType.Move;
        if (RsSelfParameterUtil.getMutability(self) == Mutability.MUTABLE) return SelfType.RefMut;
        return SelfType.Ref;
    }

    private static String getSelfArgumentPrefix(SelfType selfType, RsExpr receiver) {
        Ty type = RsTypesUtil.getType(receiver);
        switch (selfType) {
            case Move:
                return "";
            case Ref:
                return type instanceof TyReference ? "" : "&";
            case RefMut:
                return type instanceof TyReference ? "" : "&mut ";
            default:
                return "";
        }
    }
}
