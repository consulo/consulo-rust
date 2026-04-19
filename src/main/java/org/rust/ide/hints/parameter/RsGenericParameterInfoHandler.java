/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.parameter;

import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.ParameterInfoUtils;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.presentation.PsiRenderingOptions;
import org.rust.ide.presentation.RsPsiRenderer;
import org.rust.ide.presentation.RsPsiRendererUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.ArrayList;
import java.util.List;
import org.rust.lang.core.psi.ext.RsTypeReferenceUtil;
import org.rust.lang.core.psi.ext.RsTypeParameterUtil;
import org.rust.lang.core.psi.ext.RsTraitRefUtil;

public class RsGenericParameterInfoHandler extends RsAsyncParameterInfoHandler<RsTypeArgumentList, HintLine> {

    private static final String WHERE_PREFIX = "where ";

    @Nullable
    @Override
    public RsTypeArgumentList findTargetElement(@NotNull PsiFile file, int offset) {
        PsiElement element = file.findElementAt(offset);
        if (element == null) return null;
        if (RsElementUtil.getElementType(element) == RsElementTypes.COLONCOLON) return null;
        return RsElementUtil.ancestorStrict(element, RsTypeArgumentList.class);
    }

    @Nullable
    @Override
    public HintLine[] calculateParameterInfo(@NotNull RsTypeArgumentList element) {
        PsiElement parent = element.getParent();
        RsGenericDeclaration genericDeclaration;
        if (parent instanceof RsMethodCall || parent instanceof RsPath) {
            PsiElement resolved = parent.getReference() != null ? parent.getReference().resolve() : null;
            if (resolved instanceof RsGenericDeclaration) {
                genericDeclaration = (RsGenericDeclaration) resolved;
            } else {
                return null;
            }
        } else {
            return null;
        }
        List<RsGenericParameter> paramsWithBounds;
        {
            RsTypeParameterList tpl = genericDeclaration.getTypeParameterList();
            paramsWithBounds = tpl != null
                ? org.rust.lang.core.psi.ext.RsTypeParameterListUtil.getGenericParameters(tpl, false)
                : java.util.Collections.emptyList();
        }
        if (paramsWithBounds.isEmpty()) return null;

        List<HintLine> lines = new ArrayList<>();
        lines.add(firstLine(paramsWithBounds));
        HintLine second = secondLine(paramsWithBounds);
        if (second != null) {
            lines.add(second);
        }
        return lines.toArray(new HintLine[0]);
    }

    @Override
    public void showParameterInfo(@NotNull RsTypeArgumentList element, @NotNull CreateParameterInfoContext context) {
        context.setHighlightedElement(null);
        super.showParameterInfo(element, context);
    }

    @Override
    public void updateParameterInfo(@NotNull RsTypeArgumentList parameterOwner, @NotNull UpdateParameterInfoContext context) {
        if (context.getParameterOwner() != parameterOwner) {
            context.removeHint();
            return;
        }
        int curParam = ParameterInfoUtils.getCurrentParameterIndex(parameterOwner.getNode(), context.getOffset(), RsElementTypes.COMMA);
        context.setCurrentParameter(curParam);
    }

    @Override
    public void updateUI(@NotNull HintLine p, @NotNull ParameterInfoUIContext context) {
        TextRange range = p.getRange(context.getCurrentParameterIndex());
        context.setupUIComponentPresentation(
            p.getPresentText(),
            range.getStartOffset(),
            range.getEndOffset(),
            false,
            false,
            false,
            context.getDefaultParameterColor()
        );
    }

    @NotNull
    private static HintLine firstLine(@NotNull List<RsGenericParameter> params) {
        RsPsiRenderer renderer = new RsPsiRenderer(new PsiRenderingOptions(false, true, true));
        List<String> split = new ArrayList<>();
        for (RsGenericParameter param : params) {
            if (param instanceof RsTypeParameter) {
                RsTypeParameter tp = (RsTypeParameter) param;
                String name = tp.getName();
                if (name == null) {
                    split.add("");
                    continue;
                }
                List<String> qSizedBound = RsTypeParameterUtil.isSized(tp) ? new ArrayList<>() : new ArrayList<>(List.of("?Sized"));
                List<String> declaredBounds = new ArrayList<>();
                for (RsPolybound bound : RsTypeParameterUtil.getBounds(tp)) {
                    RsTraitRef traitRef = bound.getBound().getTraitRef();
                    if (traitRef != null
                        && RsTraitRefUtil.resolveToBoundTrait(traitRef) != null
                        && !RsTraitItemUtil.isSizedTrait(RsTraitRefUtil.resolveToBoundTrait(traitRef).getTypedElement())) {
                        String text = traitRef.getPath().getText();
                        if (text != null) {
                            declaredBounds.add(text);
                        }
                    }
                }
                List<String> allBounds = new ArrayList<>(qSizedBound);
                allBounds.addAll(declaredBounds);
                String result = name;
                if (!allBounds.isEmpty()) {
                    result += ": " + String.join(" + ", allBounds);
                }
                split.add(result);
            } else if (param instanceof RsConstParameter) {
                RsConstParameter cp = (RsConstParameter) param;
                String typeReference = cp.getTypeReference() != null
                    ? ": " + RsPsiRendererUtil.renderTypeReference(renderer, cp.getTypeReference())
                    : "";
                String paramName = cp.getName() != null ? cp.getName() : "_";
                split.add("const " + paramName + typeReference);
            }
        }
        String text = String.join(", ", split);
        List<TextRange> ranges = new ArrayList<>();
        for (int i = 0; i < split.size(); i++) {
            ranges.add(calculateRange(split, i));
        }
        return new HintLine(text, ranges);
    }

    @Nullable
    private static HintLine secondLine(@NotNull List<RsGenericParameter> params) {
        PsiElement first = params.isEmpty() ? null : params.get(0);
        if (first == null) return null;
        PsiElement grandparent = first.getParent() != null ? first.getParent().getParent() : null;
        if (!(grandparent instanceof RsGenericDeclaration)) return null;
        RsGenericDeclaration owner = (RsGenericDeclaration) grandparent;
        List<RsWherePred> wherePreds = RsGenericDeclarationUtil.getWherePreds(owner);
        List<String> split = new ArrayList<>();
        for (RsWherePred pred : wherePreds) {
            RsTypeReference typeRef = pred.getTypeReference();
            if (typeRef != null) {
                PsiElement resolved = null;
                RsTypeReference skipped = RsTypeReferenceUtil.skipParens(typeRef);
                if (skipped instanceof RsPathType) {
                    RsPath path = ((RsPathType) skipped).getPath();
                    if (path.getReference() != null) {
                        resolved = path.getReference().resolve();
                    }
                }
                if (params.contains(resolved)) continue;
            }
            split.add(pred.getText());
        }
        if (!split.isEmpty()) {
            String text = WHERE_PREFIX + String.join(", ", split);
            List<TextRange> ranges = new ArrayList<>();
            for (int i = 0; i < split.size(); i++) {
                ranges.add(new TextRange(0, WHERE_PREFIX.length()));
            }
            return new HintLine(text, ranges);
        }
        return null;
    }

    @NotNull
    private static TextRange calculateRange(@NotNull List<String> list, int index) {
        int start = 0;
        for (int i = 0; i < index; i++) {
            start += list.get(i).length() + 2;
        }
        return new TextRange(start, start + list.get(index).length());
    }
}
