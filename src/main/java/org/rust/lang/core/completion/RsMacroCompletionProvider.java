/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.openapi.util.Key;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsExpressionCodeFragment;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.psi.ext.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;

/**
 * This completion provider is used only in the case of incomplete macro call outside of function or other code block.
 */
public class RsMacroCompletionProvider extends RsCompletionProvider {
    public static final RsMacroCompletionProvider INSTANCE = new RsMacroCompletionProvider();

    private static final int MAXIMUM_SUPPORTED_SEGMENTS = 10;

    private RsMacroCompletionProvider() {
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
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

        if (leftSiblings.isEmpty() && CompletionUtil.DUMMY_IDENTIFIER_TRIMMED.equals(position.getText())) return;

        String text = leftSiblingsText.toString() + position.getText() + "!()";
        RsExpressionCodeFragment fragment = new RsExpressionCodeFragment(position.getProject(), text, mod);
        fragment.putUserData(FORCE_OUT_OF_SCOPE_COMPLETION, true);

        int offset = leftSiblingsText.length() + (parameters.getOffset() - position.getTextRange().getStartOffset());
        PsiElement element = fragment.findElementAt(offset);
        if (element == null) return;
        Utils.rerunCompletion(parameters.withPosition(element, offset), result);
    }

    @NotNull
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
