/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import consulo.disposer.Disposable;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import com.intellij.openapi.components.Service;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jdom.Element;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

    @Nonnull
    private final Project project;
    private volatile boolean initialized = false;

    public CargoProjectsServiceImpl(@Nonnull Project project) {
        this.project = project;
    }

    @Nonnull
    @Override
    public Project getProject() {
        return project;
    }

    @Nonnull
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
    public CargoProject findProjectForFile(@Nonnull VirtualFile file) {
        return null;
    }

    @Nullable
    @Override
    public CargoWorkspace.Package findPackageForFile(@Nonnull VirtualFile file) {
        return null;
    }

    @Override
    public boolean attachCargoProject(@Nonnull Path manifest) {
        return false;
    }

    @Override
    public void attachCargoProjects(@Nonnull Path... manifests) {
    }

    @Override
    public void detachCargoProject(@Nonnull CargoProject cargoProject) {
    }

    @Nonnull
    @Override
    public CompletableFuture<? extends List<CargoProject>> refreshAllProjects() {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Nonnull
    @Override
    public CompletableFuture<? extends List<CargoProject>> discoverAndRefresh() {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Nonnull
    @Override
    public Sequence<VirtualFile> suggestManifests() {
        return Collections::emptyIterator;
    }

    @Override
    public void modifyFeatures(@Nonnull CargoProject cargoProject, @Nonnull Set<PackageFeature> features, @Nonnull FeatureState newState) {
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
    public void loadState(@Nonnull Element state) {
        initialized = true;
    }

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
