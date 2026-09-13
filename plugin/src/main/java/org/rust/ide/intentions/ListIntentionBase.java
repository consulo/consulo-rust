/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.ext.impl.PsiElementExt;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.Iterator;
import java.util.List;
import org.rust.lang.core.psi.RsTokenType;
import consulo.language.ast.IElementType;
import consulo.localize.LocalizeValue;

public abstract class ListIntentionBase<TList extends RsElement, TElement extends RsElement>
    extends RsElementBaseIntentionAction<TList> {

    private final Class<TList> myListClass;
    private final Class<TElement> myElementClass;

    public ListIntentionBase(
        @Nonnull Class<TList> listClass,
        @Nonnull Class<TElement> elementClass,
        @Nonnull  String intentionText
    ) {
        myListClass = listClass;
        myElementClass = elementClass;
        setText(consulo.localize.LocalizeValue.of(intentionText));
    }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    @Nonnull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Nullable
    protected TList getListContext(@Nonnull PsiElement element) {
        return PsiTreeUtil.getParentOfType(element, myListClass, true);
    }

    @Nonnull
    protected List<PsiElement> getElements(@Nonnull TList context) {
        List<TElement> typed = PsiTreeUtil.getChildrenOfTypeAsList(context, myElementClass);
        return new java.util.ArrayList<PsiElement>(typed);
    }

    @Nonnull
    protected PsiElement getEndElement(@Nonnull TList ctx, @Nonnull PsiElement element) {
        PsiElement comma = commaAfter(element);
        return comma != null ? comma : element;
    }

    protected boolean hasLineBreakAfter(@Nonnull TList ctx, @Nonnull PsiElement element) {
        return nextBreak(getEndElement(ctx, element)) != null;
    }

    @Nullable
    protected PsiWhiteSpace nextBreak(@Nonnull PsiElement element) {
        return lineBreak(PsiElementExt.getRightSiblings(element));
    }

    protected boolean hasLineBreakBefore(@Nonnull PsiElement element) {
        return prevBreak(element) != null;
    }

    @Nullable
    protected PsiWhiteSpace prevBreak(@Nonnull PsiElement element) {
        return lineBreak(PsiElementExt.getLeftSiblings(element));
    }

    protected boolean hasEolComment(@Nonnull PsiElement element) {
        for (PsiComment comment : PsiTreeUtil.findChildrenOfType(element, PsiComment.class)) {
            if (((IElementType) comment.getTokenType()) == RsTokenType.EOL_COMMENT) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private PsiElement commaAfter(@Nonnull PsiElement element) {
        PsiElement next = PsiElementExt.getNextNonCommentSibling(element);
        if (next != null && PsiElementExt.getElementType(next) == RsElementTypes.COMMA) {
            return next;
        }
        return null;
    }

    @Nullable
    private PsiWhiteSpace lineBreak(@Nonnull Iterable<PsiElement> siblings) {
        Iterator<PsiElement> iter = siblings.iterator();
        while (iter.hasNext()) {
            PsiElement it = iter.next();
            if (myElementClass.isInstance(it)) break;
            if (it instanceof PsiWhiteSpace && it.getText().contains("\n")) {
                return (PsiWhiteSpace) it;
            }
        }
        return null;
    }
}
