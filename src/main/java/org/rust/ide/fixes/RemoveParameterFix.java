/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.completion.CompletionUtilsUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.PsiElementUtil;
// CompletionUtilsUtil removed
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.psi.ext.RsPatBindingUtil;

/**
 * Fix that removes a parameter and all its usages at call sites.
 */
public class RemoveParameterFix extends RsQuickFixBase<RsPatBinding> {

    private final String bindingName;

    public RemoveParameterFix(@NotNull RsPatBinding binding, @NotNull String bindingName) {
        super(binding);
        this.bindingName = bindingName;
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.remove.parameter", bindingName);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.remove.parameter");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsPatBinding element) {
        RsPat topLevelPat = RsPatBindingUtil.getTopLevelPattern(element);
        if (!(topLevelPat instanceof RsPatIdent)) return;
        PsiElement parent = topLevelPat.getParent();
        if (!(parent instanceof RsValueParameter)) return;
        RsValueParameter parameter = (RsValueParameter) parent;
        RsFunction function = PsiTreeUtil.getParentOfType(parameter, RsFunction.class);
        if (function == null) return;
        RsFunction originalFunction = CompletionUtilsUtil.safeGetOriginalOrSelf(function);
        List<PsiElement> calls;
        if (RsPsiImplUtil.isIntentionPreviewElement(function)) {
            calls = Collections.emptyList();
        } else {
            calls = new ArrayList<>();
            for (PsiElement c : RsFunctionUtil.findFunctionCalls(originalFunction)) calls.add(c);
            for (PsiElement c : RsFunctionUtil.findMethodCalls(originalFunction)) calls.add(c);
        }

        RsValueParameterList paramList = function.getValueParameterList();
        if (paramList == null) return;
        int parameterIndex = paramList.getValueParameterList().indexOf(parameter);
        if (parameterIndex == -1) return;

        PsiElementUtil.deleteWithSurroundingCommaAndWhitespace(parameter);
        removeArguments(function, calls, parameterIndex);
    }

    private static void removeArguments(@NotNull RsFunction function, @NotNull List<PsiElement> calls, int parameterIndex) {
        for (PsiElement call : calls) {
            RsValueArgumentList arguments;
            if (call instanceof RsCallExpr) {
                arguments = ((RsCallExpr) call).getValueArgumentList();
            } else if (call instanceof RsMethodCall) {
                arguments = ((RsMethodCall) call).getValueArgumentList();
            } else {
                continue;
            }
            boolean isMethod = RsFunctionUtil.getHasSelfParameters(function);
            int argumentIndex;
            if (isMethod && call instanceof RsCallExpr) {
                argumentIndex = parameterIndex + 1; // UFCS
            } else {
                argumentIndex = parameterIndex;
            }
            List<RsExpr> exprList = arguments.getExprList();
            if (argumentIndex < exprList.size()) {
                PsiElementUtil.deleteWithSurroundingCommaAndWhitespace(exprList.get(argumentIndex));
            }
        }
    }
}
