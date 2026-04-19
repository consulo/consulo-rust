/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsAbstractableOwner;
import org.rust.lang.core.psi.ext.RsAbstractableUtil;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;
import static org.rust.lang.core.PsiElementPatternExtUtil.or;

public class RsSelfParameterCompletionProvider extends RsCompletionProvider {
    public static final RsSelfParameterCompletionProvider INSTANCE = new RsSelfParameterCompletionProvider();

    private RsSelfParameterCompletionProvider() {
    }

    @NotNull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        ElementPattern<RsValueParameter> firstParam = psiElement(RsValueParameter.class)
            .with(new com.intellij.patterns.PatternCondition<PsiElement>("isImplFirstParam") {
                @Override
                public boolean accepts(@org.jetbrains.annotations.NotNull PsiElement param, ProcessingContext ctx) {
                    if (!(param instanceof RsValueParameter)) return false;
                    RsValueParameter valueParam = (RsValueParameter) param;
                    PsiElement paramList = valueParam.getContext();
                    if (!(paramList instanceof RsValueParameterList)) return false;
                    PsiElement function = paramList.getContext();
                    if (!(function instanceof RsFunction)) return false;
                    return valueParam.getPat() == null
                        && RsAbstractableUtil.getOwner((RsFunction) function) instanceof RsAbstractableOwner.Impl
                        && ((RsValueParameterList) paramList).getSelfParameter() == null
                        && !((RsValueParameterList) paramList).getValueParameterList().isEmpty()
                        && ((RsValueParameterList) paramList).getValueParameterList().get(0) == valueParam;
                }
            });

        return RsPsiPattern.getSimplePathPattern().withParent(
            psiElement(RsPath.class).withParent(
                or(
                    psiElement(RsPathType.class).withParent(firstParam),
                    psiElement(RsPathType.class).withParent(psiElement(RsRefLikeType.class).withParent(firstParam))
                )
            )
        );
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        result.addElement(
            LookupElements.toKeywordElement(
                LookupElementBuilder.create("self")
                    .bold()
                    .withIcon(RsIcons.BINDING)
            )
        );
    }
}
