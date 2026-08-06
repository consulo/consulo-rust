/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
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

    @Nonnull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        return RsPsiPattern.INSTANCE.getSimplePathPattern();
    }

    @Override
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
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
