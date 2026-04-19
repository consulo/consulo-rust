/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @NotNull
    Project getProject();

    @NotNull
    Collection<CargoProject> getAllProjects();

    boolean getHasAtLeastOneValidProject();

    boolean getInitialized();

    @Nullable
    CargoProject findProjectForFile(@NotNull VirtualFile file);

    @Nullable
    CargoWorkspace.Package findPackageForFile(@NotNull VirtualFile file);

    /**
     * @param manifest a path to {@code Cargo.toml} file of the project that should be attached
     */
    boolean attachCargoProject(@NotNull Path manifest);

    void attachCargoProjects(@NotNull Path... manifests);

    void detachCargoProject(@NotNull CargoProject cargoProject);

    @NotNull
    CompletableFuture<? extends java.util.List<CargoProject>> refreshAllProjects();

    @NotNull
    CompletableFuture<? extends java.util.List<CargoProject>> discoverAndRefresh();

    @NotNull
    Sequence<VirtualFile> suggestManifests();

    void modifyFeatures(@NotNull CargoProject cargoProject, @NotNull Set<PackageFeature> features, @NotNull FeatureState newState);

    Topic<CargoProjectsListener> CARGO_PROJECTS_TOPIC = new Topic<>(
        "cargo projects changes",
        CargoProjectsListener.class
    );

    Topic<CargoProjectsRefreshListener> CARGO_PROJECTS_REFRESH_TOPIC = new Topic<>(
        "Cargo refresh",
        CargoProjectsRefreshListener.class
    );

    @FunctionalInterface
    interface CargoProjectsListener {
        void cargoProjectsUpdated(@NotNull CargoProjectsService service, @NotNull Collection<CargoProject> projects);
    }

    interface CargoProjectsRefreshListener {
        void onRefreshStarted();
        void onRefreshFinished(@NotNull CargoRefreshStatus status);
    }

    enum CargoRefreshStatus {
        SUCCESS,
        FAILURE,
        CANCEL
    }

    interface Sequence<T> extends Iterable<T> {
    }
}
