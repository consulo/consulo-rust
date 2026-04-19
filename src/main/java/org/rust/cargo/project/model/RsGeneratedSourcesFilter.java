/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.GeneratedSourcesFilter;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.macros.MacroExpansionManager;

public class RsGeneratedSourcesFilter extends GeneratedSourcesFilter {

    @Override
    public boolean isGeneratedSource(@NotNull VirtualFile file, @NotNull Project project) {
        return MacroExpansionManager.isExpansionFile(file)
            || CargoProjectServiceUtil.isGeneratedFile(CargoProjectServiceUtil.getCargoProjects(project), file);
    }
}
