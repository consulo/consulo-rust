/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.content.GeneratedSourcesFilter;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.rust.lang.core.macros.MacroExpansionManager;

@ExtensionImpl
public class RsGeneratedSourcesFilter implements GeneratedSourcesFilter {

    private final Project myProject;

    @Inject
    public RsGeneratedSourcesFilter(@Nonnull Project project) {
        this.myProject = project;
    }

    @Override
    public boolean isGeneratedSource(@Nonnull VirtualFile file) {
        return MacroExpansionManager.isExpansionFile(file)
            || CargoProjectServiceUtil.isGeneratedFile(CargoProjectServiceUtil.getCargoProjects(myProject), file);
    }
}
