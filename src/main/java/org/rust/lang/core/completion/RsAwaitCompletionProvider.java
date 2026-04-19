/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Key;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @NotNull
    @Override
    public ElementPattern<? extends PsiElement> getElementPattern() {
        ElementPattern<RsFieldLookup> parent = psiElement(RsFieldLookup.class)
            .with(new PatternCondition<RsFieldLookup>("RsPostfixAwait") {
                @Override
                public boolean accepts(@NotNull RsFieldLookup t, @Nullable ProcessingContext context) {
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
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
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
