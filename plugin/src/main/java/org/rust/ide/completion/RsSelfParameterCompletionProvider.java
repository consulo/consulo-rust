/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.icons.RsIcons;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsAbstractableOwner;
import org.rust.lang.core.psi.ext.impl.RsAbstractableUtil;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;
import static org.rust.lang.core.PsiElementPatternExtUtil.or;
import consulo.language.pattern.PatternCondition;
import org.rust.lang.core.completion.LookupElements;
import org.rust.lang.core.completion.RsCompletionProvider;

public class RsSelfParameterCompletionProvider extends RsCompletionProvider {
    public static final RsSelfParameterCompletionProvider INSTANCE = new RsSelfParameterCompletionProvider();

    private RsSelfParameterCompletionProvider() {
    }

    @Nonnull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        ElementPattern<RsValueParameter> firstParam = psiElement(RsValueParameter.class)
            .with(new consulo.language.pattern.PatternCondition<PsiElement>("isImplFirstParam") {
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
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
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
