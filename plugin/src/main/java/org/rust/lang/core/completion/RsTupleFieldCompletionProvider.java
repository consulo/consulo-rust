/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.util.dataholder.Key;
import consulo.language.pattern.PatternCondition;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsFieldLookup;
import org.rust.lang.core.psi.ext.RsFieldLookupUtil;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTuple;

import java.util.ArrayList;
import java.util.List;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;
import consulo.language.editor.completion.lookup.LookupElement;

public class RsTupleFieldCompletionProvider extends RsCompletionProvider {
    public static final RsTupleFieldCompletionProvider INSTANCE = new RsTupleFieldCompletionProvider();

    private static final Key<Object[]> TUPLE_FIELD_INFO = Key.create("TUPLE_FIELD_INFO");

    private RsTupleFieldCompletionProvider() {
    }

    @Nonnull
    @Override
    public PsiElementPattern.Capture<PsiElement> getElementPattern() {
        PsiElementPattern.Capture<RsFieldLookup> parent = psiElement(RsFieldLookup.class)
            .with(new PatternCondition<RsFieldLookup>("TupleType") {
                @Override
                public boolean accepts(@Nonnull RsFieldLookup t, @Nullable ProcessingContext context) {
                    if (context == null) return false;
                    RsFieldLookup fieldLookup = Utils.safeGetOriginalOrSelf(t);
                    Ty type = ExtensionsUtil.getType(RsFieldLookupUtil.getReceiver(fieldLookup));
                    if (!(type instanceof TyTuple)) return false;
                    context.put(TUPLE_FIELD_INFO, new Object[]{fieldLookup, type});
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
        Object[] info = context.get(TUPLE_FIELD_INFO);
        if (info == null) return;
        RsFieldLookup fieldLookup = (RsFieldLookup) info[0];
        TyTuple type = (TyTuple) info[1];

        RsCompletionContext completionContext = new RsCompletionContext(
            fieldLookup,
            ExtensionsUtil.getExpectedTypeCoercable(RsFieldLookupUtil.getParentDotExpr(fieldLookup)),
            false
        );

        List<Ty> types = type.getTypes();
        List<consulo.language.editor.completion.lookup.LookupElement> elements = new ArrayList<>();
        for (int index = 0; index < types.size(); index++) {
            Ty ty = types.get(index);
            int finalIndex = index;
            CompletionEntity entity = new CompletionEntity() {
                @Override
                public Ty retTy(KnownItems items) {
                    return ty;
                }

                @Override
                public RsLookupElementProperties getBaseLookupElementProperties(RsCompletionContext ctx) {
                    return new RsLookupElementProperties(RsLookupElementProperties.ElementKind.FIELD_DECL);
                }

                @Override
                public LookupElementBuilder createBaseLookupElement(RsCompletionContext ctx) {
                    return LookupElementBuilder
                        .create(finalIndex)
                        .bold()
                        .withTypeText(ty.toString())
                        .withIcon(RsIcons.FIELD);
                }
            };
            elements.add(LookupElements.createLookupElement(entity, completionContext));
        }
        result.addAllElements(elements);
    }
}
