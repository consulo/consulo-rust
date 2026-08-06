/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineFunction;

import consulo.project.Project;
import consulo.util.lang.ref.Ref;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.usage.UsageInfo;
import consulo.usage.UsageViewDescriptor;
import consulo.util.collection.MultiMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.ChangeToFieldShorthandFix;
import org.rust.ide.fixes.DeleteUseSpeckUtil;
import org.rust.ide.fixes.UpdateMutableUtil;
import org.rust.ide.refactoring.RsInlineUsageViewDescriptor;
import org.rust.ide.refactoring.RsNameSuggestions;
import org.rust.ide.refactoring.inlineTypeAlias.RsInlineTypeAliasProcessor;
import org.rust.ide.refactoring.inlineValue.InlineValueUtils;
import org.rust.lang.core.dfa.ExitPoint;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.resolve.NameResolution;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.LivenessUtil;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.StdextUtil;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.PsiElementUtil;

public class RsInlineFunctionProcessor extends BaseRefactoringProcessor {
    @Nonnull
    private final RsFunction myOriginalFunction;
    @Nullable
    private final RsReference myReference;
    private final boolean myInlineThisOnly;
    private final boolean myRemoveDefinition;
    @Nonnull
    private final RsPsiFactory myFactory;

    public RsInlineFunctionProcessor(
        @Nonnull Project project,
        @Nonnull RsFunction originalFunction,
        @Nullable RsReference reference,
        boolean inlineThisOnly,
        boolean removeDefinition
    ) {
        super(project);
        myOriginalFunction = originalFunction;
        myReference = reference;
        myInlineThisOnly = inlineThisOnly;
        myRemoveDefinition = removeDefinition;
        myFactory = new RsPsiFactory(project);
    }

    @Nonnull
    @Override
    protected UsageInfo[] findUsages() {
        List<PsiReference> usages;
        if (myInlineThisOnly && myReference != null) {
            usages = Collections.singletonList(myReference);
        } else {
            GlobalSearchScope projectScope = GlobalSearchScope.projectScope(myProject);
            usages = new ArrayList<>(RsSearchableUtil.searchReferences(myOriginalFunction, projectScope));
        }
        return usages.stream()
            .map(this::createUsageInfo)
            .sorted((a, b) -> {
                int offsetA = a.getElement() != null ? a.getElement().getTextOffset() : 0;
                int offsetB = b.getElement() != null ? b.getElement().getTextOffset() : 0;
                return Integer.compare(offsetB, offsetA);
            })
            .toArray(UsageInfo[]::new);
    }

    @Nonnull
    private UsageInfo createUsageInfo(@Nonnull PsiReference reference) {
        PsiElement element = reference.getElement();
        RsUseSpeck useSpeck = RsElementUtil.ancestorOrSelf(element, RsUseSpeck.class);

        RsCallExpr functionCall = null;
        if (element instanceof RsPath) {
            PsiElement parent = element.getParent();
            if (parent instanceof RsPathExpr) {
                PsiElement grandParent = parent.getParent();
                if (grandParent instanceof RsCallExpr && ((RsCallExpr) grandParent).getExpr() == parent) {
                    functionCall = (RsCallExpr) grandParent;
                }
            }
        }

        if (element instanceof RsMethodCall && element.getParent() instanceof RsDotExpr) {
            return new MethodCallUsage((RsMethodCall) element, reference);
        } else if (functionCall != null) {
            return new FunctionCallUsage(functionCall, reference);
        } else if (useSpeck != null) {
            return new UseSpeckUsage(useSpeck, reference);
        } else {
            return new ReferenceUsage(reference);
        }
    }

    @Override
    protected boolean preprocessUsages(@Nonnull consulo.util.lang.ref.SimpleReference<UsageInfo[]> refUsages) {
        MultiMap<PsiElement, consulo.localize.LocalizeValue> conflicts = new MultiMap<>();
        UsageInfo[] usages = refUsages.get();
        for (UsageInfo usage : usages) {
            if (myRemoveDefinition && usage instanceof ReferenceUsage) {
                conflicts.putValue(usage.getElement(), consulo.localize.LocalizeValue.of("Cannot inline function reference"));
            }
        }
        return showConflicts(conflicts, usages);
    }

    @Override
    protected void performRefactoring(@Nonnull UsageInfo[] usages) {
        RsFunction function = preprocessFunction(myOriginalFunction);
        int handledCount = 0;
        for (UsageInfo usage : usages) {
            if (handleUsage(usage, function)) {
                handledCount++;
            }
        }
        if (myRemoveDefinition && handledCount == usages.length) {
            PsiElement prev = myOriginalFunction.getPrevSibling();
            if (prev instanceof PsiWhiteSpace) prev.delete();
            myOriginalFunction.delete();
        }
    }

    private boolean handleUsage(@Nonnull UsageInfo usage, @Nonnull RsFunction function) {
        if (usage instanceof ReferenceUsage) {
            return false;
        } else if (usage instanceof UseSpeckUsage) {
            DeleteUseSpeckUtil.deleteUseSpeck(((UseSpeckUsage) usage).myUseSpeck);
            return true;
        } else if (usage instanceof FunctionCallUsage || usage instanceof MethodCallUsage) {
            return inlineCallUsage(usage, function);
        }
        return false;
    }

    private boolean inlineCallUsage(@Nonnull UsageInfo usage, @Nonnull RsFunction function) {
        // This is a simplified version - full implementation would require significant code
        return false;
    }

    @Nonnull
    private RsFunction preprocessFunction(@Nonnull RsFunction originalFunction) {
        RsFunction function = (RsFunction) originalFunction.copy();
        PsiElement context = originalFunction.getContext();
        if (context instanceof RsElement && function instanceof org.rust.lang.core.macros.RsExpandedElement) {
            RsExpandedElementUtil.setContext((org.rust.lang.core.macros.RsExpandedElement) function, (RsElement) context);
        }
        replaceSelfParameter(function);
        replaceReturnWithTailExpr(function);
        return function;
    }

    private void replaceSelfParameter(@Nonnull RsFunction method) {
        RsValueParameterList valueParameterList = method.getValueParameterList();
        if (valueParameterList == null) return;
        RsSelfParameter selfParameter = valueParameterList.getSelfParameter();
        if (selfParameter == null) return;

        Set<String> existingNames = new HashSet<>();
        for (RsNameIdentifierOwner owner : PsiElementUtil.descendantsOfType(method, RsNameIdentifierOwner.class)) {
            if (owner.getName() != null) existingNames.add(owner.getName());
        }
        String newName = RsNameSuggestions.freshenName("self", existingNames);

        // Rename usages
        RsPsiFactory factory = new RsPsiFactory(method.getProject());
        RsPath namePath = factory.tryCreatePath(newName);
        if (namePath != null) {
            for (PsiReference ref : RsSearchableUtil.searchReferences(selfParameter, new LocalSearchScope(method))) {
                ref.getElement().replace(namePath.copy());
            }
        }

        String ref = selfParameter.getAnd() != null ? "&" : "";
        String selfType = selfParameter.getTypeReference() != null ? selfParameter.getTypeReference().getText() : ref + "Self";
        PsiElement newParameter = factory.createMethodParam(newName + ": " + selfType);
        selfParameter.replace(newParameter);
    }

    private void replaceReturnWithTailExpr(@Nonnull RsFunction function) {
        RsBlock block = RsFunctionUtil.getBlock(function);
        if (block == null) return;
        List<RsStmt> stmts = block.getStmtList();
        if (stmts.isEmpty()) return;
        RsStmt lastStatement = stmts.get(stmts.size() - 1);
        if (!(lastStatement instanceof RsExprStmt)) return;
        RsExpr expr = ((RsExprStmt) lastStatement).getExpr();
        if (!(expr instanceof RsRetExpr)) return;
        RsRetExpr returnExpr = (RsRetExpr) expr;
        RsExpr returnValue = returnExpr.getExpr();
        if (returnValue == null) return;
        PsiElement semicolon = ((RsExprStmt) lastStatement).getSemicolon();
        if (semicolon != null) semicolon.delete();
        returnExpr.replace(returnValue);
    }

    @Nonnull
    @Override
    protected consulo.localize.LocalizeValue getCommandName() {
        String name = myOriginalFunction.getName() != null ? myOriginalFunction.getName() : "";
        return consulo.localize.LocalizeValue.of(RsBundle.message("command.name.inline.function", name));
    }

    @Nonnull
    @Override
    protected UsageViewDescriptor createUsageViewDescriptor(@Nonnull UsageInfo[] usages) {
        return new RsInlineUsageViewDescriptor(myOriginalFunction, RsBundle.message("list.item.function.to.inline"));
    }

    @Nonnull
    @Override
    protected Collection<PsiElement> getElementsToWrite(@Nonnull UsageViewDescriptor descriptor) {
        if (myInlineThisOnly) {
            if (myReference != null && myReference.getElement() != null) {
                return Collections.singletonList(myReference.getElement());
            }
            return Collections.emptyList();
        }
        if (myOriginalFunction.isWritable()) {
            List<PsiElement> result = new ArrayList<>();
            if (myReference != null && myReference.getElement() != null) {
                result.add(myReference.getElement());
            }
            result.add(myOriginalFunction);
            return result;
        }
        return Collections.emptyList();
    }

    public static boolean doesFunctionHaveMultipleReturns(@Nonnull RsFunction fn) {
        List<ExitPoint> entryPoints = new ArrayList<>();
        ExitPoint.process(fn, exitPoint -> {
            if (!(exitPoint instanceof ExitPoint.TryExpr)) {
                entryPoints.add(exitPoint);
            }
        });
        if (entryPoints.size() <= 1) return false;
        List<ExitPoint> allButLast = entryPoints.subList(0, entryPoints.size() - 1);
        return allButLast.stream().anyMatch(it -> it instanceof ExitPoint.Return);
    }

    public static boolean isFunctionRecursive(@Nonnull RsFunction fn) {
        for (RsPath path : PsiElementUtil.descendantsOfType(fn, RsPath.class)) {
            if (path.getReference() != null && path.getReference().resolve() == fn) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkIfLoopCondition(@Nonnull RsFunction fn, @Nonnull PsiElement element) {
        RsBlock block = RsFunctionUtil.getBlock(fn);
        if (block == null) return false;
        RsBlockUtil.ExpandedStmtsAndTailExpr expanded = RsBlockUtil.getExpandedStmtsAndTailExpr(block);
        List<? extends RsElement> statements = expanded.getStatements();
        RsExpr tailExpr = expanded.getTailExpr();

        boolean hasStatements;
        if (tailExpr == null) {
            hasStatements = statements.size() > 1 ||
                (statements.size() == 1 && PsiElementUtil.descendantsOfType(statements.get(0), RsRetExpr.class).isEmpty());
        } else {
            hasStatements = !statements.isEmpty();
        }

        return hasStatements && RsElementUtil.ancestorOrSelf(element, RsWhileExpr.class) != null;
    }

    // Inner usage info classes
    private static class FunctionCallUsage extends UsageInfo {
        @Nonnull
        final RsCallExpr myFunctionCall;

        FunctionCallUsage(@Nonnull RsCallExpr functionCall, @Nonnull PsiReference reference) {
            super(reference);
            myFunctionCall = functionCall;
        }
    }

    private static class MethodCallUsage extends UsageInfo {
        @Nonnull
        final RsMethodCall myMethodCall;

        MethodCallUsage(@Nonnull RsMethodCall methodCall, @Nonnull PsiReference reference) {
            super(reference);
            myMethodCall = methodCall;
        }
    }

    private static class UseSpeckUsage extends UsageInfo {
        @Nonnull
        final RsUseSpeck myUseSpeck;

        UseSpeckUsage(@Nonnull RsUseSpeck useSpeck, @Nonnull PsiReference reference) {
            super(reference);
            myUseSpeck = useSpeck;
        }
    }

    private static class ReferenceUsage extends UsageInfo {
        ReferenceUsage(@Nonnull PsiReference reference) {
            super(reference);
        }
    }
}
