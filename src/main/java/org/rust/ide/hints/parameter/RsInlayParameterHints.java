/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.parameter;

import com.intellij.codeInsight.hints.InlayInfo;
import com.intellij.codeInsight.hints.Option;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.hints.type.RsInlayTypeHintsProvider;
import org.rust.ide.utils.CallInfo;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.types.SubstitutionUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

@SuppressWarnings("UnstableApiUsage")
public final class RsInlayParameterHints {
    private RsInlayParameterHints() {
    }

    public static final RsInlayParameterHints INSTANCE = new RsInlayParameterHints();

    public static final Option smartOption = new Option("SMART_HINTS", RsBundle.messagePointer("settings.rust.inlay.parameter.hints.only.smart"), true);

    public static boolean getSmart() {
        return smartOption.get();
    }

    @NotNull
    public static List<InlayInfo> provideHints(@NotNull PsiElement elem) {
        PsiElement elementExpanded = findExpandedElement(elem);
        if (elementExpanded == null) return Collections.emptyList();

        CallInfo callInfo;
        if (elementExpanded instanceof RsCallExpr) {
            callInfo = CallInfo.resolve((RsCallExpr) elementExpanded);
        } else if (elementExpanded instanceof RsMethodCall) {
            callInfo = CallInfo.resolve((RsMethodCall) elementExpanded);
        } else {
            callInfo = null;
        }
        if (callInfo == null) return Collections.emptyList();

        RsValueArgumentList valueArgumentList = RsPsiJavaUtil.childOfType(elem, RsValueArgumentList.class);
        if (valueArgumentList == null) return Collections.emptyList();

        List<String> hintNames = new ArrayList<>();
        if (callInfo.getSelfParameter() != null && elem instanceof RsCallExpr) {
            hintNames.add(callInfo.getSelfParameter());
        }
        for (CallInfo.Parameter param : callInfo.getParameters()) {
            if (param.getPattern() != null) {
                hintNames.add(param.getPattern());
            } else {
                String typeText = param.getTypeRef() != null
                    ? org.rust.lang.core.psi.ext.RsTypeReferenceUtil.substAndGetText(param.getTypeRef(), SubstitutionUtil.getEmptySubstitution())
                    : "_";
                hintNames.add(typeText);
            }
        }

        List<RsExpr> args = valueArgumentList.getExprList();
        int size = Math.min(hintNames.size(), args.size());

        List<String[]> hints = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            hints.add(new String[]{hintNames.get(i), args.get(i).getText()});
        }

        boolean smart = getSmart();
        if (smart) {
            if (onlyOneParam(hints, callInfo, elem)) {
                String methodName = callInfo.getMethodName();
                if (methodName != null && methodName.startsWith("set_")) {
                    return Collections.emptyList();
                }
                String firstPattern = callInfo.getParameters().isEmpty() ? null : callInfo.getParameters().get(0).getPattern();
                if (methodName != null && methodName.equals(firstPattern)) {
                    return Collections.emptyList();
                }
                if (!hints.isEmpty()) {
                    RsExpr lastArg = args.get(Math.min(hints.size() - 1, args.size() - 1));
                    if (lastArg instanceof RsLambdaExpr) {
                        return Collections.emptyList();
                    }
                }
            }
            boolean allUnderscores = true;
            for (String[] hint : hints) {
                if (!"_".equals(hint[0])) {
                    allUnderscores = false;
                    break;
                }
            }
            if (allUnderscores) return Collections.emptyList();

            List<InlayInfo> result = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                String hint = hintNames.get(i);
                RsExpr arg = args.get(i);
                if (!arg.getText().endsWith(hint)) {
                    result.add(new InlayInfo(hint + ":", arg.getTextRange().getStartOffset()));
                }
            }
            return result;
        }

        List<InlayInfo> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String hint = hintNames.get(i);
            RsExpr arg = args.get(i);
            result.add(new InlayInfo(hint + ":", arg.getTextRange().getStartOffset()));
        }
        return result;
    }

    private static PsiElement findExpandedElement(@NotNull PsiElement element) {
        RsValueArgumentList valueArgumentList;
        if (element instanceof RsCallExpr) {
            valueArgumentList = ((RsCallExpr) element).getValueArgumentList();
        } else if (element instanceof RsMethodCall) {
            valueArgumentList = ((RsMethodCall) element).getValueArgumentList();
        } else {
            return null;
        }
        RsValueArgumentList expanded = RsInlayTypeHintsProvider.findExpandedByLeaf(valueArgumentList, null, RsValueArgumentList::getLparen);
        if (expanded == null) return null;
        if (expanded == valueArgumentList) return element;
        if (expanded.getExprList().size() != valueArgumentList.getExprList().size()) return null;
        PsiElement parent = expanded.getParent();
        if (parent != null && RsElementUtil.getElementType(parent) == RsElementUtil.getElementType(element)) {
            return parent;
        }
        return null;
    }

    private static boolean onlyOneParam(@NotNull List<String[]> hints, @NotNull CallInfo callInfo, @NotNull PsiElement elem) {
        if (callInfo.getSelfParameter() != null && elem instanceof RsCallExpr && hints.size() == 2) {
            return true;
        }
        if (!(callInfo.getSelfParameter() != null && elem instanceof RsCallExpr) && hints.size() == 1) {
            return true;
        }
        return false;
    }
}
