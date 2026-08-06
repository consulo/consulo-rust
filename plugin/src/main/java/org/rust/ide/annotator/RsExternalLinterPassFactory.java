/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;
import consulo.language.editor.FileStatusMap;

import consulo.language.editor.impl.highlight.DirtyScopeTrackingHighlightingPassFactory;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class RsExternalLinterPassFactory implements DirtyScopeTrackingHighlightingPassFactory {
    private static final int TIME_SPAN = 300;

    private final int myPassId;
    @Nonnull
    private final MergingUpdateQueue myExternalLinterQueue;

    public RsExternalLinterPassFactory(@Nonnull Project project, @Nonnull TextEditorHighlightingPassRegistrar registrar) {
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
    public TextEditorHighlightingPass createHighlightingPass(@Nonnull PsiFile file, @Nonnull Editor editor) {
        // FileStatusMap.getDirtyTextRange is not exposed in Consulo; always run the pass
        // TODO: replace with proper FileStatusMap injection + getFileDirtyScope when needed
        return new RsExternalLinterPass(this, file, editor);
    }

    @Override
    public int getPassId() {
        return myPassId;
    }

    public void scheduleExternalActivity(@Nonnull Update update) {
        myExternalLinterQueue.queue(update);
    }

    @Override
    public void register(@Nonnull consulo.language.editor.highlight.TextEditorHighlightingPassFactory.Registrar registrar) {
        // TODO: wire Registrar-based registration
    }
}
