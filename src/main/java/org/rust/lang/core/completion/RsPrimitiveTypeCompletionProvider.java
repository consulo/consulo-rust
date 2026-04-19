/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
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

    @NotNull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        return withSuperParent(PlatformPatterns.psiElement(), 2, RsTypeReference.class)
            .with(new com.intellij.patterns.PatternCondition<PsiElement>("FirstChild") {
                @Override
                public boolean accepts(@org.jetbrains.annotations.NotNull PsiElement e, ProcessingContext ctx) {
                    return e.getPrevSibling() == null;
                }
            });
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        for (String primitive : myPrimitives) {
            result.addElement(LookupElements.toKeywordElement(LookupElementBuilder.create(primitive).bold()));
        }
    }
}
