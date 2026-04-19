/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.RsPathExpr;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyBool;
import org.rust.lang.core.types.ty.TyUnknown;

public class RsBoolCompletionProvider extends RsCompletionProvider {
    public static final RsBoolCompletionProvider INSTANCE = new RsBoolCompletionProvider();

    private RsBoolCompletionProvider() {
    }

    @NotNull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        return RsPsiPattern.INSTANCE.getSimplePathPattern();
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        PsiElement position = Utils.safeGetOriginalOrSelf(parameters.getPosition());
        RsPathExpr pathExpr = RsElementUtil.ancestorOrSelf(position, RsPathExpr.class);
        if (pathExpr == null) return;
        Ty expectedType = ExtensionsUtil.getExpectedType(pathExpr);
        if (expectedType instanceof TyBool || expectedType instanceof TyUnknown) {
            for (String value : new String[]{"true", "false"}) {
                result.addElement(LookupElements.toKeywordElement(LookupElementBuilder.create(value).bold()));
            }
        }
    }
}
