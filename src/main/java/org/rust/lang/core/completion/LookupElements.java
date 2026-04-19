/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.editorActions.TabOutScopesTracker;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.icons.RsIcons;
import org.rust.ide.presentation.RsPsiRendererUtil;
import org.rust.ide.refactoring.RsNamesValidator;
import org.rust.ide.utils.imports.ImportCandidate;
import org.rust.ide.utils.imports.ImportUtil;
import org.rust.ide.settings.RsCodeInsightSettings;
import org.rust.lang.core.completion.RsLookupElementProperties.KeywordKind;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.*;
import org.rust.lang.core.resolve.ref.FieldResolveVariant;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.SubstitutionUtil;
import org.rust.lang.core.types.infer.ExpectedType;
import org.rust.lang.core.types.infer.RsInferenceContext;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.ty.*;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.doc.psi.RsDocPathLinkParent;
import org.rust.openapiext.Testmark;
import org.rust.stdext.StdextUtil;

import java.util.*;

public final class LookupElements {
    public static final double DEFAULT_PRIORITY = 0.0;

    private LookupElements() {
    }

    public static LookupElement createLookupElement(
        ScopeEntry scopeEntry,
        RsCompletionContext context
    ) {
        return createLookupElement(scopeEntry, context, null, new RsDefaultInsertHandler());
    }

    public static LookupElement createLookupElement(
        ScopeEntry scopeEntry,
        RsCompletionContext context,
        @Nullable String locationString,
        InsertHandler<LookupElement> insertHandler
    ) {
        CompletionEntity completionEntity = new ScopedBaseCompletionEntity(scopeEntry);
        return createLookupElement(completionEntity, context, locationString, insertHandler);
    }

    public static LookupElement createLookupElement(
        CompletionEntity completionEntity,
        RsCompletionContext context
    ) {
        return createLookupElement(completionEntity, context, null, new RsDefaultInsertHandler());
    }

    public static LookupElement createLookupElement(
        CompletionEntity completionEntity,
        RsCompletionContext context,
        @Nullable String locationString,
        InsertHandler<LookupElement> insertHandler
    ) {
        LookupElementBuilder lookup = completionEntity.createBaseLookupElement(context)
            .withInsertHandler(insertHandler);
        if (locationString != null) {
            lookup = lookup.appendTailText(" (" + locationString + ")", true);
        }

        ImplLookup implLookup = context.getLookup();
        boolean isCompatibleTypes = implLookup != null
            && isCompatibleTypes(implLookup, completionEntity.retTy(implLookup.getItems()), context.getExpectedTy());

        RsLookupElementProperties properties = completionEntity.getBaseLookupElementProperties(context)
            .withReturnTypeConformsToExpectedType(isCompatibleTypes);

        return toRsLookupElement(lookup, properties);
    }

    public static LookupElement withPriority(LookupElementBuilder builder, double priority) {
        if (priority == DEFAULT_PRIORITY) return builder;
        return PrioritizedLookupElement.withPriority(builder, priority);
    }

    public static LookupElement toRsLookupElement(LookupElementBuilder builder, RsLookupElementProperties properties) {
        return new RsLookupElement(builder, properties);
    }

    public static LookupElement toKeywordElement(LookupElementBuilder builder) {
        return toKeywordElement(builder, KeywordKind.KEYWORD);
    }

    public static LookupElement toKeywordElement(LookupElementBuilder builder, KeywordKind keywordKind) {
        return toRsLookupElement(builder, new RsLookupElementProperties().withKeywordKind(keywordKind));
    }

    public static Substitution getSubstitution(RsInferenceContext ctx, ScopeEntry scopeEntry) {
        if (scopeEntry instanceof AssocItemScopeEntryBase) {
            Substitution subst = ctx.instantiateMethodOwnerSubstitution((AssocItemScopeEntryBase<?>) scopeEntry);
            return subst.mapTypeValues(entry -> ctx.resolveTypeVarsIfPossible(entry.getValue()))
                .mapConstValues(entry -> ctx.resolveTypeVarsIfPossible(entry.getValue()));
        } else if (scopeEntry instanceof FieldResolveVariant) {
            return ((FieldResolveVariant) scopeEntry).getSelfTy().getTypeParameterValues();
        } else {
            return SubstitutionUtil.emptySubstitution();
        }
    }

    public static boolean nextCharIs(InsertionContext ctx, char c) {
        return indexOfSkippingSpace(ctx.getDocument().getCharsSequence(), c, ctx.getTailOffset()) != null;
    }

    public static boolean alreadyHasCallParens(InsertionContext ctx) {
        return nextCharIs(ctx, '(');
    }

    @Nullable
    private static Integer indexOfSkippingSpace(CharSequence seq, char c, int startIndex) {
        for (int i = startIndex; i < seq.length(); i++) {
            char currentChar = seq.charAt(i);
            if (c == currentChar) return i;
            if (currentChar != ' ' && currentChar != '\t') return null;
        }
        return null;
    }

    @Nullable
    public static <T extends PsiElement> T getElementOfType(InsertionContext ctx, Class<T> clazz) {
        return PsiTreeUtil.findElementOfClassAtOffset(ctx.getFile(), ctx.getTailOffset() - 1, clazz, false);
    }

    public static LookupElement withImportCandidate(LookupElement element, ImportCandidate candidate) {
        return new RsImportLookupElement(element, candidate);
    }

    public static void importInContext(InsertionContext ctx, ImportCandidate candidate) {
        if (RsCodeInsightSettings.getInstance().importOutOfScopeItems) {
            ctx.commitDocument();
            RsElement rsElement = getElementOfType(ctx, RsElement.class);
            if (rsElement != null) {
                ImportUtil.import_(candidate, rsElement);
            }
        }
    }

    private static boolean isCompatibleTypes(ImplLookup lookup, @Nullable Ty actualTy, @Nullable ExpectedType expectedType) {
        if (actualTy == null || expectedType == null) return false;
        Ty expectedTy = expectedType.getTy();
        if (actualTy instanceof TyUnknown || expectedTy instanceof TyUnknown
            || actualTy instanceof TyNever || expectedTy instanceof TyNever
            || actualTy instanceof TyTypeParameter || expectedTy instanceof TyTypeParameter) {
            return false;
        }

        TypeFolder folder = new TypeFolder() {
            @Override
            public Ty foldTy(Ty ty) {
                if (ty instanceof TyUnknown) return TyNever.INSTANCE;
                if (ty instanceof TyTypeParameter) return TyNever.INSTANCE;
                return ty.superFoldWith(this);
            }
        };

        Ty ty1 = actualTy.foldWith(folder);
        Ty ty2 = expectedTy.foldWith(folder);
        if (expectedType.isCoercable()) {
            return lookup.getCtx().tryCoerce(ty1, ty2).isOk();
        } else {
            return lookup.getCtx().combineTypesNoVars(ty1, ty2).isOk();
        }
    }

    public static final class Testmarks {
        public static final Testmark DoNotAddOpenParenCompletionChar = new Testmark();
    }
}
