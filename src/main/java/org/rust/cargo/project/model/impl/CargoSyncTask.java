/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import com.intellij.build.BuildContentDescriptor;
import com.intellij.build.BuildDescriptor;
import com.intellij.build.DefaultBuildDescriptor;
import com.intellij.build.SyncViewManager;
import com.intellij.build.events.MessageEvent;
import com.intellij.build.progress.BuildProgress;
import com.intellij.build.progress.BuildProgressDescriptor;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.RsTask;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.ProcessProgressListener;
import org.rust.cargo.project.model.RustcInfo;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.StandardLibrary;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.openapiext.TaskResult;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Cargo sync task - reloads cargo projects.
 */
@SuppressWarnings("UnstableApiUsage")
public class CargoSyncTask extends Task.Backgroundable implements RsTask {

    private static final Logger LOG = Logger.getInstance(CargoSyncTask.class);

    private final List<CargoProjectImpl> cargoProjects;
    private final CompletableFuture<List<CargoProjectImpl>> result;

    public CargoSyncTask(
        @NotNull Project project,
        @NotNull List<CargoProjectImpl> cargoProjects,
        @NotNull CompletableFuture<List<CargoProjectImpl>> result
    ) {
        super(project, RsBundle.message("progress.title.reloading.cargo.projects"), true);
        this.cargoProjects = cargoProjects;
        this.result = result;
    }

    @NotNull
    @Override
    public RsTask.TaskType getTaskType() {
        return RsTask.TaskType.CARGO_SYNC;
    }

    @Override
    public boolean getRunSyncInUnitTests() {
        return true;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        LOG.info("CargoSyncTask started");
        indicator.setIndeterminate(true);
        long start = System.currentTimeMillis();

        // Simplified - the full implementation involves build progress tracking
        try {
            // The actual sync logic is complex and delegates to toolchain operations
            result.complete(cargoProjects);
        } catch (Throwable e) {
            result.completeExceptionally(e);
            throw e;
        }

        long elapsed = System.currentTimeMillis() - start;
        LOG.debug("Finished Cargo sync task in " + elapsed + " ms");
    }

    public static class SyncContext {
        @NotNull public final Project project;
        @NotNull public final CargoProjectImpl oldCargoProject;
        @NotNull public final RsToolchainBase toolchain;
        @NotNull public final ProgressIndicator progress;
        @NotNull public final Object buildId;
        @NotNull public final BuildProgress<BuildProgressDescriptor> syncProgress;

        public SyncContext(
            @NotNull Project project,
            @NotNull CargoProjectImpl oldCargoProject,
            @NotNull RsToolchainBase toolchain,
            @NotNull ProgressIndicator progress,
            @NotNull Object buildId,
            @NotNull BuildProgress<BuildProgressDescriptor> syncProgress
        ) {
            this.project = project;
            this.oldCargoProject = oldCargoProject;
            this.toolchain = toolchain;
            this.progress = progress;
            this.buildId = buildId;
            this.syncProgress = syncProgress;
        }

        @NotNull
        public Object getId() {
            return syncProgress.getId();
        }

        public <T> TaskResult<T> runWithChildProgress(
            @NlsContexts.ProgressText @NotNull String title,
            @NotNull java.util.function.Function<SyncContext, TaskResult<T>> action
        ) {
            progress.checkCanceled();
            progress.setText(title);
            return action.apply(this);
        }

        public void withProgressText(@NlsContexts.ProgressText @NlsContexts.ProgressTitle @NotNull String text) {
            progress.setText(text);
            syncProgress.progress(text);
        }
    }
}
