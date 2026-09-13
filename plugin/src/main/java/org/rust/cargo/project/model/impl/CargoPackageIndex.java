/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.disposer.Disposer;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.indexing.LightDirectoryIndex;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;
import org.rust.cargo.api.model.CargoProjectsListener;

public class CargoPackageIndex implements CargoProjectsListener {

    private final Project project;
    private final CargoProjectsService service;
    private final Map<CargoProject, LightDirectoryIndex<Optional<CargoWorkspace.Package>>> indices = new HashMap<>();
    private Disposable indexDisposable;

    public CargoPackageIndex(@Nonnull Project project, @Nonnull CargoProjectsService service) {
        this.project = project;
        this.service = service;
        project.getMessageBus().connect(project).subscribe(CargoProjectsService.CARGO_PROJECTS_TOPIC, this);
    }

    @Override
    public void cargoProjectsUpdated(@Nonnull CargoProjectsService service, @Nonnull Collection<CargoProject> projects) {
        OpenApiUtil.checkWriteAccessAllowed();
        resetIndex();
        Disposable disposable = Disposable.newDisposable("CargoPackageIndexDisposable");
        Disposer.register(project, disposable);
        for (CargoProject cargoProject : projects) {
            Collection<CargoWorkspace.Package> packages = cargoProject.getWorkspace() != null
                ? cargoProject.getWorkspace().getPackages() : Collections.emptyList();
            indices.put(cargoProject, new LightDirectoryIndex<>(disposable, Optional.empty(), index -> {
                for (CargoWorkspace.Package pkg : packages) {
                    Optional<CargoWorkspace.Package> info = Optional.of(pkg);
                    index.putInfo(pkg.getContentRoot(), info);
                    index.putInfo(pkg.getOutDir(), info);
                    for (VirtualFile additionalRoot : CargoWorkspace.additionalRoots(pkg)) {
                        index.putInfo(additionalRoot, info);
                    }
                    for (CargoWorkspace.Target target : pkg.getTargets()) {
                        VirtualFile crateRoot = target.getCrateRoot();
                        if (crateRoot != null) {
                            index.putInfo(crateRoot.getParent(), info);
                        }
                    }
                }
            }));
        }
        indexDisposable = disposable;
    }

    @Nullable
    public CargoWorkspace.Package findPackageForFile(@Nonnull VirtualFile file) {
        OpenApiUtil.checkReadAccessAllowed();
        CargoProject cargoProject = service.findProjectForFile(file);
        if (cargoProject == null) return null;
        LightDirectoryIndex<Optional<CargoWorkspace.Package>> index = indices.get(cargoProject);
        if (index == null) return null;
        return index.getInfoForFile(file).orElse(null);
    }

    private void resetIndex() {
        if (indexDisposable != null) {
            Disposer.dispose(indexDisposable);
        }
        indexDisposable = null;
        indices.clear();
    }
}
