/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.intentions.util.macros.IntentionInMacroUtil;
import org.rust.ide.intentions.util.macros.RsIntentionInsideMacroExpansionEditor;

public final class EditorExt {
    private EditorExt() {
    }

    public static void moveCaretToOffset(@NotNull Editor editor, @NotNull PsiElement context, int absoluteOffsetInFile) {
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

    public static void setSelection(@NotNull Editor editor, @NotNull PsiElement context, int startOffset, int endOffset) {
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

    public static void showErrorHint(@NotNull Editor editor, @NotNull @NlsContexts.HintText String text, short position) {
        Editor unwrapped = IntentionInMacroUtil.unwrapEditor(editor);
        HintManager.getInstance().showErrorHint(unwrapped, text, position);
    }

    public static void showErrorHint(@NotNull Editor editor, @NotNull @NlsContexts.HintText String text) {
        Editor unwrapped = IntentionInMacroUtil.unwrapEditor(editor);
        HintManager.getInstance().showErrorHint(unwrapped, text);
    }
}
