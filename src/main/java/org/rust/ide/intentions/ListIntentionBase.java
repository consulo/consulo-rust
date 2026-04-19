/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.codeInspection.util.IntentionName;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.parser.RustParserDefinition;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.Iterator;
import java.util.List;

public abstract class ListIntentionBase<TList extends RsElement, TElement extends RsElement>
    extends RsElementBaseIntentionAction<TList> {

    private final Class<TList> myListClass;
    private final Class<TElement> myElementClass;

    public ListIntentionBase(
        @NotNull Class<TList> listClass,
        @NotNull Class<TElement> elementClass,
        @NotNull @IntentionName String intentionText
    ) {
        myListClass = listClass;
        myElementClass = elementClass;
        setText(intentionText);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @NotNull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Nullable
    protected TList getListContext(@NotNull PsiElement element) {
        return PsiTreeUtil.getParentOfType(element, myListClass, true);
    }

    @NotNull
    protected List<PsiElement> getElements(@NotNull TList context) {
        return PsiTreeUtil.getChildrenOfTypeAsList(context, myElementClass);
    }

    @NotNull
    protected PsiElement getEndElement(@NotNull TList ctx, @NotNull PsiElement element) {
        PsiElement comma = commaAfter(element);
        return comma != null ? comma : element;
    }

    protected boolean hasLineBreakAfter(@NotNull TList ctx, @NotNull PsiElement element) {
        return nextBreak(getEndElement(ctx, element)) != null;
    }

    @Nullable
    protected PsiWhiteSpace nextBreak(@NotNull PsiElement element) {
        return lineBreak(PsiElementExt.getRightSiblings(element));
    }

    protected boolean hasLineBreakBefore(@NotNull PsiElement element) {
        return prevBreak(element) != null;
    }

    @Nullable
    protected PsiWhiteSpace prevBreak(@NotNull PsiElement element) {
        return lineBreak(PsiElementExt.getLeftSiblings(element));
    }

    protected boolean hasEolComment(@NotNull PsiElement element) {
        for (PsiComment comment : PsiTreeUtil.findChildrenOfType(element, PsiComment.class)) {
            if (comment.getTokenType() == RustParserDefinition.EOL_COMMENT) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private PsiElement commaAfter(@NotNull PsiElement element) {
        PsiElement next = PsiElementExt.getNextNonCommentSibling(element);
        if (next != null && PsiElementExt.getElementType(next) == RsElementTypes.COMMA) {
            return next;
        }
        return null;
    }

    @Nullable
    private PsiWhiteSpace lineBreak(@NotNull Iterable<PsiElement> siblings) {
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
