/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.refactoring.RsNameSuggestions;
import org.rust.ide.utils.template.EditorExtUtil;
import org.rust.lang.core.completion.RsLookupElementProperties.KeywordKind;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyFunctionBase;
import org.rust.lang.core.types.ty.TyTypeParameter;
import org.rust.lang.core.types.infer.TyWithObligations;
import org.rust.openapiext.SmartPointerExtUtil;

import java.util.*;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;

public class RsLambdaExprCompletionProvider extends RsCompletionProvider {
    public static final RsLambdaExprCompletionProvider INSTANCE = new RsLambdaExprCompletionProvider();

    private RsLambdaExprCompletionProvider() {
    }

    @NotNull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        return psiElement(PsiElement.class)
            .withSuperParent(2, psiElement(RsPathExpr.class));
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        PsiElement element = Utils.safeGetOriginalOrSelf(parameters.getPosition());
        RsExpr expr = PsiTreeUtil.getParentOfType(element, RsExpr.class);
        if (expr == null) return;
        Ty exprExpectedType = ExtensionsUtil.getExpectedType(expr);
        if (exprExpectedType == null) return;

        org.rust.lang.core.resolve.ImplLookup lookup;
        if (exprExpectedType instanceof TyTypeParameter && ((TyTypeParameter) exprExpectedType).getParameter() instanceof TyTypeParameter.Named) {
            lookup = ExtensionsUtil.getImplLookup(((TyTypeParameter.Named) ((TyTypeParameter) exprExpectedType).getParameter()).getParameter());
        } else {
            lookup = ExtensionsUtil.getImplLookup(expr);
        }

        TyWithObligations<TyFunctionBase> fnResult = lookup.asTyFunction(exprExpectedType);
        if (fnResult == null) return;
        TyFunctionBase fnValue = fnResult.getValue();
        if (fnValue == null) return;
        List<Ty> paramTypes = fnValue.getParamTypes();

        String params = suggestedParams(paramTypes, expr);
        PsiElement firstNonErrorSibling = null;
        for (PsiElement sibling = expr; sibling != null; sibling = sibling.getPrevSibling()) {
            if (!(sibling instanceof PsiErrorElement) && !(sibling instanceof com.intellij.psi.PsiWhiteSpace)) {
                firstNonErrorSibling = sibling;
                break;
            }
        }
        String start;
        if (firstNonErrorSibling != null && firstNonErrorSibling.getNode().getElementType() == RsElementTypes.OR) {
            start = "";
        } else {
            start = "|";
        }
        String text = start + params + "| {}";

        result.addElement(
            LookupElements.toKeywordElement(
                LookupElementBuilder
                    .create(text)
                    .bold()
                    .withPresentableText("|| {}")
                    .withInsertHandler((ctx, item) -> handleInsert(ctx)),
                KeywordKind.LAMBDA_EXPR
            )
        );
    }

    private static void handleInsert(InsertionContext ctx) {
        RsLambdaExpr lambda = LookupElements.getElementOfType(ctx, RsLambdaExpr.class);
        if (lambda == null) return;
        List<RsPatIdent> pats = new ArrayList<>(PsiTreeUtil.findChildrenOfType(lambda, RsPatIdent.class));

        com.intellij.openapi.util.Ref<com.intellij.psi.SmartPsiElementPointer<RsLambdaExpr>> lambdaPtr =
            new com.intellij.openapi.util.Ref<>(SmartPointerExtUtil.createSmartPointer(lambda));

        org.rust.ide.utils.template.RsTemplateBuilder tpl = EditorExtUtil.newTemplateBuilder(ctx.getEditor(), lambda);
        for (RsPatIdent pat : pats) {
            tpl.replaceElement(pat, (String) null);
        }
        RsExpr expr = lambda.getExpr();
        if (expr != null) {
            tpl.replaceElement(expr, (String) null);
        }
        tpl.runInline(() -> {
            RsLambdaExpr element = lambdaPtr.get().getElement();
            if (element != null) {
                RsExpr blockExprElement = element.getExpr();
                if (!(blockExprElement instanceof RsBlockExpr)) return;
                RsBlockExpr blockExpr = (RsBlockExpr) blockExprElement;
                PsiElement lbrace = blockExpr.getBlock().getLbrace();
                PsiElement next = lbrace.getNextSibling();
                while (next instanceof com.intellij.psi.PsiWhiteSpace) {
                    next = next.getNextSibling();
                }
                if (next != blockExpr.getBlock().getRbrace()) return;
                int offset = lbrace.getTextRange().getStartOffset() + 1;
                ctx.getEditor().getCaretModel().moveToOffset(offset);
            }
        });
    }

    private static String suggestedParams(List<Ty> paramTypes, RsExpr contextExpr) {
        Set<String> alreadyGivenNames = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramTypes.size(); i++) {
            if (i > 0) sb.append(", ");
            Ty ty = paramTypes.get(i);
            String name = RsNameSuggestions.suggestedNames(ty, contextExpr, alreadyGivenNames).getDefault();
            alreadyGivenNames.add(name);
            sb.append(name);
        }
        return sb.toString();
    }
}
