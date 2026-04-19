/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.presentation.PsiRenderingOptions;
import org.rust.ide.presentation.RsPsiRenderer;
import org.rust.ide.presentation.RsPsiRendererUtil;
import org.rust.ide.refactoring.implementMembers.MembersGenerator;
import org.rust.ide.utils.imports.ImportUtil;
import org.rust.ide.utils.template.EditorExtUtil;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.impl.RsConstantImpl;
import org.rust.lang.core.psi.impl.RsFunctionImpl;
import org.rust.lang.core.psi.impl.RsTypeAliasImpl;
import org.rust.lang.core.resolve.ref.PathPsiSubstUtil;
import org.rust.lang.core.types.RsPsiSubstitution;
import org.rust.openapiext.SmartPointerExtUtil;

import java.util.*;

public class RsImplTraitMemberCompletionProvider extends RsCompletionProvider {
    public static final RsImplTraitMemberCompletionProvider INSTANCE = new RsImplTraitMemberCompletionProvider();

    private static final Map<IElementType, Class<? extends RsAbstractable>> KEYWORD_TO_ABSTRACTABLE;

    static {
        KEYWORD_TO_ABSTRACTABLE = new LinkedHashMap<>();
        KEYWORD_TO_ABSTRACTABLE.put(RsElementTypes.FN, RsFunctionImpl.class);
        KEYWORD_TO_ABSTRACTABLE.put(RsElementTypes.CONST, RsConstantImpl.class);
        KEYWORD_TO_ABSTRACTABLE.put(RsElementTypes.TYPE_KW, RsTypeAliasImpl.class);
    }

    private static final TokenSet KEYWORD_TOKEN_TYPES = TokenSet.orSet(
        RsTokenType.tokenSetOf(RsElementTypes.FN, RsElementTypes.CONST, RsElementTypes.TYPE_KW,
            com.intellij.psi.TokenType.WHITE_SPACE, com.intellij.psi.TokenType.ERROR_ELEMENT),
        RsTokenType.RS_COMMENTS
    );

    private final PsiElementPattern.Capture<PsiElement> myElementPattern;
    private final ElementPattern<PsiElement> myWithoutPrefixPattern;

    private RsImplTraitMemberCompletionProvider() {
        myElementPattern = RsPsiPattern.baseTraitOrImplDeclaration();
        myWithoutPrefixPattern = myElementPattern.and(RsPsiPattern.onStatementBeginning);
    }

    @NotNull
    @Override
    public PsiElementPattern.Capture<PsiElement> getElementPattern() {
        return myElementPattern;
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        PsiElement element = parameters.getPosition();
        RsImplItem implBlock = PsiTreeUtil.getParentOfType(element, RsImplItem.class);
        if (implBlock == null) return;

        RsTraitRef traitRef = implBlock.getTraitRef();
        if (traitRef == null) return;

        org.rust.lang.core.types.BoundElement<RsTraitItem> trait = RsImplItemUtil.getImplementedTrait(implBlock);
        if (trait == null) return;
        RsPsiSubstitution subst = PathPsiSubstUtil.pathPsiSubst(traitRef.getPath(), trait.getTypedElement());

        RsMembers members = trait.getTypedElement().getMembers();
        if (members == null) return;

        Set<RsAbstractable> parentItems = new LinkedHashSet<>(RsMembersUtil.getExpandedMembers(members));
        for (RsAbstractable item : RsMembersUtil.getExpandedMembers(implBlock)) {
            parentItems.removeIf(it -> it.getClass() == item.getClass() && Objects.equals(it.getName(), item.getName()));
        }

        Map.Entry<PsiElement, Class<? extends RsAbstractable>> keyword = null;
        PsiElement previousKeyword = getPreviousKeyword(element);
        if (previousKeyword != null) {
            Class<? extends RsAbstractable> abstractable = KEYWORD_TO_ABSTRACTABLE.get(previousKeyword.getNode().getElementType());
            if (abstractable != null) {
                keyword = new AbstractMap.SimpleEntry<>(previousKeyword, abstractable);
            }
        }

        if (keyword != null) {
            Map.Entry<PsiElement, Class<? extends RsAbstractable>> finalKeyword = keyword;
            parentItems.removeIf(it -> it.getClass() != finalKeyword.getValue());
        } else if (!myWithoutPrefixPattern.accepts(element)) {
            return;
        }

        for (RsAbstractable item : parentItems) {
            MembersGenerator memberGenerator = new MembersGenerator(new RsPsiFactory(element.getProject()), implBlock, trait);
            LookupElementBuilder lookup = getCompletion(item, implBlock, subst, memberGenerator, keyword != null ? keyword.getKey() : null);
            result.addElement(
                LookupElements.toRsLookupElement(lookup, new RsLookupElementProperties(true))
            );
        }
    }

    @Nullable
    private PsiElement getPreviousKeyword(PsiElement element) {
        PsiElement current = element.getPrevSibling();
        while (current != null) {
            if (!KEYWORD_TOKEN_TYPES.contains(current.getNode().getElementType())) {
                return null;
            }
            if (KEYWORD_TO_ABSTRACTABLE.containsKey(current.getNode().getElementType())) {
                return current;
            }
            current = current.getPrevSibling();
        }
        return null;
    }

    private static LookupElementBuilder getCompletion(
        RsAbstractable target,
        RsImplItem impl,
        RsPsiSubstitution substitution,
        MembersGenerator memberGenerator,
        @Nullable PsiElement keyword
    ) {
        if (target instanceof RsConstant) {
            return completeConstant((RsConstant) target, impl, memberGenerator, keyword);
        } else if (target instanceof RsTypeAlias) {
            return completeType((RsTypeAlias) target, memberGenerator, keyword);
        } else if (target instanceof RsFunction) {
            return completeFunction((RsFunction) target, impl, substitution, memberGenerator, keyword);
        }
        throw new IllegalStateException("unreachable");
    }

    private static LookupElementBuilder completeConstant(
        RsConstant target,
        RsImplItem impl,
        MembersGenerator memberGenerator,
        @Nullable PsiElement keyword
    ) {
        String text = removePrefix(memberGenerator.renderAbstractable(target), keyword);
        return LookupElementBuilder.create(text)
            .withIcon(target.getIcon(0))
            .withInsertHandler((ctx, item) -> {
                RsConstant element = LookupElements.getElementOfType(ctx, RsConstant.class);
                if (element == null) return;
                for (Object importCandidate : memberGenerator.getItemsToImport()) {
                    ImportUtil.import_((org.rust.ide.utils.imports.ImportCandidate) importCandidate, impl);
                }
                RsConstant reformatted = reformat(element);
                if (reformatted == null) return;
                RsExpr expr = reformatted.getExpr();
                if (expr == null) return;
                runTemplate(expr, ctx.getEditor());
            });
    }

    private static LookupElementBuilder completeType(
        RsTypeAlias target,
        MembersGenerator memberGenerator,
        @Nullable PsiElement keyword
    ) {
        String text = removePrefix(memberGenerator.renderAbstractable(target), keyword);
        return LookupElementBuilder.create(text)
            .withIcon(target.getIcon(0))
            .withInsertHandler((ctx, item) -> {
                RsTypeAlias element = LookupElements.getElementOfType(ctx, RsTypeAlias.class);
                if (element == null) return;
                RsTypeReference typeRef = element.getTypeReference();
                if (typeRef == null) return;
                runTemplate(typeRef, ctx.getEditor());
            });
    }

    private static LookupElementBuilder completeFunction(
        RsFunction target,
        RsImplItem impl,
        RsPsiSubstitution substitution,
        MembersGenerator memberGenerator,
        @Nullable PsiElement keyword
    ) {
        RsPsiRenderer shortRenderer = new RsPsiRenderer(
            new PsiRenderingOptions(false)
        );
        String shortSignature = removePrefix(RsPsiRendererUtil.renderFunctionSignature(shortRenderer, target), keyword);
        String text = removePrefix(memberGenerator.renderAbstractable(target), keyword);

        return LookupElementBuilder
            .create(text)
            .withIcon(target.getIcon(0))
            .withInsertHandler((ctx, item) -> {
                RsFunction element = LookupElements.getElementOfType(ctx, RsFunction.class);
                if (element == null) return;
                for (Object importCandidate : memberGenerator.getItemsToImport()) {
                    ImportUtil.import_((org.rust.ide.utils.imports.ImportCandidate) importCandidate, impl);
                }
                RsFunction reformatted = reformat(element);
                if (reformatted == null) return;
                RsBlock block = RsFunctionUtil.getBlock(reformatted);
                if (block == null) return;
                RsExprStmt tailStmt = RsBlockUtil.getSyntaxTailStmt(block);
                if (tailStmt == null) return;
                RsExpr body = tailStmt.getExpr();
                runTemplate(body, ctx.getEditor());
            })
            .withPresentableText(shortSignature + " { ... }");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T extends RsElement> T reformat(T element) {
        com.intellij.psi.SmartPsiElementPointer<T> ptr = SmartPointerExtUtil.createSmartPointer(element);
        CodeStyleManager.getInstance(element.getProject()).reformatText(
            element.getContainingFile(),
            element.getTextRange().getStartOffset(),
            element.getTextRange().getEndOffset()
        );
        return ptr.getElement();
    }

    private static void runTemplate(RsElement element, Editor editor) {
        EditorExtUtil.buildAndRunTemplate(editor, element.getParent(), Collections.singletonList(element));
    }

    private static String removePrefix(String text, @Nullable PsiElement keyword) {
        if (keyword != null) {
            String keywordText = keyword.getText();
            if (text.startsWith(keywordText)) {
                return text.substring(keywordText.length()).stripLeading();
            }
        }
        return text;
    }
}
