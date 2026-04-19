/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.changeSignature;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.refactoring.changeSignature.ParameterInfo;
import com.intellij.refactoring.rename.RenameUtil;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.intentions.visibility.ChangeVisibilityIntention;
import org.rust.ide.refactoring.ExtraxtExpressionUtils;
import org.rust.ide.utils.imports.RsImportHelper;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ty.TyUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.rust.lang.core.psi.ext.RsValueParameterUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsElement;

public final class ChangeSignatureImpl {
    private ChangeSignatureImpl() {
    }

    @NotNull
    public static List<RsFunctionUsage> findFunctionUsages(@NotNull RsFunction function) {
        List<RsFunctionUsage> result = new ArrayList<>();
        for (PsiElement usage : RsFunctionUtil.findUsages(function)) {
            if (usage instanceof RsCallExpr) {
                result.add(new RsFunctionUsage.FunctionCall((RsCallExpr) usage));
            } else if (usage instanceof RsMethodCall) {
                result.add(new RsFunctionUsage.MethodCall((RsMethodCall) usage));
            } else if (usage instanceof RsPath) {
                result.add(new RsFunctionUsage.Reference((RsPath) usage));
            }
        }
        return result;
    }

    public static void processFunctionUsage(
        @NotNull RsChangeFunctionSignatureConfig config,
        @NotNull RsFunctionUsage usage
    ) {
        RsFunction function = config.getFunction();
        RsPsiFactory factory = new RsPsiFactory(function.getProject());
        if (config.nameChanged()) {
            renameFunctionUsage(factory, usage, config);
        }
        if (usage.isCallUsage() && config.parameterSetOrOrderChanged()) {
            changeArguments(factory, usage, config, function.isMethod());
        }
    }

    public static void processFunction(
        @NotNull Project project,
        @NotNull RsChangeFunctionSignatureConfig config,
        @NotNull RsFunction function,
        boolean changeSignature
    ) {
        RsPsiFactory factory = new RsPsiFactory(project);

        RsValueParameterList paramList = function.getValueParameterList();
        List<RsValueParameter> parameters = paramList != null ? paramList.getValueParameterList() : null;
        if (parameters != null) {
            renameParameterUsages(parameters, config.getParameters());
        }

        if (!changeSignature) return;

        if (config.nameChanged()) {
            rename(factory, function, config);
        }
        changeVisibility(function, config);
        changeReturnType(factory, function, config);
        changeParameters(factory, function, config);
        changeAsync(factory, function, config);
        changeUnsafe(factory, function, config);

        for (org.rust.lang.core.types.ty.Ty type : config.getAdditionalTypesToImport()) {
            RsImportHelper.importTypeReferencesFromTy(function, type);
        }
    }

    public static void changeVisibility(@NotNull RsFunction function, @NotNull RsChangeFunctionSignatureConfig config) {
        String currentVisText = function.getVis() != null ? function.getVis().getText() : null;
        String configVisText = config.getVisibility() != null ? config.getVisibility().getText() : null;
        if (java.util.Objects.equals(currentVisText, configVisText)) return;

        if (function.getVis() != null) {
            function.getVis().delete();
        }

        RsVis vis = config.getVisibility();
        if (vis != null) {
            PsiElement anchor = ChangeVisibilityIntention.findInsertionAnchor(function);
            function.addBefore(vis, anchor);
        }
    }

    private static void rename(@NotNull RsPsiFactory factory, @NotNull RsFunction function, @NotNull RsChangeFunctionSignatureConfig config) {
        function.getIdentifier().replace(factory.createIdentifier(config.getName()));
    }

    private static void renameFunctionUsage(
        @NotNull RsPsiFactory factory,
        @NotNull RsFunctionUsage usage,
        @NotNull RsChangeFunctionSignatureConfig config
    ) {
        PsiElement identifier = factory.createIdentifier(config.getName());
        if (usage instanceof RsFunctionUsage.Reference) {
            RsPath path = ((RsFunctionUsage.Reference) usage).getPath();
            if (path.getReferenceNameElement() != null) {
                path.getReferenceNameElement().replace(identifier);
            }
        } else if (usage instanceof RsFunctionUsage.FunctionCall) {
            RsCallExpr call = ((RsFunctionUsage.FunctionCall) usage).getCall();
            RsExpr expr = call.getExpr();
            if (expr instanceof RsPathExpr) {
                RsPath path = ((RsPathExpr) expr).getPath();
                if (path.getReferenceNameElement() != null) {
                    path.getReferenceNameElement().replace(identifier);
                }
            }
        } else if (usage instanceof RsFunctionUsage.MethodCall) {
            ((RsFunctionUsage.MethodCall) usage).getCall().getIdentifier().replace(identifier);
        }
    }

    private static void changeReturnType(
        @NotNull RsPsiFactory factory,
        @NotNull RsFunction function,
        @NotNull RsChangeFunctionSignatureConfig config
    ) {
        RsTypeReference currentRetType = function.getRetType() != null ? function.getRetType().getTypeReference() : null;
        if (!areTypesEqual(currentRetType, config.getReturnTypeReference())) {
            if (function.getRetType() != null) {
                function.getRetType().delete();
            }
            if (!(config.getReturnType() instanceof TyUnit)) {
                RsRetType ret = factory.createRetType(config.getReturnTypeReference().getText());
                function.addAfter(ret, function.getValueParameterList());
                RsImportHelper.importTypeReferencesFromElement(function, config.getReturnTypeReference());
            }
        }
    }

    private static void changeArguments(
        @NotNull RsPsiFactory factory,
        @NotNull RsFunctionUsage usage,
        @NotNull RsChangeFunctionSignatureConfig config,
        boolean isMethod
    ) {
        RsValueArgumentList arguments;
        if (usage instanceof RsFunctionUsage.FunctionCall) {
            arguments = ((RsFunctionUsage.FunctionCall) usage).getCall().getValueArgumentList();
        } else if (usage instanceof RsFunctionUsage.MethodCall) {
            arguments = ((RsFunctionUsage.MethodCall) usage).getCall().getValueArgumentList();
        } else {
            throw new IllegalStateException("unreachable");
        }
        for (Parameter parameter : config.getParameters()) {
            RsExpr defaultValue = parameter.getDefaultValue().getItem();
            if (defaultValue == null) continue;
            RsImportHelper.importTypeReferencesFromElement(arguments, defaultValue);
        }
        RsValueArgumentList argumentsCopy = (RsValueArgumentList) arguments.copy();
        List<RsExpr> argumentsList = new ArrayList<>(argumentsCopy.getExprList());
        boolean isUFCS = isMethod && usage instanceof RsFunctionUsage.FunctionCall;
        RsElement selfArg = isUFCS ? argumentsList.get(0) : null;
        List<? extends RsElement> argsWithoutSelf = isUFCS ? argumentsList.subList(1, argumentsList.size()) : argumentsList;

        fixParametersOrder(
            factory, (RsElement) arguments, (RsElement) argumentsCopy,
            selfArg, argsWithoutSelf, config,
            p -> p.getDefaultValue().getItem()
        );
    }

    private static void changeParameters(
        @NotNull RsPsiFactory factory,
        @NotNull RsFunction function,
        @NotNull RsChangeFunctionSignatureConfig config
    ) {
        RsValueParameterList parameters = function.getValueParameterList();
        if (parameters == null) return;

        importParameterTypes(config.getParameters(), function);
        changeParametersNameAndType(parameters.getValueParameterList(), config.getParameters());
        if (!config.parameterSetOrOrderChanged()) return;

        RsValueParameterList parametersCopy = (RsValueParameterList) parameters.copy();
        fixParametersOrder(
            factory, (RsElement) parameters, (RsElement) parametersCopy,
            parametersCopy.getSelfParameter(),
            new ArrayList<>(parametersCopy.getValueParameterList()),
            config,
            p -> factory.tryCreateValueParameter(p.getPatText(), p.getTypeReference(), false)
        );
    }

    private static void changeParametersNameAndType(
        @NotNull List<RsValueParameter> parameters,
        @NotNull List<Parameter> descriptors
    ) {
        for (Parameter descriptor : descriptors) {
            if (descriptor.getIndex() == ParameterInfo.NEW_PARAMETER) continue;
            RsValueParameter psi = parameters.get(descriptor.getIndex());
            changeParameterNameAndType(psi, descriptor);
        }
    }

    private static void changeParameterNameAndType(@NotNull RsValueParameter psi, @NotNull Parameter descriptor) {
        if (psi.getPat() != null && !descriptor.getPatText().equals(psi.getPat().getText())) {
            psi.getPat().replace(descriptor.getPat());
        }
        if (!areTypesEqual(descriptor.getTypeReference(), psi.getTypeReference())) {
            if (psi.getTypeReference() != null) {
                psi.getTypeReference().replace(descriptor.getTypeReference());
            }
        }
    }

    private static void renameParameterUsages(
        @NotNull List<RsValueParameter> parameters,
        @NotNull List<Parameter> descriptors
    ) {
        for (Parameter descriptor : descriptors) {
            if (descriptor.getIndex() != ParameterInfo.NEW_PARAMETER) {
                RsValueParameter psiParameter = parameters.get(descriptor.getIndex());
                RsPatBinding binding = psiParameter.getPat() != null
                    ? ExtraxtExpressionUtils.findBinding(psiParameter.getPat())
                    : null;
                if (binding != null) {
                    String currentPatText = psiParameter.getPat() != null ? psiParameter.getPat().getText() : null;
                    if (!descriptor.getPatText().equals(currentPatText)) {
                        if (descriptor.getPat() instanceof RsPatIdent && psiParameter.getPat() instanceof RsPatIdent) {
                            String newName = descriptor.getPatText();
                            UsageInfo[] usages = RenameUtil.findUsages(binding, newName, false, false, java.util.Collections.emptyMap());
                            for (UsageInfo info : usages) {
                                RenameUtil.rename(info, newName);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void fixParametersOrder(
        @NotNull RsPsiFactory factory,
        @NotNull RsElement parameters,
        @NotNull RsElement parametersCopy,
        @Nullable RsElement parameterSelf,
        @NotNull List<? extends RsElement> parametersList,
        @NotNull RsChangeFunctionSignatureConfig config,
        @NotNull Function<Parameter, PsiElement> createNewParameter
    ) {
        if (parametersList.size() != config.getOriginalParameters().size()) return;
        List<Parameter> descriptors = config.getParameters();

        PsiElement lparen = parameters.getFirstChild();
        PsiElement rparen = parameters.getLastChild();
        if (lparen.getNextSibling() != rparen) {
            parameters.deleteChildRange(lparen.getNextSibling(), rparen.getPrevSibling());
        }
        if (descriptors.isEmpty() && parameterSelf == null) return;

        boolean isMultiline = parametersCopy.getText().contains("\n");
        List<PsiElement> selfGroup = parameterSelf != null ? collectSurroundingWhiteSpaceAndComments(parameterSelf) : null;

        List<List<PsiElement>> groupsWithoutSelf = new ArrayList<>();
        for (Parameter descriptor : descriptors) {
            if (descriptor.getIndex() == ParameterInfo.NEW_PARAMETER) {
                List<PsiElement> group = new ArrayList<>();
                if (isMultiline) {
                    group.add(factory.createNewline());
                }
                PsiElement psi = createNewParameter.apply(descriptor);
                if (psi != null) {
                    group.add(psi);
                }
                groupsWithoutSelf.add(group);
            } else {
                RsElement psi = parametersList.get(descriptor.getIndex());
                groupsWithoutSelf.add(collectSurroundingWhiteSpaceAndComments(psi));
            }
        }

        List<List<PsiElement>> groups = new ArrayList<>();
        if (selfGroup != null) {
            groups.add(selfGroup);
        }
        groups.addAll(groupsWithoutSelf);

        PsiElement rspace = parametersCopy.getLastChild().getPrevSibling() instanceof PsiWhiteSpace
            ? parametersCopy.getLastChild().getPrevSibling()
            : null;
        boolean isSingleParameter = groups.size() == 1 && descriptors.size() < config.getOriginalParameters().size();

        PsiElement lastParam = parametersList.isEmpty() ? null : parametersList.get(parametersList.size() - 1);
        boolean hasTrailingComma = lastParam != null
            && RsElementUtil.getNextNonCommentSibling(lastParam) != null
            && RsElementUtil.getNextNonCommentSibling(lastParam).getNode().getElementType() == RsElementTypes.COMMA;

        for (int i = 0; i < groups.size(); i++) {
            List<PsiElement> group = groups.get(i);
            if (i > 0) {
                parameters.addBefore(factory.createComma(), rparen);
            }
            for (PsiElement element : group) {
                if (element == rspace) continue;
                parameters.addBefore(element, rparen);
            }
        }

        if (isSingleParameter) {
            PsiElement lspace = parameters.getFirstChild().getNextSibling();
            if (lspace instanceof PsiWhiteSpace) lspace.delete();
        } else {
            if (hasTrailingComma && isMultiline) {
                parameters.addBefore(factory.createComma(), rparen);
            }
            if (rspace != null) {
                parameters.addBefore(rspace, rparen);
            }
        }
    }

    @NotNull
    private static List<PsiElement> collectSurroundingWhiteSpaceAndComments(@NotNull PsiElement element) {
        PsiElement prevNonComment = RsElementUtil.getPrevNonCommentSibling(element);
        PsiElement first = prevNonComment != null ? prevNonComment.getNextSibling() : element;
        PsiElement nextNonComment = RsElementUtil.getNextNonCommentSibling(element);
        PsiElement last = nextNonComment != null ? nextNonComment : (element.getNextSibling());
        List<PsiElement> result = new ArrayList<>();
        PsiElement current = first;
        while (current != null && current != last) {
            result.add(current);
            current = current.getNextSibling();
        }
        return result;
    }

    private static void importParameterTypes(@NotNull List<Parameter> descriptors, @NotNull RsElement context) {
        for (Parameter descriptor : descriptors) {
            RsImportHelper.importTypeReferencesFromElement(context, descriptor.getTypeReference());
        }
    }

    private static void changeAsync(@NotNull RsPsiFactory factory, @NotNull RsFunction function, @NotNull RsChangeFunctionSignatureConfig config) {
        com.intellij.lang.ASTNode asyncNode = function.getNode().findChildByType(RsElementTypes.ASYNC);
        PsiElement async = asyncNode != null ? asyncNode.getPsi() : null;
        if (config.isAsync()) {
            if (async == null) {
                PsiElement asyncKw = factory.createAsyncKeyword();
                PsiElement anchor = function.getUnsafe() != null ? function.getUnsafe() : function.getFn();
                function.addBefore(asyncKw, anchor);
            }
        } else {
            if (async != null) {
                async.delete();
            }
        }
    }

    private static void changeUnsafe(@NotNull RsPsiFactory factory, @NotNull RsFunction function, @NotNull RsChangeFunctionSignatureConfig config) {
        if (config.isUnsafe()) {
            if (function.getUnsafe() == null) {
                PsiElement unsafe = factory.createUnsafeKeyword();
                function.addBefore(unsafe, function.getFn());
            }
        } else {
            if (function.getUnsafe() != null) {
                function.getUnsafe().delete();
            }
        }
    }

    private static boolean areTypesEqual(@Nullable RsTypeReference t1, @Nullable RsTypeReference t2) {
        String text1 = t1 != null ? t1.getText() : "()";
        String text2 = t2 != null ? t2.getText() : "()";
        return text1.equals(text2);
    }
}
