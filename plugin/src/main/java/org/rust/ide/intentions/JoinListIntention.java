/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.List;

abstract class JoinListIntentionBase<TList extends RsElement, TElement extends RsElement>
    extends ListIntentionBase<TList, TElement> {

    private final String myPrefix;
    private final String mySuffix;

    public JoinListIntentionBase(
        @Nonnull Class<TList> listClass,
        @Nonnull Class<TElement> elementClass,
        @Nonnull  String intentionText,
        @Nonnull String prefix,
        @Nonnull String suffix
    ) {
        super(listClass, elementClass, intentionText);
        myPrefix = prefix;
        mySuffix = suffix;
    }

    public JoinListIntentionBase(
        @Nonnull Class<TList> listClass,
        @Nonnull Class<TElement> elementClass,
        @Nonnull  String intentionText
    ) {
        this(listClass, elementClass, intentionText, "", "");
    }

    @Nullable
    @Override
    public TList findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        TList list = getListContext(element);
        if (list == null) return null;
        List<PsiElement> elements = getElements(list);
        if (elements.isEmpty()) return null;
        boolean firstHasBreak = hasLineBreakBefore(elements.get(0));
        boolean anyHasBreakAfter = false;
        for (PsiElement el : elements) {
            if (hasLineBreakAfter(list, el)) {
                anyHasBreakAfter = true;
                break;
            }
        }
        if (!firstHasBreak && !anyHasBreakAfter) return null;
        if (hasEolComment(list)) return null;
        if (!PsiModificationUtil.canReplace(list)) return null;
        return list;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull TList ctx) {
        Document document = editor.getDocument();
        List<PsiElement> elements = getElements(ctx);

        PsiElement last = getEndElement(ctx, elements.get(elements.size() - 1));
        PsiWhiteSpace nextBr = nextBreak(last);
        if (nextBr != null) {
            replaceWhiteSpace(nextBr, myPrefix, document);
        }

        for (int i = elements.size() - 2; i >= 0; i--) {
            PsiWhiteSpace br = nextBreak(elements.get(i));
            if (br != null) {
                replaceWhiteSpace(br, " ", document);
            }
        }

        PsiWhiteSpace prevBr = prevBreak(elements.get(0));
        if (prevBr != null) {
            replaceWhiteSpace(prevBr, mySuffix, document);
        }
    }

    private static void replaceWhiteSpace(@Nonnull PsiWhiteSpace ws, @Nonnull String replaceString, @Nonnull Document document) {
        int startOffset = ws.getTextRange().getStartOffset();
        int endOffset = ws.getTextRange().getEndOffset();
        if (replaceString.isEmpty()) {
            document.deleteString(startOffset, endOffset);
        } else {
            document.replaceString(startOffset, endOffset, replaceString);
        }
    }
}
