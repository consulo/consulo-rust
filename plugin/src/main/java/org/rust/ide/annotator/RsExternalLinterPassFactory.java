/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.language.editor.impl.highlight.DirtyScopeTrackingHighlightingPassFactory;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/**
 * Creates the highlighting pass that runs the external linter and merges the requests it produces
 * into a single throttled queue.
 */
@ExtensionImpl
public class RsExternalLinterPassFactory implements DirtyScopeTrackingHighlightingPassFactory {
    private static final int TIME_SPAN = 300;

    @Nonnull
    private final MergingUpdateQueue myExternalLinterQueue;

    private int myPassId = -1;

    @Inject
    public RsExternalLinterPassFactory(@Nonnull Project project) {
        myExternalLinterQueue = new MergingUpdateQueue(
            "RsExternalLinterQueue",
            TIME_SPAN,
            true,
            MergingUpdateQueue.ANY_COMPONENT,
            project,
            null,
            false
        );
    }

    @Override
    public void register(@Nonnull Registrar registrar) {
        myPassId = registrar.registerTextEditorHighlightingPass(this, null, null, false, -1);
    }

    @Nullable
    @Override
    public TextEditorHighlightingPass createHighlightingPass(@Nonnull PsiFile file, @Nonnull Editor editor) {
        return new RsExternalLinterPass(this, file, editor);
    }

    @Override
    public int getPassId() {
        return myPassId;
    }

    public void scheduleExternalActivity(@Nonnull Update update) {
        myExternalLinterQueue.queue(update);
    }
}
