/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.generated;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.language.psi.PsiModificationTrackerListener;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/** Regenerates the generated includes whose sources - for example {@code .proto} files - were edited. */
@TopicImpl(ComponentScope.PROJECT)
public final class RsGeneratedSourcesListener implements PsiModificationTrackerListener {
    @Nonnull
    private final Project project;

    @Inject
    public RsGeneratedSourcesListener(@Nonnull Project project) {
        this.project = project;
    }

    @Override
    public void modificationCountChanged() {
        if (project.isDisposed()) return;
        RsGeneratedIncludes.getInstance(project).checkSources();
    }
}
