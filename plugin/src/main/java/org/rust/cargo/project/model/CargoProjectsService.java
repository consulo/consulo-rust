/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.FeatureState;
import org.rust.cargo.project.workspace.PackageFeature;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

/**
 * Stores a list of {@link CargoProject}s associated with the current {@link Project}.
 * Use {@link CargoProjectServiceKt#getCargoProjects(Project)} to get an instance of the service.
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface CargoProjectsService {

    @Nonnull
    Project getProject();

    @Nonnull
    Collection<CargoProject> getAllProjects();

    boolean getHasAtLeastOneValidProject();

    boolean getInitialized();

    @Nullable
    CargoProject findProjectForFile(@Nonnull VirtualFile file);

    @Nullable
    CargoWorkspace.Package findPackageForFile(@Nonnull VirtualFile file);

    /**
     * @param manifest a path to {@code Cargo.toml} file of the project that should be attached
     */
    boolean attachCargoProject(@Nonnull Path manifest);

    void attachCargoProjects(@Nonnull Path... manifests);

    void detachCargoProject(@Nonnull CargoProject cargoProject);

    @Nonnull
    CompletableFuture<? extends java.util.List<CargoProject>> refreshAllProjects();

    @Nonnull
    CompletableFuture<? extends java.util.List<CargoProject>> discoverAndRefresh();

    @Nonnull
    Sequence<VirtualFile> suggestManifests();

    void modifyFeatures(@Nonnull CargoProject cargoProject, @Nonnull Set<PackageFeature> features, @Nonnull FeatureState newState);

    Class<CargoProjectsListener> CARGO_PROJECTS_TOPIC = CargoProjectsListener.class;

    Class<CargoProjectsRefreshListener> CARGO_PROJECTS_REFRESH_TOPIC = CargoProjectsRefreshListener.class;



    enum CargoRefreshStatus {
        SUCCESS,
        FAILURE,
        CANCEL
    }

    interface Sequence<T> extends Iterable<T> {
    }
}
