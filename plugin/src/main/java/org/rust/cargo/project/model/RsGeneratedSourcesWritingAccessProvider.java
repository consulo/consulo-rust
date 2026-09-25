/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import consulo.util.lang.ref.SimpleReference;
import org.rust.cargo.api.model.CargoProjectsUtil;
import org.rust.cargo.api.model.CargoProjectsService;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.WritingAccessProvider;
import jakarta.annotation.Nonnull;

import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@ExtensionImpl
public class RsGeneratedSourcesWritingAccessProvider extends WritingAccessProvider {

    private final Project project;

    @Inject
    public RsGeneratedSourcesWritingAccessProvider(@Nonnull Project project) {
        this.project = project;
    }

    @Nonnull
    @Override
    public Collection<VirtualFile> requestWriting(VirtualFile... files) {
        CargoProjectsService cargoProjects = CargoProjectServiceUtil.getCargoProjects(project);
        return Arrays.stream(files)
            .filter(file -> CargoProjectsUtil.isGeneratedFile(cargoProjects, file))
            .collect(Collectors.toList());
    }

    @Override
    public boolean isPotentiallyWritable(@Nonnull VirtualFile file) {
        SimpleReference<Boolean> ref = new SimpleReference<>(Boolean.FALSE);
        if (project.getApplication().tryRunReadAction(ref, () -> !CargoProjectsUtil.isGeneratedFile(project, file))) {
            return ref.requiredGet();
        }
        return false;
    }
}
