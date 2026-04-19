/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.text.CharArrayUtil;
import org.rust.lang.core.psi.RsDotExpr;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsPat;

public class RsTypedHandler extends TypedHandlerDelegate {

    @Override
    public Result charTyped(char c, Project project, Editor editor, PsiFile file) {
        if (!(file instanceof RsFile)) return Result.CONTINUE;
        if (c != '.') return Result.CONTINUE;

        int offset = editor.getCaretModel().getOffset();
        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
        if (indentDotIfNeeded(project, (RsFile) file, offset)) return Result.STOP;

        return Result.CONTINUE;
    }

    private boolean indentDotIfNeeded(Project project, RsFile file, int offset) {
        com.intellij.psi.PsiElement currElement = file.findElementAt(offset - 1);
        if (currElement == null) return false;
        com.intellij.psi.PsiElement prevLeaf = PsiTreeUtil.prevLeaf(currElement);
        if (!(prevLeaf instanceof PsiWhiteSpace && prevLeaf.getText().contains("\n"))) return false;
        if (!(currElement.getParent() instanceof RsDotExpr)) return false;
        int curElementLength = currElement.getText().length();
        if (offset < curElementLength) return false;
        CodeStyleManager.getInstance(project).adjustLineIndent(file, offset - curElementLength);
        return true;
    }

    @Override
    public Result checkAutoPopup(char charTyped, Project project, Editor editor, PsiFile file) {
        if (!(file instanceof RsFile)) return Result.CONTINUE;

        int offset = editor.getCaretModel().getOffset();

        // `:` is typed right after `:`
        if (charTyped == ':' && StringUtil.endsWith(editor.getDocument().getImmutableCharSequence(), 0, offset, ":")) {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor, CompletionType.BASIC, f -> {
                com.intellij.psi.PsiElement leaf = f.findElementAt(offset - 1);
                return leaf != null && com.intellij.psi.util.PsiUtilCore.getElementType(leaf) == RsElementTypes.COLONCOLON;
            });
            return Result.STOP;
        }

        // `|` is typed after `(` or `,` or `=` (ignoring whitespace) - perform lambda expr completion
        if (charTyped == '|') {
            int i = CharArrayUtil.shiftBackward(editor.getDocument().getImmutableCharSequence(), 0, offset - 1, " \n") + 1;
            boolean shouldShowPopup = StringUtil.endsWith(editor.getDocument().getImmutableCharSequence(), 0, i, "(")
                || StringUtil.endsWith(editor.getDocument().getImmutableCharSequence(), 0, i, ",")
                || StringUtil.endsWith(editor.getDocument().getImmutableCharSequence(), 0, i, "=");
            if (shouldShowPopup) {
                AutoPopupController.getInstance(project).scheduleAutoPopup(editor, CompletionType.BASIC, f -> {
                    com.intellij.psi.PsiElement leaf = f.findElementAt(offset);
                    return leaf == null || !(leaf.getParent() instanceof RsPat);
                });
                return Result.STOP;
            }
        }

        // struct literal `Foo {/*caret*/}`
        if (charTyped == '{') {
            AutoPopupController.getInstance(project).autoPopupParameterInfo(editor, null);
            return Result.STOP;
        }

        if (charTyped == 'i' || charTyped == 'u' || charTyped == 'f') {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor, CompletionType.BASIC, f -> {
                com.intellij.psi.PsiElement leaf = f.findElementAt(offset);
                if (leaf == null) return false;
                com.intellij.psi.tree.IElementType type = com.intellij.psi.util.PsiUtilCore.getElementType(leaf);
                return type == RsElementTypes.INTEGER_LITERAL || type == RsElementTypes.FLOAT_LITERAL;
            });
        }

        if (charTyped == '%') {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor, CompletionType.BASIC, f -> {
                com.intellij.psi.PsiElement leaf = f.findElementAt(offset);
                return leaf != null && com.intellij.psi.util.PsiUtilCore.getElementType(leaf) == RsElementTypes.STRING_LITERAL;
            });
        }

        return Result.CONTINUE;
    }
}
