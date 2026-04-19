/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate;
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.util.text.CharArrayUtil;
import org.rust.lang.core.parser.RustParserDefinition;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.doc.psi.RsDocKind;
import org.rust.lang.doc.psi.ext.RsDocCommentUtil;

public class RsEnterInLineCommentHandler extends EnterHandlerDelegateAdapter {

    @Override
    public Result preprocessEnter(
        PsiFile file,
        Editor editor,
        Ref<Integer> caretOffsetRef,
        Ref<Integer> caretAdvanceRef,
        DataContext dataContext,
        EditorActionHandler originalHandler
    ) {
        // return if this is not a Rust file
        if (!(file instanceof RsFile)) {
            return Result.Continue;
        }

        // get current document and commit any changes, so we'll get latest PSI
        com.intellij.openapi.editor.Document document = editor.getDocument();
        PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);

        int caretOffset = caretOffsetRef.get();
        CharSequence text = document.getCharsSequence();

        // skip following spaces and tabs
        int offset = CharArrayUtil.shiftForward(text, caretOffset, " \t");

        // figure out if the caret is at the end of the line
        boolean isEOL = offset < text.length() && text.charAt(offset) == '\n';

        // find the PsiElement at the caret
        PsiElement elementAtCaret = file.findElementAt(offset);
        if (elementAtCaret == null) return Result.Continue;
        if (isEOL && isEolWhitespace(elementAtCaret, offset)) {
            // ... or the previous one if this is end-of-line whitespace
            elementAtCaret = elementAtCaret.getPrevSibling();
            if (elementAtCaret == null) return Result.Continue;
        }

        PsiElement containingDoc = RsDocCommentUtil.getContainingDoc(elementAtCaret);
        if (containingDoc != null) {
            elementAtCaret = containingDoc;
        }

        // check if the element at the caret is a line comment
        // and extract the comment token (//, /// or //!) from the comment text
        com.intellij.psi.tree.IElementType elementType = RsElementUtil.getElementType(elementAtCaret);
        String prefix;
        if (elementType == RustParserDefinition.OUTER_EOL_DOC_COMMENT) {
            prefix = RsDocKind.OuterEol.getPrefix();
        } else if (elementType == RustParserDefinition.INNER_EOL_DOC_COMMENT) {
            prefix = RsDocKind.InnerEol.getPrefix();
        } else if (elementType == RustParserDefinition.EOL_COMMENT) {
            // return if caret is at end of line for a non-documentation comment
            if (isEOL) {
                return Result.Continue;
            }
            prefix = "//";
        } else {
            return Result.Continue;
        }

        // If caret is currently inside some prefix, do nothing.
        if (offset < elementAtCaret.getTextOffset() + prefix.length()) {
            return Result.Continue;
        }

        if (startsWith(text, offset, prefix)) {
            int afterPrefix = offset + prefix.length();
            if (afterPrefix < document.getTextLength() && text.charAt(afterPrefix) != ' ') {
                document.insertString(afterPrefix, " ");
            }
            caretOffsetRef.set(offset);
        } else {
            String prefixToAdd = (text.charAt(caretOffset) != ' ') ? prefix + " " : prefix;
            document.insertString(caretOffset, prefixToAdd);
            caretAdvanceRef.set(prefixToAdd.length());
        }

        return Result.Default;
    }

    private static boolean startsWith(CharSequence text, int offset, String prefix) {
        if (offset + prefix.length() > text.length()) return false;
        for (int i = 0; i < prefix.length(); i++) {
            if (text.charAt(offset + i) != prefix.charAt(i)) return false;
        }
        return true;
    }

    private static boolean isEolWhitespace(PsiElement element, int caretOffset) {
        if (element.getNode() == null || element.getNode().getElementType() != TokenType.WHITE_SPACE) return false;
        String nodeText = element.getNode().getText();
        int pos = nodeText.indexOf('\n');
        return pos == -1 || caretOffset <= pos + element.getTextRange().getStartOffset();
    }
}
