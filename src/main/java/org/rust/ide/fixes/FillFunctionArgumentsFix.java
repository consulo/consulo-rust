/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.annotator.FunctionCallContextUtil;
import org.rust.ide.utils.template.EditorExtUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyFunctionBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FillFunctionArgumentsFix extends RsQuickFixBase<PsiElement> {

    public FillFunctionArgumentsFix(@NotNull PsiElement element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.fill.missing.arguments");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
        RsValueArgumentList arguments = PsiTreeUtil.getParentOfType(element, RsValueArgumentList.class, false);
        if (arguments == null) return;
        PsiElement parent = arguments.getParent();
        if (!(parent instanceof RsElement)) return;
        RsElement rsParent = (RsElement) parent;

        var context = FunctionCallContextUtil.getFunctionCallContext(arguments);
        if (context == null) return;
        int requiredParameterCount = context.getExpectedParameterCount();
        List<Ty> parameters = getParameterTypes(parent);
        if (parameters == null) return;
        if (parameters.size() > requiredParameterCount) {
            parameters = parameters.subList(0, requiredParameterCount);
        }

        RsPsiFactory factory = new RsPsiFactory(project);
        RsDefaultValueBuilder builder = new RsDefaultValueBuilder(
            KnownItems.knownItems(rsParent),
            RsElementUtil.getContainingMod(rsParent),
            factory
        );
        Map<String, RsPatBinding> bindings = RsElementUtil.getLocalVariableVisibleBindings(rsParent);

        int parameterIndex = 0;
        int argumentIndex = -1;

        List<RsExpr> argumentList = new ArrayList<>();
        List<Integer> newArgumentIndices = new ArrayList<>();
        List<PsiElement> children = new ArrayList<>();
        for (PsiElement child = arguments.getFirstChild(); child != null; child = child.getNextSibling()) {
            children.add(child);
        }
        int childIndex = 0;

        while (parameterIndex < parameters.size()) {
            PsiElement child = childIndex < children.size() ? children.get(childIndex) : null;
            childIndex++;

            boolean isComma = false;
            if (child != null) {
                isComma = child.getNode().getElementType() == RsElementTypes.COMMA
                    || (child instanceof PsiErrorElement && child.getFirstChild() != null
                        && child.getFirstChild().getNode().getElementType() == RsElementTypes.COMMA);
            }
            boolean atEnd = child == null || child.getNode().getElementType() == RsElementTypes.RPAREN;

            if (atEnd || isComma) {
                if (argumentIndex < parameterIndex) {
                    argumentList.add(builder.buildFor(parameters.get(parameterIndex), bindings));
                    argumentIndex++;
                    newArgumentIndices.add(argumentIndex);
                }
                parameterIndex++;
            } else if (child instanceof RsExpr) {
                argumentList.add((RsExpr) child);
                argumentIndex++;
            }
        }

        RsValueArgumentList newArgumentList = factory.tryCreateValueArgumentList(argumentList);
        if (newArgumentList == null) return;
        RsValueArgumentList insertedArguments = (RsValueArgumentList) arguments.replace(newArgumentList);
        List<RsExpr> toBeChanged = new ArrayList<>();
        for (int idx : newArgumentIndices) {
            toBeChanged.add(insertedArguments.getExprList().get(idx));
        }

        if (editor != null) {
            EditorExtUtil.buildAndRunTemplate(editor, insertedArguments, toBeChanged);
        }
    }

    @Nullable
    private static List<Ty> getParameterTypes(@NotNull PsiElement element) {
        if (element instanceof RsCallExpr) {
            Ty type = RsTypesUtil.getType(((RsCallExpr) element).getExpr());
            if (type instanceof TyFunctionBase) {
                return ((TyFunctionBase) type).getParamTypes();
            }
        } else if (element instanceof RsMethodCall) {
            var inference = ExtensionsUtil.getInference((RsMethodCall) element);
            if (inference != null) {
                var resolvedType = inference.getResolvedMethodType((RsMethodCall) element);
                if (resolvedType != null) {
                    List<Ty> paramTypes = resolvedType.getParamTypes();
                    if (paramTypes.size() > 1) {
                        return paramTypes.subList(1, paramTypes.size());
                    }
                    return new ArrayList<>();
                }
            }
        }
        return null;
    }
}
