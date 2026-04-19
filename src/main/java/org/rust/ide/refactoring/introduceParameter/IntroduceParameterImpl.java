/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.introduceParameter;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.presentation.TypeRendering;
import org.rust.ide.refactoring.RsInPlaceVariableIntroducer;
import org.rust.ide.refactoring.RsNameSuggestions;
import org.rust.ide.refactoring.SuggestedNames;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.rust.ide.refactoring.ExtraxtExpressionUtils.findOccurrences;
import static org.rust.ide.refactoring.ExtraxtExpressionUiUtils.showOccurrencesChooser;

public final class IntroduceParameterImpl {

    private IntroduceParameterImpl() {
    }

    public static void extractExpression(@NotNull Editor editor, @NotNull RsExpr expr) {
        Project project = expr.getProject();
        List<RsFunction> enclosingFunctions = findEnclosingFunctions(expr);
        switch (enclosingFunctions.size()) {
            case 0: {
                String message = RsBundle.message("dialog.message.no.suitable.function.to.extract.parameter.found");
                String title = RefactoringBundle.message("introduce.parameter.title");
                String helpId = "refactoring.extractParameter";
                CommonRefactoringUtil.showErrorHint(project, editor, message, title, helpId);
                break;
            }
            case 1:
                replaceExpressionOccurrences(editor, expr, enclosingFunctions.get(0));
                break;
            default:
                IntroduceParameterUiUtils.showEnclosingFunctionsChooser(editor, enclosingFunctions, chosenFunction ->
                    replaceExpressionOccurrences(editor, expr, chosenFunction)
                );
                break;
        }
    }

    private static void replaceExpressionOccurrences(@NotNull Editor editor, @NotNull RsExpr expr, @NotNull RsFunction function) {
        List<RsExpr> occurrences = findOccurrences(function, expr);
        showOccurrencesChooser(editor, expr, occurrences, occurrencesToReplace ->
            replaceExpression(expr.getProject(), editor, function, occurrencesToReplace)
        );
    }

    private static void replaceExpression(
        @NotNull Project project,
        @NotNull Editor editor,
        @NotNull RsFunction function,
        @NotNull List<RsExpr> exprs
    ) {
        if (exprs.isEmpty()) return;
        if (!CommonRefactoringUtil.checkReadOnlyStatus(project, function)) return;

        RsAbstractableOwner owner = RsAbstractableUtil.getOwner(function);
        boolean replaceForTrait = owner instanceof RsAbstractableOwner.Trait || RsAbstractableOwnerUtil.isTraitImpl(owner);
        ParamIntroducer paramIntroducer = new ParamIntroducer(project, editor);
        paramIntroducer.replaceExpressions(function, exprs, replaceForTrait);
    }

    @NotNull
    private static List<RsFunction> findEnclosingFunctions(@NotNull RsExpr expr) {
        List<RsFunction> result = new ArrayList<>();
        PsiElement current = expr;
        while (current != null) {
            if (current instanceof RsFunction) {
                result.add((RsFunction) current);
            }
            current = current.getParent();
        }
        return result;
    }

    private static class ParamIntroducer {
        @NotNull
        private final Project project;
        @NotNull
        private final Editor editor;
        @NotNull
        private final RsPsiFactory psiFactory;

        ParamIntroducer(@NotNull Project project, @NotNull Editor editor) {
            this.project = project;
            this.editor = editor;
            this.psiFactory = new RsPsiFactory(project);
        }

        void replaceExpressions(@NotNull RsFunction function, @NotNull List<RsExpr> exprs, boolean replaceForTrait) {
            if (exprs.isEmpty()) return;
            RsExpr expr = exprs.get(0);
            RsTypeReference typeRef = psiFactory.tryCreateType(
                TypeRendering.renderInsertionSafe(RsTypesUtil.getType(expr))
            );
            if (typeRef == null) return;

            SuggestedNames suggestedNames = RsNameSuggestions.suggestedNames(expr);

            RsFunction traitFunction = function.getSuperItem() instanceof RsFunction
                ? (RsFunction) function.getSuperItem()
                : function;

            Iterable<PsiElement> functionUsages;
            if (replaceForTrait) {
                functionUsages = findFunctionUsagesWithImpl(traitFunction);
            } else {
                functionUsages = findFunctionUsages(function);
            }

            List<PsiElement> functionUsagesList = new ArrayList<>();
            functionUsages.forEach(functionUsagesList::add);

            org.rust.openapiext.OpenApiUtil.runWriteCommandAction(project, RefactoringBundle.message("introduce.parameter.title"), () -> {
                appendNewArgument(functionUsagesList, expr);
                if (replaceForTrait) {
                    for (RsFunction impl : getTraitAndImpls(traitFunction)) {
                        if (impl != function) {
                            introduceParam(impl, suggestedNames.getDefault(), typeRef);
                        }
                    }
                }
                PsiElement newParam = introduceParam(function, suggestedNames.getDefault(), typeRef);
                RsExpr name = psiFactory.createExpression(suggestedNames.getDefault());
                for (RsExpr e : exprs) {
                    e.replace(name);
                }
                PsiElement newParameter = org.rust.ide.refactoring.ExtraxtExpressionUtils.moveEditorToNameElement(editor, newParam);

                if (newParameter != null) {
                    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
                    new RsInPlaceVariableIntroducer(
                        (com.intellij.psi.PsiNamedElement) newParameter, editor, project,
                        RsBundle.message("command.name.choose.parameter")
                    ).performInplaceRefactoring(suggestedNames.getAll());
                }
            });
        }

        private void appendNewArgument(@NotNull List<PsiElement> usages, @NotNull RsExpr value) {
            for (PsiElement it : usages) {
                if (it instanceof RsPath) {
                    RsCallExpr callExpr = RsElementUtil.ancestorOrSelf(it, RsCallExpr.class);
                    if (callExpr == null) return;
                    introduceValueArgument(value, callExpr.getValueArgumentList());
                } else if (it instanceof RsMethodCall) {
                    introduceValueArgument(value, ((RsMethodCall) it).getValueArgumentList());
                }
            }
        }

        @NotNull
        private Iterable<PsiElement> findFunctionUsages(@NotNull RsFunction chosenFunction) {
            GlobalSearchScope projectScope = GlobalSearchScope.projectScope(chosenFunction.getProject());
            Collection<PsiReference> functionUsages = ReferencesSearch.search(chosenFunction, projectScope, false).findAll();
            return functionUsages.stream().map(PsiReference::getElement).collect(Collectors.toList());
        }

        @NotNull
        private List<RsFunction> getTraitAndImpls(@NotNull RsFunction traitFunction) {
            RsAbstractableOwner owner = RsAbstractableUtil.getOwner(traitFunction);
            RsTraitItem trait = owner instanceof RsAbstractableOwner.Trait
                ? ((RsAbstractableOwner.Trait) owner).getTrait()
                : null;
            if (trait == null) return Collections.emptyList();

            GlobalSearchScope projectScope = GlobalSearchScope.projectScope(traitFunction.getProject());
            Collection<PsiReference> traitUsages = ReferencesSearch.search(trait, projectScope, false).findAll();
            List<RsFunction> refs = traitUsages.stream()
                .map(ref -> findDescendantFunction(ref, traitFunction))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            refs.add(traitFunction);
            return refs;
        }

        @NotNull
        private Iterable<PsiElement> findFunctionUsagesWithImpl(@NotNull RsFunction traitFunction) {
            List<PsiElement> result = new ArrayList<>();
            for (RsFunction fn : getTraitAndImpls(traitFunction)) {
                for (PsiElement usage : findFunctionUsages(fn)) {
                    result.add(usage);
                }
            }
            return result;
        }

        @Nullable
        private RsFunction findDescendantFunction(@NotNull PsiReference traitImplRef, @NotNull RsFunction functionToSearch) {
            PsiElement element = traitImplRef.getElement();
            if (element.getParent() == null || !(element.getParent().getParent() instanceof RsImplItem)) return null;
            RsImplItem traitImpl = (RsImplItem) element.getParent().getParent();
            List<RsFunction> functions = RsElementUtil.descendantsOfType(traitImpl, RsFunction.class);
            for (RsFunction fn : functions) {
                if (fn.getName() != null && fn.getName().equals(functionToSearch.getName())) {
                    return fn;
                }
            }
            return null;
        }

        @NotNull
        private RsValueParameter createParam(@NotNull String name, @NotNull RsTypeReference typeRef) {
            return createParamList(name, typeRef).getValueParameterList().get(0);
        }

        @NotNull
        private RsValueParameterList createParamList(@NotNull String name, @NotNull RsTypeReference typeRef) {
            return psiFactory.createSimpleValueParameterList(name, typeRef);
        }

        private void introduceValueArgument(@NotNull RsExpr value, @NotNull RsValueArgumentList argumentList) {
            List<RsExpr> args = argumentList.getExprList();
            if (args.isEmpty()) {
                argumentList.addAfter(value, argumentList.getFirstChild());
            } else {
                argumentList.addAfter(value, args.get(args.size() - 1));
                PsiElement comma = psiFactory.createComma();
                argumentList.addAfter(comma, args.get(args.size() - 1));
            }
        }

        @Nullable
        private PsiElement introduceParam(@NotNull RsFunction func, @NotNull String name, @NotNull RsTypeReference typeRef) {
            List<RsValueParameter> params = func.getRawValueParameters();
            RsValueParameterList parent = func.getValueParameterList();
            if (parent == null) return null;
            RsValueParameter newParam = createParam(name, typeRef);
            if (params.isEmpty()) {
                if (parent.getSelfParameter() != null) {
                    PsiElement newElem = parent.addAfter(newParam, parent.getSelfParameter());
                    PsiElement comma = psiFactory.createComma();
                    parent.addAfter(comma, parent.getSelfParameter());
                    return newElem;
                } else {
                    return parent.addAfter(newParam, parent.getFirstChild());
                }
            } else {
                PsiElement newElem = parent.addAfter(newParam, params.get(params.size() - 1));
                PsiElement comma = psiFactory.createComma();
                parent.addAfter(comma, params.get(params.size() - 1));
                return newElem;
            }
        }
    }
}
