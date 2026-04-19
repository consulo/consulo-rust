/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.rust.RsBundle;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

import java.util.List;

public abstract class ChopListIntentionBase<TList extends RsElement, TElement extends RsElement>
    extends ListIntentionBase<TList, TElement> {

    public ChopListIntentionBase(Class<TList> listClass, Class<TElement> elementClass, String intentionText) {
        super(listClass, elementClass, intentionText);
    }

    @Override
    public TList findApplicableContext(Project project, Editor editor, PsiElement element) {
        TList list = getListContext(element);
        if (list == null) return null;
        List<? extends PsiElement> elements = getElements(list);
        if (elements.size() < 2) return null;
        boolean allHaveLineBreaks = true;
        for (int i = 0; i < elements.size() - 1; i++) {
            if (!hasLineBreakAfter(list, elements.get(i))) {
                allHaveLineBreaks = false;
                break;
            }
        }
        if (allHaveLineBreaks) return null;
        if (!PsiModificationUtil.canReplace(list)) return null;
        return list;
    }

    @Override
    public void invoke(Project project, Editor editor, TList ctx) {
        Document document = editor.getDocument();
        int startOffset = ctx.getTextRange().getStartOffset();

        List<? extends PsiElement> elements = getElements(ctx);
        PsiElement last = elements.get(elements.size() - 1);
        if (!hasLineBreakAfter(ctx, last)) {
            PsiElement endElement = getEndElement(ctx, last);
            if (endElement.getTextRange() != null) {
                document.insertString(endElement.getTextRange().getEndOffset(), "\n");
            }
        }

        for (int i = elements.size() - 1; i >= 0; i--) {
            PsiElement el = elements.get(i);
            if (!hasLineBreakBefore(el)) {
                document.insertString(el.getTextRange().getStartOffset(), "\n");
            }
        }

        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        documentManager.commitDocument(document);
        PsiFile psiFile = documentManager.getPsiFile(document);
        if (psiFile != null) {
            PsiElement found = psiFile.findElementAt(startOffset);
            if (found != null) {
                TList listCtx = getListContext(found);
                if (listCtx != null) {
                    CodeStyleManager.getInstance(project).adjustLineIndent(psiFile, listCtx.getTextRange());
                }
            }
        }
    }
}
