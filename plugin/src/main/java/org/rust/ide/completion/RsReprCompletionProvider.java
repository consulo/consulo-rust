/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.completion;

import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;
import org.rust.lang.core.types.ty.TyInteger;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;
import org.rust.lang.core.completion.RsCompletionProvider;

public class RsReprCompletionProvider extends RsCompletionProvider {
    public static final RsReprCompletionProvider INSTANCE = new RsReprCompletionProvider();

    private RsReprCompletionProvider() {
    }

    @Nonnull
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
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
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
