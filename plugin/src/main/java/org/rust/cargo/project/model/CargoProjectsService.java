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

/**
 * Stores a list of {@link CargoProject}s associated with the current IntelliJ {@link Project}.
 * Use {@link CargoProjectServiceKt#getCargoProjects(Project)} to get an instance of the service.
 */
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

    @FunctionalInterface
    interface CargoProjectsListener {
        void cargoProjectsUpdated(@Nonnull CargoProjectsService service, @Nonnull Collection<CargoProject> projects);
    }

    interface CargoProjectsRefreshListener {
        void onRefreshStarted();
        void onRefreshFinished(@Nonnull CargoRefreshStatus status);
    }

    enum CargoRefreshStatus {
        SUCCESS,
        FAILURE,
        CANCEL
    }

    interface Sequence<T> extends Iterable<T> {
    }
}
