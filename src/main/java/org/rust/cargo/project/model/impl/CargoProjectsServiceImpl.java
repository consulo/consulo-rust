/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.*;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.FeatureState;
import org.rust.cargo.project.workspace.PackageFeature;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of CargoProjectsService.
 */
@State(name = "CargoProjects", storages = {
    @Storage(StoragePathMacros.WORKSPACE_FILE),
    @Storage(value = "misc.xml", deprecated = true)
})
public class CargoProjectsServiceImpl implements CargoProjectsService, PersistentStateComponent<Element>, Disposable {

    @NotNull
    private final Project project;
    private volatile boolean initialized = false;

    public CargoProjectsServiceImpl(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public Project getProject() {
        return project;
    }

    @NotNull
    @Override
    public Collection<CargoProject> getAllProjects() {
        return Collections.emptyList();
    }

    @Override
    public boolean getHasAtLeastOneValidProject() {
        return false;
    }

    @Override
    public boolean getInitialized() {
        return initialized;
    }

    @Nullable
    @Override
    public CargoProject findProjectForFile(@NotNull VirtualFile file) {
        return null;
    }

    @Nullable
    @Override
    public CargoWorkspace.Package findPackageForFile(@NotNull VirtualFile file) {
        return null;
    }

    @Override
    public boolean attachCargoProject(@NotNull Path manifest) {
        return false;
    }

    @Override
    public void attachCargoProjects(@NotNull Path... manifests) {
    }

    @Override
    public void detachCargoProject(@NotNull CargoProject cargoProject) {
    }

    @NotNull
    @Override
    public CompletableFuture<? extends List<CargoProject>> refreshAllProjects() {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @NotNull
    @Override
    public CompletableFuture<? extends List<CargoProject>> discoverAndRefresh() {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @NotNull
    @Override
    public Sequence<VirtualFile> suggestManifests() {
        return Collections::emptyIterator;
    }

    @Override
    public void modifyFeatures(@NotNull CargoProject cargoProject, @NotNull Set<PackageFeature> features, @NotNull FeatureState newState) {
    }

    @Nullable
    @Override
    public Element getState() {
        Element state = new Element("state");
        for (CargoProject cargoProject : getAllProjects()) {
            Element cargoProjectElement = new Element("cargoProject");
            cargoProjectElement.setAttribute("FILE", cargoProject.getManifest().toString().replace('\\', '/'));
            state.addContent(cargoProjectElement);
        }
        return state;
    }

    @Override
    public void loadState(@NotNull Element state) {
        initialized = true;
    }

    @Override
    public void noStateLoaded() {
        initialized = true;
    }

    @Override
    public void dispose() {
    }

    @Override
    public String toString() {
        return "CargoProjectsService(projects = " + getAllProjects() + ")";
    }

    public static final String CARGO_DISABLE_PROJECT_REFRESH_ON_CREATION = "cargo.disable.project.refresh.on.creation";
}
