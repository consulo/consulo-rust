/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.api.model;

import consulo.annotation.access.RequiredReadAction;
import org.rust.cargo.api.model.CargoProjectsRefreshListener;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.FeatureState;
import org.rust.cargo.api.workspace.PackageFeature;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

/**
 * Stores a list of {@link CargoProject}s associated with the current {@link Project}.
 * Use {@link CargoProjectsUtil#getCargoProjects(Project)} to get an instance of the service.
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface CargoProjectsService {

    /**
     * The cargo projects of {@code project}.
     */
    static CargoProjectsService getInstance(@Nonnull Project project) {
        return project.getInstance(CargoProjectsService.class);
    }

    @Nonnull
    Project getProject();

    @Nonnull
    Collection<CargoProject> getAllProjects();

    boolean getHasAtLeastOneValidProject();

    boolean getInitialized();

    @Nullable
    CargoProject findProjectForFile(@Nonnull VirtualFile file);

    @Nullable
    @RequiredReadAction
    CargoWorkspace.Package findPackageForFile(@Nonnull VirtualFile file);

    /**
     * @param manifest a path to {@code Cargo.toml} file of the project that should be attached
     */
    boolean attachCargoProject(@Nonnull Path manifest);

    /**
     * Like {@link #attachCargoProject}, but hands back the refresh that registers the project, so a
     * caller can wait for it. Attaching is asynchronous: until the returned future completes,
     * {@link #findProjectForFile} still answers {@code null} for files of the new project.
     */
    @Nonnull
    CompletableFuture<?> attachCargoProjectAsync(@Nonnull Path manifest);

    void attachCargoProjects(@Nonnull Path... manifests);

    void detachCargoProject(@Nonnull CargoProject cargoProject);

    @Nonnull
    CompletableFuture<? extends List<CargoProject>> refreshAllProjects();

    @Nonnull
    CompletableFuture<? extends List<CargoProject>> discoverAndRefresh();

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
