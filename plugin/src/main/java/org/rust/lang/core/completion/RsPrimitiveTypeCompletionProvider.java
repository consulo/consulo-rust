/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.types.ty.TyBool;
import org.rust.lang.core.types.ty.TyChar;
import org.rust.lang.core.types.ty.TyFloat;
import org.rust.lang.core.types.ty.TyInteger;
import org.rust.lang.core.types.ty.TyStr;

import java.util.ArrayList;
import java.util.List;

import static org.rust.lang.core.PsiElementPatternExtUtil.withSuperParent;

public class RsPrimitiveTypeCompletionProvider extends RsCompletionProvider {
    public static final RsPrimitiveTypeCompletionProvider INSTANCE = new RsPrimitiveTypeCompletionProvider();

    private final List<String> myPrimitives;

    private RsPrimitiveTypeCompletionProvider() {
        myPrimitives = new ArrayList<>();
        myPrimitives.addAll(TyInteger.NAMES);
        myPrimitives.addAll(TyFloat.NAMES);
        myPrimitives.add(TyBool.INSTANCE.getName());
        myPrimitives.add(TyStr.INSTANCE.getName());
        myPrimitives.add(TyChar.INSTANCE.getName());
    }

    @Nonnull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        return withSuperParent(PlatformPatterns.psiElement(), 2, RsTypeReference.class)
            .with(new consulo.language.pattern.PatternCondition<PsiElement>("FirstChild") {
                @Override
                public boolean accepts(@org.jetbrains.annotations.NotNull PsiElement e, ProcessingContext ctx) {
                    return e.getPrevSibling() == null;
                }
            });
    }

    @Override
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
    ) {
        for (String primitive : myPrimitives) {
            result.addElement(LookupElements.toKeywordElement(LookupElementBuilder.create(primitive).bold()));
        }
    }
}
