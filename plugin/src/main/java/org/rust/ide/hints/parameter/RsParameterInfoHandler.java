/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.parameter;

import consulo.language.editor.parameterInfo.ParameterInfoUIContext;
import consulo.language.editor.parameterInfo.ParameterInfoUtils;
import consulo.language.editor.parameterInfo.UpdateParameterInfoContext;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.utils.CallInfo;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElementUtil;

import java.util.ArrayList;
import java.util.List;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.parameterInfo.ParameterInfoContext;
import org.rust.lang.RsLanguage;

/**
 * Provides functions/methods arguments hint.
 */
@ExtensionImpl
public class RsParameterInfoHandler extends RsAsyncParameterInfoHandler<RsValueArgumentList, RsArgumentsDescription> {

    @Nullable
    @Override
    public RsValueArgumentList findTargetElement(@Nonnull PsiFile file, int offset) {
        PsiElement element = file.findElementAt(offset);
        if (element == null) return null;
        return RsElementUtil.ancestorStrict(element, RsValueArgumentList.class);
    }

    @Nullable
    @Override
    public RsArgumentsDescription[] calculateParameterInfo(@Nonnull RsValueArgumentList element) {
        RsArgumentsDescription desc = RsArgumentsDescription.findDescription(element);
        if (desc == null) return null;
        return new RsArgumentsDescription[]{desc};
    }

    @Override
    public void updateParameterInfo(@Nonnull RsValueArgumentList parameterOwner, @Nonnull UpdateParameterInfoContext context) {
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
    public void updateUI(@Nonnull RsArgumentsDescription p, @Nonnull ParameterInfoUIContext context) {
        TextRange range = getArgumentRange(p.getArguments(), context.getCurrentParameterIndex());
        updateUI(p.getPresentText(), range, context);
    }

    @Override
    public Object[] getParametersForLookup(consulo.language.editor.completion.lookup.LookupElement item, consulo.language.editor.parameterInfo.ParameterInfoContext context) {
        return null;
    }

    @Override
    public boolean couldShowInLookup() {
        return false;
    }

    @jakarta.annotation.Nonnull
    @Override
    public consulo.language.Language getLanguage() {
        return org.rust.lang.RsLanguage.INSTANCE;
    }
}
