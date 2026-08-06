/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.WritingAccessProvider;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.stream.Collectors;

public class RsGeneratedSourcesWritingAccessProvider extends WritingAccessProvider {

    private final Project project;

    public RsGeneratedSourcesWritingAccessProvider(@Nonnull Project project) {
        this.project = project;
    }

    @Nonnull
    @Override
    public Collection<VirtualFile> requestWriting(VirtualFile... files) {
        CargoProjectsService cargoProjects = CargoProjectServiceUtil.getCargoProjects(project);
        return java.util.Arrays.stream(files)
            .filter(file -> CargoProjectServiceUtil.isGeneratedFile(cargoProjects, file))
            .collect(Collectors.toList());
    }

    @Override
    public boolean isPotentiallyWritable(@Nonnull VirtualFile file) {
        return !CargoProjectServiceUtil.isGeneratedFile(CargoProjectServiceUtil.getCargoProjects(project), file);
    }
}
