/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.introduceParameter;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.presentation.TypeRendering;
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
import consulo.language.psi.PsiNamedElement;
import org.rust.ide.refactoring.ExtraxtExpressionUtils;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.ext.impl.*;

public final class IntroduceParameterImpl {

    private IntroduceParameterImpl() {
    }

    public static void extractExpression(@Nonnull Editor editor, @Nonnull RsExpr expr) {
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

    private static void replaceExpressionOccurrences(@Nonnull Editor editor, @Nonnull RsExpr expr, @Nonnull RsFunction function) {
        List<RsExpr> occurrences = findOccurrences(function, expr);
        showOccurrencesChooser(editor, expr, occurrences, occurrencesToReplace ->
            replaceExpression(expr.getProject(), editor, function, occurrencesToReplace)
        );
    }

    private static void replaceExpression(
        @Nonnull Project project,
        @Nonnull Editor editor,
        @Nonnull RsFunction function,
        @Nonnull List<RsExpr> exprs
    ) {
        if (exprs.isEmpty()) return;
        if (!CommonRefactoringUtil.checkReadOnlyStatus(project, function)) return;

        RsAbstractableOwner owner = RsAbstractableUtil.getOwner(function);
        boolean replaceForTrait = owner instanceof RsAbstractableOwner.Trait || RsAbstractableOwnerUtil.isTraitImpl(owner);
        ParamIntroducer paramIntroducer = new ParamIntroducer(project, editor);
        paramIntroducer.replaceExpressions(function, exprs, replaceForTrait);
    }

    @Nonnull
    private static List<RsFunction> findEnclosingFunctions(@Nonnull RsExpr expr) {
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
        @Nonnull
        private final Project project;
        @Nonnull
        private final Editor editor;
        @Nonnull
        private final RsPsiFactory psiFactory;

        ParamIntroducer(@Nonnull Project project, @Nonnull Editor editor) {
            this.project = project;
            this.editor = editor;
            this.psiFactory = new RsPsiFactory(project);
        }

        void replaceExpressions(@Nonnull RsFunction function, @Nonnull List<RsExpr> exprs, boolean replaceForTrait) {
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
                        (consulo.language.psi.PsiNamedElement) newParameter, editor, project,
                        RsBundle.message("command.name.choose.parameter")
                    ).performInplaceRefactoring(suggestedNames.getAll());
                }
            });
        }

        private void appendNewArgument(@Nonnull List<PsiElement> usages, @Nonnull RsExpr value) {
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

        @Nonnull
        private Iterable<PsiElement> findFunctionUsages(@Nonnull RsFunction chosenFunction) {
            GlobalSearchScope projectScope = GlobalSearchScope.projectScope(chosenFunction.getProject());
            Collection<PsiReference> functionUsages = ReferencesSearch.search(chosenFunction, projectScope, false).findAll();
            return functionUsages.stream().map(PsiReference::getElement).collect(Collectors.toList());
        }

        @Nonnull
        private List<RsFunction> getTraitAndImpls(@Nonnull RsFunction traitFunction) {
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

        @Nonnull
        private Iterable<PsiElement> findFunctionUsagesWithImpl(@Nonnull RsFunction traitFunction) {
            List<PsiElement> result = new ArrayList<>();
            for (RsFunction fn : getTraitAndImpls(traitFunction)) {
                for (PsiElement usage : findFunctionUsages(fn)) {
                    result.add(usage);
                }
            }
            return result;
        }

        @Nullable
        private RsFunction findDescendantFunction(@Nonnull PsiReference traitImplRef, @Nonnull RsFunction functionToSearch) {
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

        @Nonnull
        private RsValueParameter createParam(@Nonnull String name, @Nonnull RsTypeReference typeRef) {
            return createParamList(name, typeRef).getValueParameterList().get(0);
        }

        @Nonnull
        private RsValueParameterList createParamList(@Nonnull String name, @Nonnull RsTypeReference typeRef) {
            return psiFactory.createSimpleValueParameterList(name, typeRef);
        }

        private void introduceValueArgument(@Nonnull RsExpr value, @Nonnull RsValueArgumentList argumentList) {
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
        private PsiElement introduceParam(@Nonnull RsFunction func, @Nonnull String name, @Nonnull RsTypeReference typeRef) {
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
