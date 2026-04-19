/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;
import org.rust.lang.core.types.ty.TyInteger;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;

public class RsReprCompletionProvider extends RsCompletionProvider {
    public static final RsReprCompletionProvider INSTANCE = new RsReprCompletionProvider();

    private RsReprCompletionProvider() {
    }

    @NotNull
    @Override
    public ElementPattern<? extends PsiElement> getElementPattern() {
        return PlatformPatterns.psiElement()
            .withLanguage(RsLanguage.INSTANCE)
            .withParent(
                psiElement(RsPath.class)
                    .withParent(
                        psiElement(RsMetaItem.class)
                            .withSuperParent(
                                2,
                                RsPsiPattern.INSTANCE.rootMetaItem("repr", psiElement(RsStructOrEnumItemElement.class))
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
        RsStructOrEnumItemElement owner = RsElementUtil.ancestorStrict(
            parameters.getPosition(), RsStructOrEnumItemElement.class);
        if (owner == null) return;

        if (owner instanceof RsStructItem || owner instanceof RsEnumItem) {
            for (String name : new String[]{"C", "transparent", "align()"}) {
                result.addElement(createLookupElement(name));
            }
        }

        if (owner instanceof RsEnumItem) {
            for (String name : TyInteger.NAMES) {
                result.addElement(createLookupElement(name));
            }
        }

        if (owner instanceof RsStructItem) {
            for (String name : new String[]{"packed", "packed()", "simd"}) {
                result.addElement(createLookupElement(name));
            }
        }
    }

    private static LookupElementBuilder createLookupElement(String name) {
        LookupElementBuilder builder = LookupElementBuilder.create(name);
        if (name.endsWith("()")) {
            builder = builder.withInsertHandler((ctx, item) ->
                EditorModificationUtil.moveCaretRelatively(ctx.getEditor(), -1));
        }
        return builder;
    }
}
