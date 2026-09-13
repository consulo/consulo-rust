/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.cargo;

import org.rust.cargo.api.model.CargoProjectsUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.content.GeneratedSourcesFilter;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.rust.lang.core.macros.MacroExpansionManager;
import org.rust.cargo.project.model.CargoProjectServiceUtil;

@ExtensionImpl
public class RsGeneratedSourcesFilter implements GeneratedSourcesFilter {

    private final Project myProject;

    @Inject
    public RsGeneratedSourcesFilter(@Nonnull Project project) {
        this.myProject = project;
    }

    @Override
    public boolean isGeneratedSource(@Nonnull VirtualFile file) {
        return CargoProjectsUtil.isGeneratedFile(myProject, file);
    }
}
