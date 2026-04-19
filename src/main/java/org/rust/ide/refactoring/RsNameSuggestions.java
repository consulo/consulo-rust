/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.refactoring.introduceVariable.IntroduceVariableTestmarks;
import org.rust.ide.utils.CallInfo;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.ty.*;
import org.rust.openapiext.Testmark;

import java.util.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.BoundElement;

public final class RsNameSuggestions {

    public static final int FRESHEN_LIMIT = 1000;

    private static final List<String> USELESS_NAMES = Arrays.asList("new", "default");

    private RsNameSuggestions() {
    }

    @NotNull
    public static SuggestedNames suggestedNames(@NotNull RsExpr expr) {
        LinkedHashSet<String> names = suggestedNamesForTy(RsTypesUtil.getType(expr));

        PsiElement parent = expr.getParent();
        if (parent instanceof RsValueArgumentList) {
            RsCallExpr callExpr = RsElementUtil.ancestorStrict(parent, RsCallExpr.class);
            if (callExpr != null) {
                CallInfo call = CallInfo.resolve(callExpr);
                if (call != null) {
                    List<RsValueArgumentList> argLists = Collections.singletonList((RsValueArgumentList) parent);
                    int index = ((RsValueArgumentList) parent).getExprList().indexOf(expr);
                    List<CallInfo.Parameter> params = call.getParameters();
                    if (index >= 0 && index < params.size()) {
                        String paramName = params.get(index).getPattern();
                        addName(names, paramName);
                    }
                }
            }
        }

        if (expr instanceof RsCallExpr) {
            for (String name : nameForCall((RsCallExpr) expr)) {
                addName(names, name);
            }
        }

        if (parent instanceof RsStructLiteralField) {
            RsStructLiteralField field = (RsStructLiteralField) parent;
            if (field.getIdentifier() != null) {
                addName(names, field.getIdentifier().getText());
            }
        }

        return finalizeNameSelection(expr, names, Collections.emptySet());
    }

    @NotNull
    public static SuggestedNames suggestedNames(@NotNull Ty ty, @NotNull PsiElement context, @NotNull Set<String> additionalNamesInScope) {
        LinkedHashSet<String> names = suggestedNamesForTy(ty);
        return finalizeNameSelection(context, names, additionalNamesInScope);
    }

    @NotNull
    public static SuggestedNames suggestedNames(@NotNull Ty ty, @NotNull PsiElement context) {
        return suggestedNames(ty, context, Collections.emptySet());
    }

    @NotNull
    public static String freshenName(@NotNull String name, @NotNull Set<String> usedNames) {
        if (!usedNames.contains(name)) return name;

        int suffixStart = name.length();
        while (suffixStart > 0 && name.charAt(suffixStart - 1) >= '0' && name.charAt(suffixStart - 1) <= '9') {
            suffixStart--;
        }
        String numberSuffix = name.substring(suffixStart);
        String nameWithoutNumber;
        int startIndex;
        if (numberSuffix.isEmpty()) {
            nameWithoutNumber = name;
            startIndex = 1;
        } else {
            nameWithoutNumber = name.substring(0, suffixStart);
            startIndex = Integer.parseInt(numberSuffix);
        }

        for (int i = startIndex; i < startIndex + FRESHEN_LIMIT; i++) {
            String candidate = nameWithoutNumber + i;
            if (!usedNames.contains(candidate)) {
                return candidate;
            }
        }
        return nameWithoutNumber + startIndex;
    }

    @NotNull
    private static SuggestedNames finalizeNameSelection(
        @NotNull PsiElement context,
        @NotNull LinkedHashSet<String> names,
        @NotNull Set<String> additionalNamesInScope
    ) {
        String topName = names.isEmpty() ? "x" : names.iterator().next();
        Set<String> usedNames = new HashSet<>(findNamesInLocalScope(context));
        usedNames.addAll(additionalNamesInScope);

        String name = freshenName(topName, usedNames);
        names.removeAll(usedNames);
        return new SuggestedNames(name, names);
    }

    @NotNull
    private static LinkedHashSet<String> suggestedNamesForTy(@NotNull Ty ty) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (ty instanceof TyInteger) {
            addName(names, "i");
        } else if (ty instanceof TyTypeParameter) {
            TyTypeParameter.TypeParameter param = ((TyTypeParameter) ty).getParameter();
            if (param instanceof TyTypeParameter.Named) {
                addName(names, ((TyTypeParameter.Named) param).getParameter().getName());
            }
        } else if (ty instanceof TyAdt) {
            addName(names, ((TyAdt) ty).getItem().getName());
        } else if (ty instanceof TyTraitObject) {
            for (BoundElement<org.rust.lang.core.psi.RsTraitItem> trait : ((TyTraitObject) ty).getTraits()) {
                addName(names, trait.getTypedElement().getName());
            }
        }
        return names;
    }

    private static void addName(@NotNull LinkedHashSet<String> names, @org.jetbrains.annotations.Nullable String name) {
        if (name == null || USELESS_NAMES.contains(name) || !RsNamesValidator.isValidRustVariableIdentifier(name)) return;
        List<String> suggestions = NameUtil.getSuggestionsByName(name, "", "", false, false, false);
        for (String suggestion : suggestions) {
            if (!USELESS_NAMES.contains(suggestion)
                && IntroduceVariableTestmarks.InvalidNamePart.hitOnFalse(RsNamesValidator.isValidRustVariableIdentifier(suggestion))) {
                names.add(org.rust.ide.inspections.lints.RsNamingInspection.toSnakeCase(suggestion, false));
            }
        }
    }

    @NotNull
    private static List<String> nameForCall(@NotNull RsCallExpr expr) {
        RsExpr pathElement = expr.getExpr();
        if (pathElement instanceof RsPathExpr) {
            RsPath path = ((RsPathExpr) pathElement).getPath();
            List<String> result = new ArrayList<>();
            if (path.getIdentifier() != null) {
                result.add(path.getIdentifier().getText());
            }
            if (path.getPath() != null && path.getPath().getIdentifier() != null) {
                result.add(path.getPath().getIdentifier().getText());
            }
            return result;
        }
        return Collections.singletonList(pathElement.getText());
    }

    @NotNull
    private static Set<String> findNamesInLocalScope(@NotNull PsiElement expr) {
        RsFunction functionScope = RsElementUtil.ancestorOrSelf(expr, RsFunction.class);
        Set<String> result = new HashSet<>();
        Collection<PsiElement> children = PsiTreeUtil.findChildrenOfAnyType(functionScope, RsPatBinding.class, RsPath.class);
        for (PsiElement child : children) {
            if (child instanceof RsPath) {
                String refName = ((RsPath) child).getReferenceName();
                if (refName != null) result.add(refName);
            } else if (child instanceof RsPatBinding) {
                String bindName = ((RsPatBinding) child).getName();
                if (bindName != null) result.add(bindName);
            }
        }
        return result;
    }
}
