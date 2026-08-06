/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.DefaultBuildDescriptor;
import consulo.build.ui.SyncViewManager;
import consulo.build.ui.event.MessageEvent;
import consulo.build.ui.progress.BuildProgress;
import consulo.build.ui.progress.BuildProgressDescriptor;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.application.AllIcons;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.logging.Logger;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.localize.LocalizeValue;

import jakarta.annotation.Nonnull;
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
        @Nonnull Project project,
        @Nonnull List<CargoProjectImpl> cargoProjects,
        @Nonnull CompletableFuture<List<CargoProjectImpl>> result
    ) {
        super(project, RsBundle.message("progress.title.reloading.cargo.projects"), true);
        this.cargoProjects = cargoProjects;
        this.result = result;
    }

    @Nonnull
    @Override
    public RsTask.TaskType getTaskType() {
        return RsTask.TaskType.CARGO_SYNC;
    }

    @Override
    public boolean getRunSyncInUnitTests() {
        return true;
    }

    @Override
    public void run(@Nonnull ProgressIndicator indicator) {
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
        @Nonnull public final Project project;
        @Nonnull public final CargoProjectImpl oldCargoProject;
        @Nonnull public final RsToolchainBase toolchain;
        @Nonnull public final ProgressIndicator progress;
        @Nonnull public final Object buildId;
        @Nonnull public final BuildProgress<BuildProgressDescriptor> syncProgress;

        public SyncContext(
            @Nonnull Project project,
            @Nonnull CargoProjectImpl oldCargoProject,
            @Nonnull RsToolchainBase toolchain,
            @Nonnull ProgressIndicator progress,
            @Nonnull Object buildId,
            @Nonnull BuildProgress<BuildProgressDescriptor> syncProgress
        ) {
            this.project = project;
            this.oldCargoProject = oldCargoProject;
            this.toolchain = toolchain;
            this.progress = progress;
            this.buildId = buildId;
            this.syncProgress = syncProgress;
        }

        @Nonnull
        public Object getId() {
            return syncProgress.getId();
        }

        public <T> TaskResult<T> runWithChildProgress(
             @Nonnull String title,
            @Nonnull java.util.function.Function<SyncContext, TaskResult<T>> action
        ) {
            progress.checkCanceled();
            progress.setText(title);
            return action.apply(this);
        }

        public void withProgressText(  @Nonnull String text) {
            progress.setText(text);
            syncProgress.progress(LocalizeValue.of(text));
        }
    }
}
