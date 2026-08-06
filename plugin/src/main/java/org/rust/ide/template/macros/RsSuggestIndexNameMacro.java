/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.macros;

import consulo.language.editor.template.Expression;
import consulo.language.editor.template.ExpressionContext;
import consulo.language.editor.template.Result;
import consulo.language.editor.template.TextResult;
import consulo.language.editor.template.macro.MacroBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.NameResolutionUtil;

import java.util.HashSet;
import java.util.Set;

public class RsSuggestIndexNameMacro extends MacroBase {

    public RsSuggestIndexNameMacro() {
        super("rustSuggestIndexName", consulo.localize.LocalizeValue.of("rustSuggestIndexName()"));
    }

    @Override
    protected Result calculateResult(Expression[] params, ExpressionContext context, boolean quick) {
        if (params.length > 0) return null;
        PsiElement startElement = context.getPsiElementAtStartOffset();
        if (startElement == null) return null;
        RsElement pivot = PsiTreeUtil.getParentOfType(startElement, RsElement.class, false);
        if (pivot == null) return null;
        Set<String> pats = getPatBindingNamesVisibleAt(pivot);
        for (char c = 'i'; c <= 'z'; c++) {
            String name = String.valueOf(c);
            if (!pats.contains(name)) {
                return new TextResult(name);
            }
        }
        return null;
    }

    private static Set<String> getPatBindingNamesVisibleAt(RsElement pivot) {
        Set<String> result = new HashSet<>();
        NameResolutionUtil.processLocalVariables(pivot, patBinding -> {
            String name = patBinding.getName();
            if (name != null) {
                result.add(name);
            }
        });
        return result;
    }
}
