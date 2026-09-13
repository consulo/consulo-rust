/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.sync;

import consulo.application.progress.ProgressIndicator;
import consulo.project.Project;

import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.api.toolchain.RustcMessage.CompilerArtifactMessage;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.rust.cargo.api.toolchain.RustcMessage;

public abstract class CargoBuildContextBase {
    private final CargoProject cargoProject;
    
    private final String progressTitle;
    private final boolean isTestBuild;
    private final Object buildId;
    private final Object parentId;

    private volatile ProgressIndicator indicator;
    private final AtomicInteger errors = new AtomicInteger();
    private final ConcurrentHashMap.KeySetView<String, Boolean> errorCodes = ConcurrentHashMap.newKeySet();
    private final AtomicInteger warnings = new AtomicInteger();
    private volatile List<CompilerArtifactMessage> artifacts = Collections.emptyList();

    public CargoBuildContextBase(
        CargoProject cargoProject,
         String progressTitle,
        boolean isTestBuild,
        Object buildId,
        Object parentId
    ) {
        this.cargoProject = cargoProject;
        this.progressTitle = progressTitle;
        this.isTestBuild = isTestBuild;
        this.buildId = buildId;
        this.parentId = parentId;
    }

    public CargoProject getCargoProject() {
        return cargoProject;
    }

    public String getProgressTitle() {
        return progressTitle;
    }

    public boolean isTestBuild() {
        return isTestBuild;
    }

    public Object getBuildId() {
        return buildId;
    }

    public Object getParentId() {
        return parentId;
    }

    public Project getProject() {
        return cargoProject.getProject();
    }

    public Path getWorkingDirectory() {
        return org.rust.cargo.project.model.CargoProjectLocator.getWorkingDirectory(cargoProject);
    }

    public ProgressIndicator getIndicator() {
        return indicator;
    }

    public void setIndicator(ProgressIndicator indicator) {
        this.indicator = indicator;
    }

    public AtomicInteger getErrors() {
        return errors;
    }

    public ConcurrentHashMap.KeySetView<String, Boolean> getErrorCodes() {
        return errorCodes;
    }

    public AtomicInteger getWarnings() {
        return warnings;
    }

    public List<CompilerArtifactMessage> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<CompilerArtifactMessage> artifacts) {
        this.artifacts = artifacts;
    }
}
