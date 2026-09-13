/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.template;

import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.event.TemplateEditingAdapter;
import consulo.language.inject.InjectedLanguageManager;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.editor.inject.EditorWindow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.intentions.util.macros.IntentionInMacroUtil;
import org.rust.ide.intentions.util.macros.RsIntentionInsideMacroExpansionEditor;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.openapiext.OpenApiUtil;

public final class EditorExt {
    private EditorExt() {
    }

    public static void buildAndRunTemplate(
        @Nonnull Editor editor,
        @Nonnull PsiElement owner,
        @Nonnull Iterable<PsiElement> elementsToReplace,
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
            tpl.withResultListener(new TemplateEditingAdapter() {
                @Override
                public void templateFinished(@Nonnull Template template, boolean brokenOff) {
                    if (!brokenOff) {
                        onFinish.run();
                    }
                }
            });
        }
        tpl.runInline();
    }

    public static void buildAndRunTemplate(
        @Nonnull Editor editor,
        @Nonnull PsiElement owner,
        @Nonnull Iterable<PsiElement> elementsToReplace
    ) {
        buildAndRunTemplate(editor, owner, elementsToReplace, null);
    }

    @Nonnull
    public static RsTemplateBuilder newTemplateBuilder(@Nonnull Editor editor, @Nonnull PsiElement context) {
        // First macros, then injections (assume that macro expansion can't contain language injections)
        Editor hostEditor = EditorWindow.getTopLevelEditor(IntentionInMacroUtil.unwrapEditor(editor));
        PsiFile contextualPsiFile;
        if (editor instanceof RsIntentionInsideMacroExpansionEditor) {
            contextualPsiFile = ((RsIntentionInsideMacroExpansionEditor) editor).getOriginalFile();
        } else {
            contextualPsiFile = context.getContainingFile();
        }
        PsiFile hostPsiFile = InjectedLanguageManager.getInstance(context.getProject()).getTopLevelFile(contextualPsiFile);
        return new RsTemplateBuilder(hostPsiFile, editor, hostEditor);
    }

    public static boolean canRunTemplateFor(@Nonnull Editor editor, @Nonnull PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        if (editor instanceof RsIntentionInsideMacroExpansionEditor) {
            RsIntentionInsideMacroExpansionEditor macroEditor = (RsIntentionInsideMacroExpansionEditor) editor;
            return containingFile == macroEditor.getOriginalFile() || containingFile == macroEditor.getPsiFileCopy();
        }
        return PsiDocumentManager.getInstance(containingFile.getProject()).getPsiFile(editor.getDocument()) == containingFile;
    }
}
