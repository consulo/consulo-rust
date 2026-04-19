/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.search;

import com.intellij.psi.PsiElement;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProviderEx;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.*;

import java.util.List;

public class RsUsageTypeProvider implements UsageTypeProviderEx {

    public static final RsUsageTypeProvider INSTANCE = new RsUsageTypeProvider();

    // Instantiate each UsageType only once, so that the equality check in UsageTypeGroup.equals() works correctly
    private static final UsageType IMPL = new UsageType(() -> "impl");
    private static final UsageType TYPE_REFERENCE = new UsageType(() -> "type reference");
    private static final UsageType TRAIT_REFERENCE = new UsageType(() -> "trait reference");
    private static final UsageType EXPR = new UsageType(() -> "expr");
    private static final UsageType DOT_EXPR = new UsageType(() -> "dot expr");
    private static final UsageType FUNCTION_CALL = new UsageType(() -> "function call");
    private static final UsageType METHOD_CALL = new UsageType(() -> "method call");
    private static final UsageType ARGUMENT = new UsageType(() -> "argument");
    private static final UsageType MACRO_CALL = new UsageType(() -> "macro call");
    private static final UsageType MACRO_ARGUMENT = new UsageType(() -> "macro argument");
    private static final UsageType INIT_STRUCT = new UsageType(() -> "init struct");
    private static final UsageType INIT_FIELD = new UsageType(() -> "init field");
    private static final UsageType PAT_BINDING = new UsageType(() -> "variable binding");
    private static final UsageType FIELD = new UsageType(() -> "field");
    private static final UsageType META_ITEM = new UsageType(() -> "meta item");
    private static final UsageType USE = new UsageType(() -> "use");
    private static final UsageType MOD = new UsageType(() -> "mod");

    @Override
    public UsageType getUsageType(PsiElement element) {
        return getUsageType(element, UsageTarget.EMPTY_ARRAY);
    }

    @Override
    public UsageType getUsageType(PsiElement element, UsageTarget[] targets) {
        PsiElement refinedElement = element;
        List<PsiElement> expansionElements = RsExpandedElementUtil.findExpansionElements(element);
        if (expansionElements != null && !expansionElements.isEmpty()) {
            PsiElement first = expansionElements.get(0);
            if (first.getParent() != null) {
                refinedElement = first.getParent();
            }
        }
        if (refinedElement == null) return null;

        PsiElement parent = goUp(refinedElement, RsPath.class);
        if (parent == null) return null;

        if (parent instanceof RsPathType) {
            if (parent.getParent() instanceof RsImplItem) {
                return IMPL;
            }
            return TYPE_REFERENCE;
        } else if (parent instanceof RsPathExpr) {
            PsiElement grandParent = goUp(parent, RsPathExpr.class);
            if (grandParent instanceof RsDotExpr) return DOT_EXPR;
            if (grandParent instanceof RsCallExpr) return FUNCTION_CALL;
            if (grandParent instanceof RsValueArgumentList) return ARGUMENT;
            if (grandParent instanceof RsFormatMacroArg) return MACRO_ARGUMENT;
            if (grandParent instanceof RsExpr) return EXPR;
            return null;
        } else if (parent instanceof RsUseSpeck) {
            return USE;
        } else if (parent instanceof RsStructLiteral) {
            return INIT_STRUCT;
        } else if (parent instanceof RsStructLiteralField) {
            return INIT_FIELD;
        } else if (parent instanceof RsTraitRef) {
            return TRAIT_REFERENCE;
        } else if (parent instanceof RsMethodCall) {
            return METHOD_CALL;
        } else if (parent instanceof RsMetaItem) {
            return META_ITEM;
        } else if (parent instanceof RsFieldLookup) {
            return FIELD;
        } else if (parent instanceof RsMacroCall) {
            return MACRO_CALL;
        } else if (parent instanceof RsPatBinding) {
            return PAT_BINDING;
        } else {
            if (parent.getParent() instanceof RsModDeclItem) {
                return MOD;
            }
            return null;
        }
    }

    private static PsiElement goUp(PsiElement element, Class<? extends PsiElement> type) {
        PsiElement context = element;
        while (type.isInstance(context)) {
            context = context.getParent();
        }
        return context;
    }
}
