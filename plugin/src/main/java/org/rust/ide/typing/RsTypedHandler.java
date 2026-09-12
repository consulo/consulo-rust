/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.action.TypedHandlerDelegate;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.lang.CharArrayUtil;
import org.rust.lang.core.psi.RsDotExpr;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsPat;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;

@ExtensionImpl(id = "RsTypedHandler")
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
        consulo.language.psi.PsiElement currElement = file.findElementAt(offset - 1);
        if (currElement == null) return false;
        consulo.language.psi.PsiElement prevLeaf = PsiTreeUtil.prevLeaf(currElement);
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
                consulo.language.psi.PsiElement leaf = f.findElementAt(offset - 1);
                return leaf != null && consulo.language.psi.PsiUtilCore.getElementType(leaf) == RsElementTypes.COLONCOLON;
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
                    consulo.language.psi.PsiElement leaf = f.findElementAt(offset);
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
                consulo.language.psi.PsiElement leaf = f.findElementAt(offset);
                if (leaf == null) return false;
                consulo.language.ast.IElementType type = consulo.language.psi.PsiUtilCore.getElementType(leaf);
                return type == RsElementTypes.INTEGER_LITERAL || type == RsElementTypes.FLOAT_LITERAL;
            });
        }

        if (charTyped == '%') {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor, CompletionType.BASIC, f -> {
                consulo.language.psi.PsiElement leaf = f.findElementAt(offset);
                return leaf != null && consulo.language.psi.PsiUtilCore.getElementType(leaf) == RsElementTypes.STRING_LITERAL;
            });
        }

        return Result.CONTINUE;
    }
}
