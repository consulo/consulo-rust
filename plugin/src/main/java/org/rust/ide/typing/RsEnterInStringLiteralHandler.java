/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import consulo.language.editor.action.EnterHandlerDelegate;
import consulo.language.editor.action.EnterHandlerDelegateAdapter;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.HighlighterIterator;
import consulo.util.lang.ref.Ref;
import consulo.language.psi.PsiFile;
import consulo.language.ast.StringEscapesTokenTypes;
import org.rust.lang.core.psi.RsTokenType;
import org.rust.lang.core.psi.RsFile;

import java.util.regex.Pattern;

public class RsEnterInStringLiteralHandler extends EnterHandlerDelegateAdapter {

    private static final Pattern UNESCAPED_NEWLINE = Pattern.compile("[^\\\\]\n");

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
        if (StringEscapesTokenTypes.STRING_LITERAL_ESCAPES.contains(((consulo.language.ast.IElementType) iterator.getTokenType()))) {
            if (caretOffset == iterator.getStart()) {
                iterator.retreat();
            } else {
                return Result.Continue;
            }
        }

        if (RsTokenType.RS_STRING_LITERALS.contains(((consulo.language.ast.IElementType) iterator.getTokenType()))) {
            CharSequence tokenText = editor.getDocument().getImmutableCharSequence().subSequence(
                iterator.getStart(), iterator.getEnd()
            );
            if (UNESCAPED_NEWLINE.matcher(tokenText).find()) return Result.Continue;

            editor.getDocument().insertString(caretOffset, "\\");
            caretOffsetRef.set(caretOffset + 1);
            return Result.DefaultForceIndent;
        }

        if (RsTokenType.RS_RAW_LITERALS.contains(((consulo.language.ast.IElementType) iterator.getTokenType()))) {
            return Result.DefaultSkipIndent;
        }

        return Result.Continue;
    }
}
