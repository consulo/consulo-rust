/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.parameter;

import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.ParameterInfoUtils;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.utils.CallInfo;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElementUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides functions/methods arguments hint.
 */
public class RsParameterInfoHandler extends RsAsyncParameterInfoHandler<RsValueArgumentList, RsArgumentsDescription> {

    @Nullable
    @Override
    public RsValueArgumentList findTargetElement(@NotNull PsiFile file, int offset) {
        PsiElement element = file.findElementAt(offset);
        if (element == null) return null;
        return RsElementUtil.ancestorStrict(element, RsValueArgumentList.class);
    }

    @Nullable
    @Override
    public RsArgumentsDescription[] calculateParameterInfo(@NotNull RsValueArgumentList element) {
        RsArgumentsDescription desc = RsArgumentsDescription.findDescription(element);
        if (desc == null) return null;
        return new RsArgumentsDescription[]{desc};
    }

    @Override
    public void updateParameterInfo(@NotNull RsValueArgumentList parameterOwner, @NotNull UpdateParameterInfoContext context) {
        if (context.getParameterOwner() != parameterOwner) {
            context.removeHint();
            return;
        }
        int currentParameterIndex;
        if (parameterOwner.getTextRange().getStartOffset() == context.getOffset()) {
            currentParameterIndex = -1;
        } else {
            currentParameterIndex = ParameterInfoUtils.getCurrentParameterIndex(
                parameterOwner.getNode(), context.getOffset(), RsElementTypes.COMMA);
        }
        context.setCurrentParameter(currentParameterIndex);
    }

    @Override
    public void updateUI(@NotNull RsArgumentsDescription p, @NotNull ParameterInfoUIContext context) {
        TextRange range = getArgumentRange(p.getArguments(), context.getCurrentParameterIndex());
        updateUI(p.getPresentText(), range, context);
    }
}
