/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate;
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import com.intellij.psi.StringEscapesTokenTypes;
import org.rust.lang.core.psi.RsTokenType;
import org.rust.lang.core.psi.RsFile;

import java.util.regex.Pattern;

public class RsEnterInStringLiteralHandler extends EnterHandlerDelegateAdapter {

    private static final Pattern UNESCAPED_NEWLINE = Pattern.compile("[^\\\\]\n");

    @Override
    public Result preprocessEnter(
        PsiFile file,
        Editor editor,
        Ref<Integer> caretOffsetRef,
        Ref<Integer> caretAdvanceRef,
        DataContext dataContext,
        EditorActionHandler originalHandler
    ) {
        if (!(file instanceof RsFile)) return Result.Continue;

        int caretOffset = caretOffsetRef.get();
        if (!RsTypingUtils.isValidInnerOffset(caretOffset, editor.getDocument().getCharsSequence())) {
            return Result.Continue;
        }

        HighlighterIterator iterator = ((EditorEx) editor).getHighlighter().createIterator(caretOffset);

        // Return if we are not inside literal contents (i.e. in prefix, suffix or delimiters)
        if (!new RsQuoteHandler().isDeepInsideLiteral(iterator, caretOffset)) return Result.Continue;

        // Return if we are inside escape sequence
        if (StringEscapesTokenTypes.STRING_LITERAL_ESCAPES.contains(iterator.getTokenType())) {
            if (caretOffset == iterator.getStart()) {
                iterator.retreat();
            } else {
                return Result.Continue;
            }
        }

        if (RsTokenType.RS_STRING_LITERALS.contains(iterator.getTokenType())) {
            CharSequence tokenText = editor.getDocument().getImmutableCharSequence().subSequence(
                iterator.getStart(), iterator.getEnd()
            );
            if (UNESCAPED_NEWLINE.matcher(tokenText).find()) return Result.Continue;

            editor.getDocument().insertString(caretOffset, "\\");
            caretOffsetRef.set(caretOffset + 1);
            return Result.DefaultForceIndent;
        }

        if (RsTokenType.RS_RAW_LITERALS.contains(iterator.getTokenType())) {
            return Result.DefaultSkipIndent;
        }

        return Result.Continue;
    }
}
