/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractFunction;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.presentation.RenderingUtil;
import org.rust.ide.refactoring.RsFunctionSignatureConfig;
import org.rust.ide.utils.ExpressionUtils;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.ty.*;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.ide.presentation.TypeRendering;
import org.rust.lang.core.psi.ext.RsStmtUtil;

public class RsExtractFunctionConfig extends RsFunctionSignatureConfig {
    @NotNull
    private final List<PsiElement> myElements;
    @NotNull
    private final OutputVariables myOutputVariables;
    @Nullable
    private final ControlFlow myControlFlow;
    @NotNull
    private final ReturnKind myReturnKind;
    @NotNull
    private String myName;
    private boolean myVisibilityLevelPublic;
    private final boolean myIsAsync;
    private final boolean myIsUnsafe;
    @NotNull
    private List<Parameter> myParameters;

    private RsExtractFunctionConfig(
        @NotNull RsFunction function,
        @NotNull List<PsiElement> elements,
        @NotNull OutputVariables outputVariables,
        @Nullable ControlFlow controlFlow,
        @NotNull ReturnKind returnKind,
        @NotNull String name,
        boolean visibilityLevelPublic,
        boolean isAsync,
        boolean isUnsafe,
        @NotNull List<Parameter> parameters
    ) {
        super(function);
        myElements = elements;
        myOutputVariables = outputVariables;
        myControlFlow = controlFlow;
        myReturnKind = returnKind;
        myName = name;
        myVisibilityLevelPublic = visibilityLevelPublic;
        myIsAsync = isAsync;
        myIsUnsafe = isUnsafe;
        myParameters = parameters;
    }

    @NotNull
    public List<PsiElement> getElements() {
        return myElements;
    }

    @NotNull
    public OutputVariables getOutputVariables() {
        return myOutputVariables;
    }

    @Nullable
    public ControlFlow getControlFlow() {
        return myControlFlow;
    }

    @NotNull
    public ReturnKind getReturnKind() {
        return myReturnKind;
    }

    @NotNull
    public String getName() {
        return myName;
    }

    public void setName(@NotNull String name) {
        myName = name;
    }

    public boolean isVisibilityLevelPublic() {
        return myVisibilityLevelPublic;
    }

    public void setVisibilityLevelPublic(boolean visibilityLevelPublic) {
        myVisibilityLevelPublic = visibilityLevelPublic;
    }

    public boolean isAsync() {
        return myIsAsync;
    }

    public boolean isUnsafe() {
        return myIsUnsafe;
    }

    @NotNull
    public List<Parameter> getParameters() {
        return myParameters;
    }

    public void setParameters(@NotNull List<Parameter> parameters) {
        myParameters = parameters;
    }

    @NotNull
    public List<Parameter> getValueParameters() {
        return myParameters.stream().filter(p -> !p.isSelf()).collect(Collectors.toList());
    }

    @NotNull
    public String getArgumentsText() {
        return getValueParameters().stream()
            .filter(Parameter::isSelected)
            .map(Parameter::getArgumentText)
            .collect(Collectors.joining(", "));
    }

    @NotNull
    public String getSignature() {
        return signature(false);
    }

    @NotNull
    public List<Ty> getParametersAndReturnTypes() {
        List<Ty> result = new ArrayList<>();
        for (Parameter p : myParameters) {
            if (p.getType() != null) result.add(p.getType());
        }
        result.add(myOutputVariables.getType());
        if (myControlFlow != null) result.add(myControlFlow.getType());
        return result;
    }

    @Override
    @NotNull
    protected List<RsTypeParameter> typeParameters() {
        Set<Ty> paramAndReturnTypes = new HashSet<>();
        for (Ty ty : getParametersAndReturnTypes()) {
            paramAndReturnTypes.addAll(TyUtils.types(ty));
        }
        return getFunction().getTypeParameters().stream()
            .filter(tp -> paramAndReturnTypes.contains(tp.getDeclaredType()))
            .collect(Collectors.toList());
    }

    @NotNull
    private String signature(boolean isOriginal) {
        StringBuilder sb = new StringBuilder();
        if (myVisibilityLevelPublic) sb.append("pub ");
        if (myIsAsync) sb.append("async ");
        if (myIsUnsafe) sb.append("unsafe ");
        String paramsText = myParameters.stream()
            .filter(Parameter::isSelected)
            .map(p -> isOriginal ? p.getOriginalParameterText() : p.getParameterText())
            .collect(Collectors.joining(", "));
        sb.append("fn ").append(myName).append(getTypeParametersText())
            .append("(").append(paramsText).append(")");
        String retType = renderReturnType();
        if (retType != null) sb.append(" -> ").append(retType);
        sb.append(getWhereClausesText());
        return sb.toString();
    }

    @Nullable
    private String renderReturnType() {
        String outputVariablesType = TypeRendering.renderInsertionSafe(myOutputVariables.getType());
        String controlFlowType = myControlFlow != null ? TypeRendering.renderInsertionSafe(myControlFlow.getType()) : null;
        switch (myReturnKind) {
            case VALUE:
                return "()".equals(outputVariablesType) ? null : outputVariablesType;
            case BOOL:
                return "bool";
            case OPTION_CONTROL_FLOW:
                return "Option<" + controlFlowType + ">";
            case OPTION_VALUE:
                return "Option<" + outputVariablesType + ">";
            case RESULT:
                return "Result<" + outputVariablesType + ", " + controlFlowType + ">";
            case TRY_OPERATOR:
                return myControlFlow.getTryOperatorInfo().generateType(outputVariablesType);
            default:
                return null;
        }
    }

    @NotNull
    public String getFunctionText() {
        StringBuilder sb = new StringBuilder();
        sb.append(signature(true));
        PsiElement single = myElements.size() == 1 ? myElements.get(0) : null;
        if (single instanceof RsBlockExpr) {
            sb.append(((RsBlockExpr) single).getBlock().getText());
        } else {
            sb.append("{\n");
            for (Parameter p : myParameters) {
                if (!p.isSelected()) {
                    sb.append("let ").append(p.getName()).append(": ").append(p.getType()).append(" ;\n");
                }
            }
            if (!myElements.isEmpty()) {
                PsiElement first = myElements.get(0);
                PsiElement last = myElements.get(myElements.size() - 1);
                PsiElement current = first;
                while (current != null) {
                    sb.append(current.getText());
                    if (current == last) break;
                    current = current.getNextSibling();
                }
            }
            String outputText = myOutputVariables.getExprText();
            if (outputText != null && !outputText.isEmpty()) {
                sb.append("\n").append(outputText);
            }
            sb.append("\n}");
        }
        return sb.toString();
    }

    @Nullable
    public static RsExtractFunctionConfig create(@NotNull PsiFile file, int start, int end) {
        RsExtractFunctionConfig config = doCreate(file, start, end);
        if (config != null) return config;
        PsiElement lastElement = org.rust.ide.utils.SearchByOffset.findElementAtIgnoreWhitespaceAfter(file, end - 1);
        if (lastElement != null) {
            com.intellij.psi.tree.IElementType elementType = lastElement.getNode().getElementType();
            if (elementType == RsElementTypes.COMMA || elementType == RsElementTypes.SEMICOLON) {
                return doCreate(file, start, lastElement.getTextOffset());
            }
        }
        return null;
    }

    @Nullable
    private static RsExtractFunctionConfig doCreate(@NotNull PsiFile file, int start, int end) {
        PsiElement[] statementsOrExpr = org.rust.ide.utils.SearchByOffset.findStatementsOrExprInRange(file, start, end);
        List<PsiElement> elements = Arrays.asList(statementsOrExpr);
        if (elements.isEmpty()) return null;
        PsiElement first = elements.get(0);
        PsiElement last = elements.get(elements.size() - 1);

        RsFunction fn = RsElementUtil.ancestorStrict(first, RsFunction.class);
        if (fn == null || fn != RsElementUtil.ancestorStrict(last, RsFunction.class)) return null;

        List<RsPatBinding> letBindings = RsElementUtil.descendantsOfType(fn, RsPatBinding.class).stream()
            .filter(b -> b.getTextOffset() <= end)
            .collect(Collectors.toList());

        ImplLookup implLookup = ImplLookup.relativeTo(fn);
        List<Parameter> parameters = new ArrayList<>();
        for (RsPatBinding binding : letBindings) {
            if (binding.getTextOffset() > start) continue;
            List<com.intellij.psi.PsiReference> allRefs = new ArrayList<>(
                ReferencesSearch.search(binding, new LocalSearchScope(fn)).findAll()
            );
            List<com.intellij.psi.PsiReference> targets = allRefs.stream()
                .filter(ref -> ref.getElement().getTextOffset() >= start && ref.getElement().getTextOffset() <= end)
                .collect(Collectors.toList());
            if (targets.isEmpty()) continue;
            boolean isUsedAfterEnd = allRefs.stream().anyMatch(ref -> ref.getElement().getTextOffset() > end);
            parameters.add(Parameter.build(binding, targets, isUsedAfterEnd, implLookup));
        }

        List<RsPatBinding> innerBindings = letBindings.stream()
            .filter(b -> b.getTextOffset() >= start)
            .filter(b -> ReferencesSearch.search(b, new LocalSearchScope(fn)).findAll().stream()
                .anyMatch(ref -> ref.getElement().getTextOffset() > end))
            .collect(Collectors.toList());

        OutputVariables outputVariables;
        if (innerBindings.isEmpty()) {
            if (last instanceof RsExpr) {
                outputVariables = OutputVariables.direct((RsExpr) last);
            } else if (last instanceof RsExprStmt && RsStmtUtil.isTailStmt((RsExprStmt) last)) {
                outputVariables = OutputVariables.direct(((RsExprStmt) last).getExpr());
            } else {
                outputVariables = new OutputVariables(null, TyUnit.INSTANCE);
            }
        } else if (innerBindings.size() == 1) {
            outputVariables = OutputVariables.namedValue(innerBindings.get(0));
        } else {
            outputVariables = OutputVariables.tupleNamedValue(innerBindings);
        }

        RsSelfParameter selfParameter = fn.getSelfParameter();
        if (RsAbstractableUtil.getOwner(fn).isImplOrTrait() && selfParameter != null) {
            boolean used = ReferencesSearch.search(selfParameter, new LocalSearchScope(fn)).findAll().stream()
                .anyMatch(ref -> ref.getElement().getTextOffset() >= start && ref.getElement().getTextOffset() <= end);
            if (used) {
                parameters.add(0, Parameter.self(selfParameter.getText()));
            }
        }

        boolean isAsync = false;
        // Simplified async detection - check if function is async and contains .await
        if (RsFunctionUtil.isAsync(fn)) {
            for (PsiElement element : elements) {
                if (element.getText().contains(".await")) {
                    isAsync = true;
                    break;
                }
            }
        }

        return new RsExtractFunctionConfig(
            fn, elements, outputVariables, null,
            determineReturnKind(outputVariables, null),
            "", false, isAsync, fn.isUnsafe(), parameters
        );
    }

    @NotNull
    private static ReturnKind determineReturnKind(@Nullable OutputVariables returnValue, @Nullable ControlFlow controlFlow) {
        if (controlFlow != null && "?".equals(controlFlow.getText())) return ReturnKind.TRY_OPERATOR;
        boolean hasReturnValue = returnValue != null && !(returnValue.getType() instanceof TyUnit);
        boolean hasControlFlowValue = controlFlow != null && !(controlFlow.getType() instanceof TyUnit);
        if (controlFlow == null) return ReturnKind.VALUE;
        if (!hasReturnValue) return !hasControlFlowValue ? ReturnKind.BOOL : ReturnKind.OPTION_CONTROL_FLOW;
        return !hasControlFlowValue ? ReturnKind.OPTION_VALUE : ReturnKind.RESULT;
    }
}
