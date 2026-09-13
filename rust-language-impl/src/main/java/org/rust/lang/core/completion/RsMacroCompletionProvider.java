/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionUtilCore;
import consulo.util.dataholder.Key;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.ast.IElementType;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.impl.RsExpressionCodeFragment;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.psi.ext.*;

import java.util.ArrayList;
import java.util.List;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;
import org.rust.lang.core.RsPsiPatternUtil;
import org.rust.lang.core.psi.ext.impl.*;

/**
 * This completion provider is used only in the case of incomplete macro call outside of function or other code block.
 */
public class RsMacroCompletionProvider extends RsCompletionProvider {
    public static final RsMacroCompletionProvider INSTANCE = new RsMacroCompletionProvider();

    private static final int MAXIMUM_SUPPORTED_SEGMENTS = 10;

    private RsMacroCompletionProvider() {
    }

    @Override
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
    ) {
        PsiElement position = parameters.getPosition();
        RsElement rsElement = RsElementUtil.ancestorStrict(position, RsElement.class);
        if (rsElement == null) return;
        RsMod mod = RsElementUtil.ancestorOrSelf(rsElement, RsMod.class);
        if (mod == null) return;
        if (mod instanceof RsModItem && ((RsModItem) mod).getIdentifier() == position) return;

        List<PsiElement> leftSiblings = new ArrayList<>();
        PsiElement sibling = position.getPrevSibling();
        while (sibling != null) {
            if (!(sibling instanceof PsiWhiteSpace) && !(sibling instanceof PsiComment) && !(sibling instanceof PsiErrorElement)) {
                IElementType type = sibling.getNode().getElementType();
                if (type != RsElementTypes.IDENTIFIER && type != RsElementTypes.COLONCOLON) break;
                leftSiblings.add(sibling);
            }
            sibling = sibling.getPrevSibling();
        }

        if (leftSiblings.size() > MAXIMUM_SUPPORTED_SEGMENTS * 2) return;

        StringBuilder leftSiblingsText = new StringBuilder();
        for (int i = leftSiblings.size() - 1; i >= 0; i--) {
            leftSiblingsText.append(leftSiblings.get(i).getText());
        }

        if (leftSiblings.isEmpty() && CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED.equals(position.getText())) return;

        String text = leftSiblingsText.toString() + position.getText() + "!()";
        RsExpressionCodeFragment fragment = new RsExpressionCodeFragment(position.getProject(), text, mod);
        fragment.putUserData(FORCE_OUT_OF_SCOPE_COMPLETION, true);

        int offset = leftSiblingsText.length() + (parameters.getOffset() - position.getTextRange().getStartOffset());
        PsiElement element = fragment.findElementAt(offset);
        if (element == null) return;
        Utils.rerunCompletion(parameters.withPosition(element, offset), result);
    }

    @Nonnull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        ElementPattern<RsItemElement> incompleteItem = psiElement(RsItemElement.class).withLastChild(RsPsiPattern.error);
        return PlatformPatterns.psiElement(RsElementTypes.IDENTIFIER)
            .withLanguage(RsLanguage.INSTANCE)
            .andNot(org.rust.lang.core.RsPsiPatternUtil.withPrevSiblingSkipping(PlatformPatterns.psiElement(), RsPsiPattern.whitespace, incompleteItem))
            .withParent(psiElement(RsMod.class));
    }

    public static final Key<Boolean> FORCE_OUT_OF_SCOPE_COMPLETION = Key.create("FORCE_OUT_OF_SCOPE_COMPLETION");
}
