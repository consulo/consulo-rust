/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.template;

import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateEditingAdapter;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageEditorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.intentions.util.macros.IntentionInMacroUtil;
import org.rust.ide.intentions.util.macros.RsIntentionInsideMacroExpansionEditor;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.openapiext.OpenApiUtil;

/**
 * Extension utilities for Editor template operations.
 */
public final class EditorExtUtil {

    private EditorExtUtil() {
    }

    public static void buildAndRunTemplate(
        @NotNull Editor editor,
        @NotNull PsiElement owner,
        @NotNull Iterable<? extends PsiElement> elementsToReplace,
        @Nullable Runnable onFinish
    ) {
        if (!RsElementUtil.isIntentionPreviewElement(owner)) {
            OpenApiUtil.checkWriteAccessAllowed();
        }
        RsTemplateBuilder tpl = newTemplateBuilder(editor, owner);
        for (PsiElement element : elementsToReplace) {
            tpl.replaceElement(element, (String) null);
        }
        if (onFinish != null) {
            tpl.withListener(new TemplateEditingAdapter() {
                @Override
                public void templateFinished(@NotNull Template template, boolean brokenOff) {
                    if (!brokenOff) {
                        onFinish.run();
                    }
                }
            });
        }
        tpl.runInline();
    }

    public static void buildAndRunTemplate(
        @NotNull Editor editor,
        @NotNull PsiElement owner,
        @NotNull Iterable<? extends PsiElement> elementsToReplace
    ) {
        buildAndRunTemplate(editor, owner, elementsToReplace, null);
    }

    @NotNull
    public static RsTemplateBuilder newTemplateBuilder(@NotNull Editor editor, @NotNull PsiElement context) {
        Editor hostEditor = InjectedLanguageEditorUtil.getTopLevelEditor(IntentionInMacroUtil.unwrapEditor(editor));
        PsiFile contextualPsiFile;
        if (editor instanceof RsIntentionInsideMacroExpansionEditor) {
            contextualPsiFile = ((RsIntentionInsideMacroExpansionEditor) editor).getOriginalFile();
        } else {
            contextualPsiFile = context.getContainingFile();
        }
        PsiFile hostPsiFile = InjectedLanguageManager.getInstance(context.getProject()).getTopLevelFile(contextualPsiFile);
        return new RsTemplateBuilder(hostPsiFile, editor, hostEditor);
    }

    public static boolean canRunTemplateFor(@NotNull Editor editor, @NotNull PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        if (editor instanceof RsIntentionInsideMacroExpansionEditor) {
            RsIntentionInsideMacroExpansionEditor macroEditor = (RsIntentionInsideMacroExpansionEditor) editor;
            return containingFile == macroEditor.getOriginalFile() || containingFile == macroEditor.getPsiFileCopy();
        }
        return PsiDocumentManager.getInstance(containingFile.getProject()).getPsiFile(editor.getDocument()) == containingFile;
    }
}
