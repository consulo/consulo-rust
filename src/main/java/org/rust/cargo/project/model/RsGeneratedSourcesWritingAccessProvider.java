/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.WritingAccessProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

public class RsGeneratedSourcesWritingAccessProvider extends WritingAccessProvider {

    private final Project project;

    public RsGeneratedSourcesWritingAccessProvider(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public Collection<VirtualFile> requestWriting(@NotNull Collection<? extends VirtualFile> files) {
        CargoProjectsService cargoProjects = CargoProjectServiceUtil.getCargoProjects(project);
        return files.stream()
            .filter(file -> CargoProjectServiceUtil.isGeneratedFile(cargoProjects, file))
            .collect(Collectors.toList());
    }

    @Override
    public boolean isPotentiallyWritable(@NotNull VirtualFile file) {
        return !CargoProjectServiceUtil.isGeneratedFile(CargoProjectServiceUtil.getCargoProjects(project), file);
    }
}
