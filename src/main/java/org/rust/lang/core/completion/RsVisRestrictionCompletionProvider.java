/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
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

    @NotNull
    @Override
    public PsiElementPattern.Capture<PsiElement> getElementPattern() {
        return com.intellij.patterns.PlatformPatterns.psiElement(RsElementTypes.IDENTIFIER)
            .withParent(
                psiElement(RsPath.class)
                    .with(new com.intellij.patterns.PatternCondition<PsiElement>("hasOneSegment") {
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
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
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
