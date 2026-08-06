/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.util.dataholder.Key;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PatternCondition;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.completion.RsLookupElementProperties.KeywordKind;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsFieldLookup;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsFieldLookupUtil;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnknown;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;

public class RsAwaitCompletionProvider extends RsCompletionProvider {
    public static final RsAwaitCompletionProvider INSTANCE = new RsAwaitCompletionProvider();

    private static final Key<Ty> AWAIT_TY = Key.create("AWAIT_TY");

    private RsAwaitCompletionProvider() {
    }

    @Nonnull
    @Override
    public ElementPattern<? extends PsiElement> getElementPattern() {
        ElementPattern<RsFieldLookup> parent = psiElement(RsFieldLookup.class)
            .with(new PatternCondition<RsFieldLookup>("RsPostfixAwait") {
                @Override
                public boolean accepts(@Nonnull RsFieldLookup t, @Nullable ProcessingContext context) {
                    if (context == null || !RsElementUtil.isAtLeastEdition2018(t)) return false;
                    RsExpr receiver = Utils.safeGetOriginalOrSelf(RsFieldLookupUtil.getReceiver(t));
                    ImplLookup lookup = ImplLookup.relativeTo(receiver);
                    Ty awaitTy = lookup.lookupFutureOutputTy(RsTypesUtil.getType(receiver), true).getValue();
                    if (awaitTy instanceof TyUnknown) return false;
                    context.put(AWAIT_TY, awaitTy);
                    return true;
                }
            });

        return PlatformPatterns.psiElement(RsElementTypes.IDENTIFIER).withParent(parent);
    }

    @Override
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
    ) {
        Ty awaitTy = context.get(AWAIT_TY);
        if (awaitTy == null) return;
        LookupElementBuilder awaitBuilder = LookupElementBuilder
            .create("await")
            .bold()
            .withTypeText(awaitTy.toString());
        result.addElement(LookupElements.toKeywordElement(awaitBuilder, KeywordKind.AWAIT));
    }
}
