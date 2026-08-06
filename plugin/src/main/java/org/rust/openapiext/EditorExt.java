/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.language.editor.hint.HintManager;
import consulo.codeEditor.Editor;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.ide.intentions.util.macros.IntentionInMacroUtil;
import org.rust.ide.intentions.util.macros.RsIntentionInsideMacroExpansionEditor;

public final class EditorExt {
    private EditorExt() {
    }

    public static void moveCaretToOffset(@Nonnull Editor editor, @Nonnull PsiElement context, int absoluteOffsetInFile) {
        Editor targetEditor;
        if (editor instanceof RsIntentionInsideMacroExpansionEditor) {
            RsIntentionInsideMacroExpansionEditor macroEditor = (RsIntentionInsideMacroExpansionEditor) editor;
            if (macroEditor.getOriginalFile() == context.getContainingFile()) {
                targetEditor = macroEditor.getOriginalEditor();
            } else {
                targetEditor = editor;
            }
        } else {
            targetEditor = editor;
        }
        targetEditor.getCaretModel().moveToOffset(absoluteOffsetInFile);
    }

    public static void setSelection(@Nonnull Editor editor, @Nonnull PsiElement context, int startOffset, int endOffset) {
        Editor targetEditor;
        if (editor instanceof RsIntentionInsideMacroExpansionEditor) {
            RsIntentionInsideMacroExpansionEditor macroEditor = (RsIntentionInsideMacroExpansionEditor) editor;
            if (macroEditor.getOriginalFile() == context.getContainingFile()) {
                targetEditor = macroEditor.getOriginalEditor();
            } else {
                targetEditor = editor;
            }
        } else {
            targetEditor = editor;
        }
        targetEditor.getSelectionModel().setSelection(startOffset, endOffset);
    }

    public static void showErrorHint(@Nonnull Editor editor, @Nonnull  String text, short position) {
        Editor unwrapped = IntentionInMacroUtil.unwrapEditor(editor);
        HintManager.getInstance().showErrorHint(unwrapped, text, position);
    }

    public static void showErrorHint(@Nonnull Editor editor, @Nonnull  String text) {
        Editor unwrapped = IntentionInMacroUtil.unwrapEditor(editor);
        HintManager.getInstance().showErrorHint(unwrapped, text);
    }
}
