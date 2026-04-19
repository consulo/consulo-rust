/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.impl.RustcMessage.CompilerArtifactMessage;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class CargoBuildContextBase {
    private final CargoProject cargoProject;
    @NlsContexts.ProgressText
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
        @NlsContexts.ProgressText String progressTitle,
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
        return CargoCommandConfiguration.getWorkingDirectory(cargoProject);
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
