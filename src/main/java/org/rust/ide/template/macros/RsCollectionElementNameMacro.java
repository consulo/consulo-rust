/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.macros;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.codeInsight.template.macro.MacroBase;
import com.intellij.openapi.util.text.StringUtil;
import org.rust.ide.refactoring.RsNamesValidator;

import java.util.ArrayList;
import java.util.List;

public class RsCollectionElementNameMacro extends MacroBase {

    private static final String[] SUFFIXES = {"_list", "_set"};

    public RsCollectionElementNameMacro() {
        super("rustCollectionElementName", "rustCollectionElementName()");
    }

    @Override
    protected Result calculateResult(Expression[] params, ExpressionContext context, boolean quick) {
        String param = getCollectionExprStr(params, context);
        if (param == null) return null;

        if (param.endsWith(".iter()")) param = param.substring(0, param.length() - 7);
        else if (param.endsWith(".iter_mut()")) param = param.substring(0, param.length() - 11);
        else if (param.endsWith(".into_iter()")) param = param.substring(0, param.length() - 12);

        int lastDot = param.lastIndexOf('.');
        if (lastDot >= 0) {
            param = param.substring(lastDot + 1);
        }

        int lastDoubleColon = param.lastIndexOf("::");
        if (lastDoubleColon > 0) {
            param = param.substring(0, lastDoubleColon);
        }

        if (param.endsWith(")")) {
            int lastParen = param.lastIndexOf('(');
            if (lastParen > 0) {
                param = param.substring(0, lastParen);
            }
        }

        String name = unpluralize(param);
        if (name == null) return null;
        if (new RsNamesValidator().isIdentifier(name, context.getProject())) {
            return new TextResult(name);
        }
        return null;
    }

    @Override
    public LookupElement[] calculateLookupItems(Expression[] params, ExpressionContext context) {
        Result result = calculateResult(params, context);
        if (result == null) return null;
        String[] words = result.toString().split("_");
        if (words.length > 1) {
            List<LookupElement> lookups = new ArrayList<>();
            for (int i = 0; i < words.length; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = i; j < words.length; j++) {
                    if (j > i) sb.append('_');
                    sb.append(words[j]);
                }
                lookups.add(LookupElementBuilder.create(sb.toString()));
            }
            return lookups.toArray(LookupElement.EMPTY_ARRAY);
        }
        return null;
    }

    private static String getCollectionExprStr(Expression[] params, ExpressionContext context) {
        if (params.length != 1) return null;
        Result result = params[0].calculateResult(context);
        return result != null ? result.toString() : null;
    }

    private static String unpluralize(String name) {
        for (String suffix : SUFFIXES) {
            if (name.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length());
            }
        }
        return StringUtil.unpluralize(name);
    }
}
