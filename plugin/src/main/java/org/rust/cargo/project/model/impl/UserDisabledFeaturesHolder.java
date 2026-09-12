/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.project.Project;
import org.jdom.Element;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Inject;

/**
 * A service needed to store {@link UserDisabledFeatures} separately from CargoProjectsServiceImpl.
 */
@State(name = "CargoProjectFeatures", storages = {
    @Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)
})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class UserDisabledFeaturesHolder implements PersistentStateComponent<Element> {

    private final Project project;
    private Map<Path, UserDisabledFeatures> loadedUserDisabledFeatures = Collections.emptyMap();

    @Inject

    public UserDisabledFeaturesHolder(@Nonnull Project project) {
        this.project = project;
    }

    @Nonnull
    public Map<Path, UserDisabledFeatures> takeLoadedUserDisabledFeatures() {
        Map<Path, UserDisabledFeatures> result = loadedUserDisabledFeatures;
        loadedUserDisabledFeatures = Collections.emptyMap();
        return result;
    }

    @Nullable
    @Override
    public Element getState() {
        Element state = new Element("state");
        for (CargoProject cargoProject : CargoProjectServiceUtil.getCargoProjects(project).getAllProjects()) {
            UserDisabledFeatures pkgToFeatures = cargoProject.getUserDisabledFeatures();
            if (!pkgToFeatures.isEmpty()) {
                Element cargoProjectElement = new Element("cargoProject");
                cargoProjectElement.setAttribute("file", cargoProject.getManifest().toString().replace('\\', '/'));
                for (var entry : pkgToFeatures.getPkgRootToDisabledFeatures().entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        Element packageElement = new Element("package");
                        packageElement.setAttribute("file", entry.getKey().toString().replace('\\', '/'));
                        for (String feature : entry.getValue()) {
                            Element featureElement = new Element("feature");
                            featureElement.setAttribute("name", feature);
                            packageElement.addContent(featureElement);
                        }
                        cargoProjectElement.addContent(packageElement);
                    }
                }
                state.addContent(cargoProjectElement);
            }
        }
        return state;
    }

    @Override
    public void loadState(@Nonnull Element state) {
        List<Element> cargoProjects = state.getChildren("cargoProject");

        Map<Path, UserDisabledFeatures> result = new HashMap<>();
        for (Element cargoProject : cargoProjects) {
            String projectFile = cargoProject.getAttributeValue("file");
            if (projectFile == null) projectFile = "";

            Map<Path, Set<String>> pkgFeatures = new HashMap<>();
            for (Element pkg : cargoProject.getChildren("package")) {
                String packageFile = pkg.getAttributeValue("file");
                if (packageFile == null) packageFile = "";
                Set<String> features = new HashSet<>();
                for (Element featureElem : pkg.getChildren("feature")) {
                    String name = featureElem.getAttributeValue("name");
                    if (name != null) features.add(name);
                }
                pkgFeatures.put(Paths.get(packageFile), features);
            }
            result.put(Paths.get(projectFile), UserDisabledFeatures.of(pkgFeatures));
        }
        loadedUserDisabledFeatures = result;
    }
}
