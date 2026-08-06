/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsVisRestriction;
import org.rust.lang.core.psi.ext.RsPathUtil;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;

/**
 * Provides completion inside visibility restriction:
 * {@code pub(<here>)}
 */
public class RsVisRestrictionCompletionProvider extends RsCompletionProvider {
    public static final RsVisRestrictionCompletionProvider INSTANCE = new RsVisRestrictionCompletionProvider();

    private RsVisRestrictionCompletionProvider() {
    }

    @Nonnull
    @Override
    public PsiElementPattern.Capture<PsiElement> getElementPattern() {
        return consulo.language.pattern.PlatformPatterns.psiElement(RsElementTypes.IDENTIFIER)
            .withParent(
                psiElement(RsPath.class)
                    .with(new consulo.language.pattern.PatternCondition<PsiElement>("hasOneSegment") {
                        @Override
                        public boolean accepts(@org.jetbrains.annotations.NotNull PsiElement item, ProcessingContext ctx) {
                            if (!(item instanceof RsPath)) return false;
                            RsPath path = (RsPath) item;
                            return RsPathUtil.getQualifier(path) == null && path.getTypeQual() == null && !path.getHasColonColon();
                        }
                    })
            ).withSuperParent(2, psiElement(RsVisRestriction.class));
    }

    @Override
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
    ) {
        for (String name : new String[]{"crate", "super", "self"}) {
            result.addElement(
                LookupElements.toKeywordElement(
                    LookupElementBuilder
                        .create(name)
                        .withIcon(RsIcons.MODULE)
                        .bold()
                )
            );
        }
        PsiElement parent = parameters.getPosition().getParent();
        if (parent != null) {
            PsiElement grandParent = parent.getParent();
            if (grandParent instanceof RsVisRestriction) {
                if (((RsVisRestriction) grandParent).getIn() == null) {
                    result.addElement(LookupElementBuilder.create("in ").withPresentableText("in"));
                }
            }
        }
    }
}
