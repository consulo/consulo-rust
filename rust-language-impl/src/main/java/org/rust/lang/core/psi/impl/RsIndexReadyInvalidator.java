/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.startup.BackgroundStartupActivity;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;

/**
 * Drops the caches built while the indexes were still being written.
 * <p>
 * Resolve answers questions about impls out of the index, so anything resolved before indexing
 * finishes sees no impls at all and caches that empty answer - a method call is then left
 * unresolved even though a later lookup would find it. The platform does not invalidate our caches
 * on its own, so the first moment the project is smart the Rust structure count is bumped and the
 * highlighting is re-run.
 */
@ExtensionImpl
public class RsIndexReadyInvalidator implements BackgroundStartupActivity {

    @Override
    public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        DumbService.getInstance(project).runWhenSmart(() -> {
            if (project.isDisposed()) return;
            project.getInstance(RsPsiManager.class).incRustStructureModificationCount();
            DaemonCodeAnalyzer.getInstance(project).restart();
        });
    }
}
