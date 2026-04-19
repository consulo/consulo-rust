/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.ide.presentation.TypeRendering;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.ide.utils.template.EditorExt;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyFunctionBase;
import org.rust.lang.core.types.ty.TyUnit;
import org.rust.lang.core.types.ty.TyUnknown;
import org.rust.lang.core.types.ty.Ty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsValueParameterUtil;

public class ConvertClosureToFunctionIntention extends RsElementBaseIntentionAction<ConvertClosureToFunctionIntention.Context> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.convert.closure.to.function");
    }

    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static class Context {
        public final RsLetDecl letDecl;
        public final RsLambdaExpr lambda;

        public Context(RsLetDecl letDecl, RsLambdaExpr lambda) {
            this.letDecl = letDecl;
            this.lambda = lambda;
        }
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        RsLetDecl possibleTarget = RsPsiJavaUtil.ancestorStrict(element, RsLetDecl.class);
        if (possibleTarget == null) return null;

        PsiElement expr = possibleTarget.getExpr();
        if (!(expr instanceof RsLambdaExpr)) return null;
        RsLambdaExpr lambdaExpr = (RsLambdaExpr) expr;

        int startOffset = possibleTarget.getLet().getTextRange().getStartOffset();
        int endOffset = lambdaExpr.getRetType() != null
            ? lambdaExpr.getRetType().getTextRange().getEndOffset()
            : lambdaExpr.getValueParameterList().getTextRange().getEndOffset();
        TextRange availabilityRange = new TextRange(startOffset, endOffset);
        if (!availabilityRange.containsOffset(element.getTextRange().getStartOffset())) return null;
        if (!PsiModificationUtil.canReplace(possibleTarget)) return null;

        return new Context(possibleTarget, lambdaExpr);
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        RsPsiFactory factory = new RsPsiFactory(project);

        RsPatIdent patIdent = ctx.letDecl.getPat() instanceof RsPatIdent ? (RsPatIdent) ctx.letDecl.getPat() : null;
        String letBindingName = patIdent != null ? patIdent.getPatBinding().getNameIdentifier().getText() : null;
        boolean useDefaultName = letBindingName == null;
        String targetFunctionName = letBindingName != null ? letBindingName : "func";

        Ty type = RsTypesUtil.getType(ctx.lambda);
        if (!(type instanceof TyFunctionBase)) return;
        TyFunctionBase fnType = (TyFunctionBase) type;

        List<RsValueParameter> params = ctx.lambda.getValueParameters();
        List<Ty> paramTypes = fnType.getParamTypes();
        StringBuilder parametersText = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) parametersText.append(", ");
            String patText = RsValueParameterUtil.getPatText(params.get(i)) != null ? RsValueParameterUtil.getPatText(params.get(i)) : "_";
            String typeText = TypeRendering.renderInsertionSafe(paramTypes.get(i));
            parametersText.append(patText).append(": ").append(typeText);
        }

        RsRetType lambdaRetTypePsi = ctx.lambda.getRetType();
        Ty lambdaRetTy = fnType.getRetType();

        String returnText;
        if (lambdaRetTypePsi != null) {
            returnText = lambdaRetTypePsi.getText();
        } else if (!(lambdaRetTy instanceof TyUnknown) && !(lambdaRetTy instanceof TyUnit)) {
            returnText = "-> " + TypeRendering.renderInsertionSafe(lambdaRetTy);
        } else {
            returnText = "";
        }

        RsExpr lambdaExpr = ctx.lambda.getExpr();
        String body;
        if (lambdaExpr instanceof RsBlockExpr) {
            body = lambdaExpr.getText();
        } else {
            body = "{ " + (lambdaExpr != null ? lambdaExpr.getText() : "") + " }";
        }

        RsFunction function = factory.createFunction("fn " + targetFunctionName + "(" + parametersText + ") " + returnText + " " + body);
        RsFunction replaced = (RsFunction) ctx.letDecl.replace(function);

        List<PsiElement> placeholders = findPlaceholders(replaced);

        if (useDefaultName || !placeholders.isEmpty()) {
            List<PsiElement> placeholderElements = new ArrayList<>();
            if (useDefaultName) {
                placeholderElements.add(replaced.getIdentifier());
            }
            placeholderElements.addAll(placeholders);
            EditorExt.buildAndRunTemplate(editor, replaced, placeholderElements);
        } else {
            org.rust.openapiext.Editor.moveCaretToOffset(editor, replaced, replaced.getTextRange().getEndOffset());
        }
    }

    private List<PsiElement> findPlaceholders(RsFunction replaced) {
        RsValueParameterList parameters = replaced.getValueParameterList();
        if (parameters == null) return Collections.emptyList();
        List<PsiElement> result = new ArrayList<>();
        result.addAll(RsPsiJavaUtil.descendantsOfType(parameters, RsPatWild.class));
        List<RsPath> paths = RsPsiJavaUtil.descendantsOfType(parameters, RsPath.class);
        for (RsPath path : paths) {
            RsPath subPath = path.getPath();
            if (subPath != null && "_".equals(subPath.getText())) {
                result.add(path);
            }
        }
        return result;
    }
}
