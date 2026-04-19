/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.intentions.util.macros.IntentionInMacroUtil;
import org.rust.ide.intentions.util.macros.RsIntentionInsideMacroExpansionEditor;

public final class Editor {
    private Editor() {
    }

    public static void moveCaretToOffset(@NotNull com.intellij.openapi.editor.Editor editor,
                                         @NotNull PsiElement context,
                                         int absoluteOffsetInFile) {
        com.intellij.openapi.editor.Editor targetEditor;
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

    public static void setSelection(@NotNull com.intellij.openapi.editor.Editor editor,
                                    @NotNull PsiElement context,
                                    int startOffset,
                                    int endOffset) {
        com.intellij.openapi.editor.Editor targetEditor;
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

    public static void showErrorHint(@NotNull com.intellij.openapi.editor.Editor editor,
                                     @NotNull @NlsContexts.HintText String text,
                                     short position) {
        com.intellij.openapi.editor.Editor unwrapped = IntentionInMacroUtil.unwrapEditor(editor);
        HintManager.getInstance().showErrorHint(unwrapped, text, position);
    }

    public static void showErrorHint(@NotNull com.intellij.openapi.editor.Editor editor,
                                     @NotNull @NlsContexts.HintText String text) {
        com.intellij.openapi.editor.Editor unwrapped = IntentionInMacroUtil.unwrapEditor(editor);
        HintManager.getInstance().showErrorHint(unwrapped, text);
    }
}
