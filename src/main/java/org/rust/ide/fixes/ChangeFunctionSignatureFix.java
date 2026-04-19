/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.PriorityAction;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.annotator.FunctionCallContextUtil;
import org.rust.ide.presentation.TypeRendering;
import org.rust.ide.refactoring.RsSuggestedNamesUtil;
import org.rust.ide.refactoring.changeSignature.*;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsValueParameterListUtil;
// import org.rust.lang.core.types.ImplLookupUtil; // placeholder
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnknown;
import org.rust.lang.utils.RsDiagnostic;
import org.rust.stdext.CollectionExtUtil;
import org.rust.stdext.NumberExtUtil;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.ide.refactoring.changeSignature.ChangeSignatureImpl;
import org.rust.lang.core.psi.ext.RsValueParameterUtil;
import org.rust.lang.core.types.ImplLookupUtil;

/**
 * This fix can add, remove or change the type of parameters of a function.
 */
public class ChangeFunctionSignatureFix extends RsQuickFixBase<RsValueArgumentList> implements PriorityAction {

    private final Signature signature;
    private final PriorityAction.Priority priority;
    @IntentionName
    private final String fixText;

    private ChangeFunctionSignatureFix(
        @NotNull RsValueArgumentList argumentList,
        @NotNull RsFunction function,
        @NotNull Signature signature,
        @Nullable PriorityAction.Priority priority
    ) {
        super(argumentList);
        this.signature = signature;
        this.priority = priority;

        String callableType = function.isMethod()
            ? RsBundle.message("intention.name.method")
            : RsBundle.message("intention.name.function");
        String name = function.getName();
        List<IndexedAction> changes = new ArrayList<>();
        List<SignatureAction> actions = signature.actions;
        for (int i = 0; i < actions.size(); i++) {
            SignatureAction action = actions.get(i);
            if (!(action instanceof SignatureAction.KeepParameter)) {
                changes.add(new IndexedAction(i, action));
            }
        }
        List<RsExpr> arguments = getEffectiveArguments(argumentList, function);

        if (changes.size() == 1) {
            IndexedAction indexedAction = changes.get(0);
            int index = indexedAction.index;
            SignatureAction action = indexedAction.action;
            int indexOffset = index + 1;
            String ordinal = RsBundle.message("intention.name.", indexOffset, org.rust.stdext.Utils.numberSuffix(indexOffset));
            List<RsValueParameter> valueParams = RsValueParameterListUtil.getValueParameterList(function);
            RsValueParameter param = index < valueParams.size() ? valueParams.get(index) : null;
            String parameterFormat = param != null && param.getPat() != null ? formatParameter(param.getPat(), index) : null;

            if (action instanceof SignatureAction.ChangeParameterType) {
                RsExpr argument = arguments.get(((SignatureAction.ChangeParameterType) action).argumentIndex);
                this.fixText = RsBundle.message("intention.name.change.type.to",
                    parameterFormat != null ? parameterFormat : "",
                    callableType,
                    name != null ? name : "",
                    renderType(RsTypesUtil.getType(argument)));
            } else if (action instanceof SignatureAction.InsertArgument) {
                RsExpr argument = arguments.get(((SignatureAction.InsertArgument) action).argumentIndex);
                this.fixText = RsBundle.message("intention.name.add.as.parameter.to",
                    renderType(RsTypesUtil.getType(argument)),
                    ordinal,
                    callableType,
                    name != null ? name : "");
            } else if (action instanceof SignatureAction.RemoveParameter) {
                this.fixText = RsBundle.message("intention.name.remove.from",
                    parameterFormat != null ? parameterFormat : "",
                    callableType,
                    name != null ? name : "");
            } else {
                throw new IllegalStateException("unreachable");
            }
        } else {
            List<SignatureAction> nonRemoveActions = signature.actions.stream()
                .filter(a -> !(a instanceof SignatureAction.RemoveParameter))
                .collect(Collectors.toList());
            List<RsValueParameter> valueParams = RsValueParameterListUtil.getValueParameterList(function);
            StringBuilder signatureText = new StringBuilder();
            for (int i = 0; i < nonRemoveActions.size(); i++) {
                if (i > 0) signatureText.append(", ");
                SignatureAction action = nonRemoveActions.get(i);
                if (action instanceof SignatureAction.InsertArgument) {
                    signatureText.append(RsBundle.message("intention.name.b.b2",
                        renderType(RsTypesUtil.getType(arguments.get(((SignatureAction.InsertArgument) action).argumentIndex)))));
                } else if (action instanceof SignatureAction.KeepParameter) {
                    int paramIndex = ((SignatureAction.KeepParameter) action).parameterIndex;
                    RsTypeReference typeRef = paramIndex < valueParams.size() ? valueParams.get(paramIndex).getTypeReference() : null;
                    Ty ty = typeRef != null ? RsTypesUtil.getNormType(typeRef) : TyUnknown.INSTANCE;
                    signatureText.append(renderType(ty));
                } else if (action instanceof SignatureAction.ChangeParameterType) {
                    signatureText.append(RsBundle.message("intention.name.b.b",
                        renderType(RsTypesUtil.getType(arguments.get(((SignatureAction.ChangeParameterType) action).argumentIndex)))));
                } else {
                    throw new IllegalStateException("unreachable");
                }
            }
            this.fixText = RsBundle.message("intention.name.html.change.signature.to.html",
                name != null ? name : "", signatureText.toString());
        }
    }

    @NotNull
    @Override
    public String getText() {
        return fixText;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.change.function.signature");
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @NotNull
    @Override
    public PriorityAction.Priority getPriority() {
        return priority != null ? priority : PriorityAction.Priority.NORMAL;
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsValueArgumentList element) {
        var context = FunctionCallContextUtil.getFunctionCallContext(element);
        if (context == null) return;
        RsFunction function = context.getFunction();
        if (function == null) return;

        Set<String> usedNames = new HashSet<>();
        getExistingParameterNames(usedNames, function);

        RsChangeFunctionSignatureConfig config = RsChangeFunctionSignatureConfig.create(function);
        RsPsiFactory factory = new RsPsiFactory(project);

        List<RsExpr> arguments = getEffectiveArguments(element, function);
        int parameterIndex = 0;
        List<SignatureAction> actions = signature.actions;
        for (int index = 0; index < actions.size(); index++) {
            SignatureAction item = actions.get(index);
            if (item instanceof SignatureAction.InsertArgument) {
                int argIndex = ((SignatureAction.InsertArgument) item).argumentIndex;
                RsExpr expr = argIndex < arguments.size() ? arguments.get(argIndex) : null;
                if (expr == null) continue;
                String typeText = renderType(RsTypesUtil.getType(expr));
                RsTypeReference typeReference = factory.tryCreateType(typeText);
                if (typeReference == null) typeReference = factory.createType("()");
                String sugName = suggestName(expr);
                String paramName = generateName(sugName, usedNames);

                config.getParameters().add(index, new Parameter(factory, paramName, ParameterProperty.fromItem(typeReference)));
                config.getAdditionalTypesToImport().add(RsTypesUtil.getType(expr));
                parameterIndex++;
            } else if (item instanceof SignatureAction.RemoveParameter) {
                config.getParameters().remove(parameterIndex);
            } else if (item instanceof SignatureAction.ChangeParameterType) {
                Parameter original = config.getParameters().get(parameterIndex);
                int argIndex = ((SignatureAction.ChangeParameterType) item).argumentIndex;
                RsExpr expr = argIndex < arguments.size() ? arguments.get(argIndex) : null;
                if (expr == null) continue;
                String typeText = renderType(RsTypesUtil.getType(expr));
                RsTypeReference typeReference = factory.tryCreateType(typeText);
                if (typeReference == null) typeReference = factory.createType("()");
                config.getParameters().set(parameterIndex, new Parameter(
                    factory,
                    original.getPatText(),
                    ParameterProperty.fromItem(typeReference),
                    original.getIndex()
                ));
                config.getAdditionalTypesToImport().add(RsTypesUtil.getType(expr));
                parameterIndex++;
            } else {
                parameterIndex++;
            }
        }

        org.rust.ide.refactoring.changeSignature.RsChangeSignatureProcessor.runChangeSignatureRefactoring(config);
    }

    @NotNull
    public static List<ChangeFunctionSignatureFix> createIfCompatible(
        @NotNull RsValueArgumentList arguments,
        @NotNull RsFunction function
    ) {
        if (!RsChangeSignatureHandler.isChangeSignatureAvailable(function)) return Collections.emptyList();
        var context = FunctionCallContextUtil.getFunctionCallContext(arguments);
        if (context == null) return Collections.emptyList();

        var diagnostics = ExtensionsUtil.getInference(arguments) != null
            ? ExtensionsUtil.getInference(arguments).getDiagnostics()
            : Collections.<RsDiagnostic>emptyList();

        Set<PsiElement> errorArguments = new HashSet<>();
        for (RsDiagnostic d : diagnostics) {
            if (d instanceof RsDiagnostic.TypeError && arguments.getExprList().contains(d.getElement())) {
                errorArguments.add(d.getElement());
            }
        }

        Map<Signature, ChangeFunctionSignatureFix> signatureToFix = new LinkedHashMap<>();

        int argumentCount = arguments.getExprList().size();
        if (context.getExpectedParameterCount() < argumentCount) {
            int effectiveArgumentCount = getEffectiveArguments(arguments, function).size();

            PriorityAction.Priority[] priorities = {PriorityAction.Priority.HIGH, PriorityAction.Priority.NORMAL};
            ArgumentScanDirection[] directions = {ArgumentScanDirection.FORWARD, ArgumentScanDirection.BACKWARD};

            for (int i = 0; i < 2; i++) {
                Signature sig = calculateSignatureWithInsertion(function, arguments, directions[i]);
                if (sig.getActualParameterCount() != effectiveArgumentCount) {
                    continue;
                }
                signatureToFix.put(sig, new ChangeFunctionSignatureFix(arguments, function, sig, priorities[i]));
            }
        }

        if (signatureToFix.isEmpty()) {
            Signature simpleSignature = calculateSignatureWithoutInsertion(function, arguments, errorArguments);
            boolean allKeep = simpleSignature.actions.stream().allMatch(a -> a instanceof SignatureAction.KeepParameter);
            if (!allKeep) {
                signatureToFix.put(simpleSignature, new ChangeFunctionSignatureFix(arguments, function, simpleSignature, null));
            }
        }

        return new ArrayList<>(signatureToFix.values());
    }

    // --- Inner types ---

    public enum ArgumentScanDirection {
        FORWARD,
        BACKWARD;

        public <T> List<T> map(List<T> list) {
            if (this == BACKWARD) {
                List<T> reversed = new ArrayList<>(list);
                Collections.reverse(reversed);
                return reversed;
            }
            return list;
        }
    }

    // --- Private helpers ---

    private static String formatParameter(@NotNull RsPat pat, int index) {
        if (pat instanceof RsPatIdent) {
            String name = ((RsPatIdent) pat).getPatBinding().getName();
            return "parameter `" + name + "`";
        } else {
            int num = index + 1;
            return "`" + num + org.rust.stdext.Utils.numberSuffix(num) + "` parameter";
        }
    }

    private static String suggestName(@NotNull RsExpr expr) {
        if (expr instanceof RsPathExpr) {
            PsiElement reference = ((RsPathExpr) expr).getPath().getReference() != null
                ? ((RsPathExpr) expr).getPath().getReference().resolve()
                : null;
            if (reference instanceof RsPatBinding) {
                String name = ((RsPatBinding) reference).getName();
                if (name != null) {
                    return name;
                }
            }
        }
        return org.rust.ide.refactoring.RsNameSuggestions.suggestedNames(expr).getDefault();
    }

    private static String generateName(@NotNull String defaultName, @NotNull Set<String> usedNames) {
        String name = defaultName;
        int index = 0;
        while (usedNames.contains(name)) {
            name = defaultName + index;
            index++;
        }
        usedNames.add(name);
        return name;
    }

    private static Signature calculateSignatureWithInsertion(
        @NotNull RsFunction function,
        @NotNull RsValueArgumentList argumentList,
        @NotNull ArgumentScanDirection direction
    ) {
        List<RsValueParameter> parameters = RsValueParameterListUtil.getValueParameterList(function);
        List<RsExpr> arguments = getEffectiveArguments(argumentList, function);

        if (parameters.isEmpty()) {
            List<SignatureAction> actions = new ArrayList<>();
            for (int i = 0; i < arguments.size(); i++) {
                actions.add(new SignatureAction.InsertArgument(i));
            }
            return new Signature(actions);
        }

        var implLookup = org.rust.lang.core.types.RsTypesUtil.getImplLookup(function);
        var ctx = implLookup.getCtx();

        List<RsValueParameter> dirParams = direction.map(parameters);
        List<RsExpr> dirArgs = direction.map(arguments);

        Iterator<RsValueParameter> paramIterator = dirParams.iterator();
        Iterator<RsExpr> argIterator = dirArgs.iterator();

        RsValueParameter currentParam = paramIterator.hasNext() ? paramIterator.next() : null;
        RsExpr currentArg = argIterator.hasNext() ? argIterator.next() : null;

        List<SignatureAction> insertions = new ArrayList<>();
        while (true) {
            if (currentArg == null) break;

            if (currentParam != null) {
                RsTypeReference typeRef = currentParam.getTypeReference();
                Ty paramTy = typeRef != null ? RsTypesUtil.normType(typeRef, implLookup) : TyUnknown.INSTANCE;
                if (ctx.combineTypes(paramTy, RsTypesUtil.getType(currentArg)).isOk()) {
                    insertions.add(new SignatureAction.KeepParameter(parameters.indexOf(currentParam)));
                    currentParam = paramIterator.hasNext() ? paramIterator.next() : null;
                    currentArg = argIterator.hasNext() ? argIterator.next() : null;
                } else {
                    insertions.add(new SignatureAction.InsertArgument(arguments.indexOf(currentArg)));
                    currentArg = argIterator.hasNext() ? argIterator.next() : null;
                }
            } else {
                insertions.add(new SignatureAction.InsertArgument(arguments.indexOf(currentArg)));
                currentArg = argIterator.hasNext() ? argIterator.next() : null;
            }
        }

        while (currentParam != null) {
            insertions.add(new SignatureAction.KeepParameter(parameters.indexOf(currentParam)));
            currentParam = paramIterator.hasNext() ? paramIterator.next() : null;
        }

        return new Signature(direction.map(insertions));
    }

    private static Signature calculateSignatureWithoutInsertion(
        @NotNull RsFunction function,
        @NotNull RsValueArgumentList args,
        @NotNull Set<PsiElement> errorArguments
    ) {
        List<SignatureAction> actions = new ArrayList<>();
        List<RsValueParameter> parameters = RsValueParameterListUtil.getValueParameterList(function);
        List<RsExpr> arguments = getEffectiveArguments(args, function);

        int length = Math.max(arguments.size(), parameters.size());
        for (int index = 0; index < length; index++) {
            SignatureAction action;
            if (index >= arguments.size()) {
                action = SignatureAction.RemoveParameter.INSTANCE;
            } else if (index >= parameters.size()) {
                action = new SignatureAction.InsertArgument(index);
            } else if (errorArguments.contains(arguments.get(index))) {
                action = new SignatureAction.ChangeParameterType(index);
            } else {
                action = new SignatureAction.KeepParameter(index);
            }
            actions.add(action);
        }

        return new Signature(actions);
    }

    private static List<RsExpr> getEffectiveArguments(@NotNull RsValueArgumentList args, @NotNull RsFunction function) {
        List<RsExpr> arguments = args.getExprList();
        boolean isUFCS = function.isMethod() && args.getParent() instanceof RsCallExpr;
        if (isUFCS && arguments.size() > 0) {
            return arguments.subList(1, arguments.size());
        } else {
            return arguments;
        }
    }

    private static String renderType(@NotNull Ty ty) {
        return TypeRendering.renderInsertionSafe(ty);
    }

    private static void getExistingParameterNames(@NotNull Set<String> usedNames, @NotNull RsFunction function) {
        RsVisitor visitor = new RsVisitor() {
            @Override
            public void visitPatBinding(@NotNull RsPatBinding o) {
                String name = o.getIdentifier().getText();
                usedNames.add(name);
            }
        };
        if (function.getValueParameterList() != null) {
            function.getValueParameterList().acceptChildren(visitor);
        }
    }

    // --- Inner data classes ---

    private static class IndexedAction {
        final int index;
        final SignatureAction action;

        IndexedAction(int index, SignatureAction action) {
            this.index = index;
            this.action = action;
        }
    }

    private static class Signature {
        final List<SignatureAction> actions;

        Signature(@NotNull List<SignatureAction> actions) {
            this.actions = actions;
        }

        int getActualParameterCount() {
            int count = 0;
            for (SignatureAction action : actions) {
                if (action instanceof SignatureAction.RemoveParameter) {
                    count--;
                } else {
                    count++;
                }
            }
            return count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Signature)) return false;
            return actions.equals(((Signature) o).actions);
        }

        @Override
        public int hashCode() {
            return actions.hashCode();
        }
    }

    private static abstract class SignatureAction {
        static class KeepParameter extends SignatureAction {
            final int parameterIndex;

            KeepParameter(int parameterIndex) {
                this.parameterIndex = parameterIndex;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof KeepParameter)) return false;
                return parameterIndex == ((KeepParameter) o).parameterIndex;
            }

            @Override
            public int hashCode() {
                return parameterIndex;
            }
        }

        static class InsertArgument extends SignatureAction {
            final int argumentIndex;

            InsertArgument(int argumentIndex) {
                this.argumentIndex = argumentIndex;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof InsertArgument)) return false;
                return argumentIndex == ((InsertArgument) o).argumentIndex;
            }

            @Override
            public int hashCode() {
                return argumentIndex;
            }
        }

        static class RemoveParameter extends SignatureAction {
            static final RemoveParameter INSTANCE = new RemoveParameter();

            private RemoveParameter() {}

            @Override
            public boolean equals(Object o) {
                return o instanceof RemoveParameter;
            }

            @Override
            public int hashCode() {
                return 0;
            }
        }

        static class ChangeParameterType extends SignatureAction {
            final int argumentIndex;

            ChangeParameterType(int argumentIndex) {
                this.argumentIndex = argumentIndex;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof ChangeParameterType)) return false;
                return argumentIndex == ((ChangeParameterType) o).argumentIndex;
            }

            @Override
            public int hashCode() {
                return argumentIndex;
            }
        }
    }

    private static boolean isMethod(@NotNull RsFunction function) {
        return RsFunctionUtil.isMethod(function);
    }
}
