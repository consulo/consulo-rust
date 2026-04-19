/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineFunction;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    @NotNull
    private final RsFunction myOriginalFunction;
    @Nullable
    private final RsReference myReference;
    private final boolean myInlineThisOnly;
    private final boolean myRemoveDefinition;
    @NotNull
    private final RsPsiFactory myFactory;

    public RsInlineFunctionProcessor(
        @NotNull Project project,
        @NotNull RsFunction originalFunction,
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

    @NotNull
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

    @NotNull
    private UsageInfo createUsageInfo(@NotNull PsiReference reference) {
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
    protected boolean preprocessUsages(@NotNull Ref<UsageInfo[]> refUsages) {
        MultiMap<PsiElement, String> conflicts = new MultiMap<>();
        UsageInfo[] usages = refUsages.get();
        for (UsageInfo usage : usages) {
            if (myRemoveDefinition && usage instanceof ReferenceUsage) {
                conflicts.putValue(usage.getElement(), "Cannot inline function reference");
            }
        }
        return showConflicts(conflicts, usages);
    }

    @Override
    protected void performRefactoring(@NotNull UsageInfo[] usages) {
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

    private boolean handleUsage(@NotNull UsageInfo usage, @NotNull RsFunction function) {
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

    private boolean inlineCallUsage(@NotNull UsageInfo usage, @NotNull RsFunction function) {
        // This is a simplified version - full implementation would require significant code
        return false;
    }

    @NotNull
    private RsFunction preprocessFunction(@NotNull RsFunction originalFunction) {
        RsFunction function = (RsFunction) originalFunction.copy();
        PsiElement context = originalFunction.getContext();
        if (context instanceof RsElement && function instanceof org.rust.lang.core.macros.RsExpandedElement) {
            RsExpandedElementUtil.setContext((org.rust.lang.core.macros.RsExpandedElement) function, (RsElement) context);
        }
        replaceSelfParameter(function);
        replaceReturnWithTailExpr(function);
        return function;
    }

    private void replaceSelfParameter(@NotNull RsFunction method) {
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

    private void replaceReturnWithTailExpr(@NotNull RsFunction function) {
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

    @NotNull
    @Override
    protected String getCommandName() {
        String name = myOriginalFunction.getName() != null ? myOriginalFunction.getName() : "";
        return RsBundle.message("command.name.inline.function", name);
    }

    @NotNull
    @Override
    protected UsageViewDescriptor createUsageViewDescriptor(@NotNull UsageInfo[] usages) {
        return new RsInlineUsageViewDescriptor(myOriginalFunction, RsBundle.message("list.item.function.to.inline"));
    }

    @NotNull
    @Override
    protected Collection<PsiElement> getElementsToWrite(@NotNull UsageViewDescriptor descriptor) {
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

    public static boolean doesFunctionHaveMultipleReturns(@NotNull RsFunction fn) {
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

    public static boolean isFunctionRecursive(@NotNull RsFunction fn) {
        for (RsPath path : PsiElementUtil.descendantsOfType(fn, RsPath.class)) {
            if (path.getReference() != null && path.getReference().resolve() == fn) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkIfLoopCondition(@NotNull RsFunction fn, @NotNull PsiElement element) {
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
        @NotNull
        final RsCallExpr myFunctionCall;

        FunctionCallUsage(@NotNull RsCallExpr functionCall, @NotNull PsiReference reference) {
            super(reference);
            myFunctionCall = functionCall;
        }
    }

    private static class MethodCallUsage extends UsageInfo {
        @NotNull
        final RsMethodCall myMethodCall;

        MethodCallUsage(@NotNull RsMethodCall methodCall, @NotNull PsiReference reference) {
            super(reference);
            myMethodCall = methodCall;
        }
    }

    private static class UseSpeckUsage extends UsageInfo {
        @NotNull
        final RsUseSpeck myUseSpeck;

        UseSpeckUsage(@NotNull RsUseSpeck useSpeck, @NotNull PsiReference reference) {
            super(reference);
            myUseSpeck = useSpeck;
        }
    }

    private static class ReferenceUsage extends UsageInfo {
        ReferenceUsage(@NotNull PsiReference reference) {
            super(reference);
        }
    }
}
