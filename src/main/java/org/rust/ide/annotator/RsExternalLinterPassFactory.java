/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import com.intellij.codeHighlighting.DirtyScopeTrackingHighlightingPassFactory;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import com.intellij.codeInsight.daemon.impl.FileStatusMap;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RsExternalLinterPassFactory implements DirtyScopeTrackingHighlightingPassFactory {
    private static final int TIME_SPAN = 300;

    private final int myPassId;
    @NotNull
    private final MergingUpdateQueue myExternalLinterQueue;

    public RsExternalLinterPassFactory(@NotNull Project project, @NotNull TextEditorHighlightingPassRegistrar registrar) {
        this.myPassId = registrar.registerTextEditorHighlightingPass(this, null, null, false, -1);
        this.myExternalLinterQueue = new MergingUpdateQueue(
            "RsExternalLinterQueue",
            TIME_SPAN,
            true,
            MergingUpdateQueue.ANY_COMPONENT,
            project,
            null,
            false
        );
    }

    @Nullable
    @Override
    public TextEditorHighlightingPass createHighlightingPass(@NotNull PsiFile file, @NotNull Editor editor) {
        if (FileStatusMap.getDirtyTextRange(editor, getPassId()) == null) return null;
        return new RsExternalLinterPass(this, file, editor);
    }

    @Override
    public int getPassId() {
        return myPassId;
    }

    public void scheduleExternalActivity(@NotNull Update update) {
        myExternalLinterQueue.queue(update);
    }
}
