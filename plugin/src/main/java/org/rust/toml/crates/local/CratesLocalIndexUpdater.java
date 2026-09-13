/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.crates.local;

import org.rust.cargo.toolchain.RsToolchainLocator;
import consulo.execution.configuration.EnvironmentVariablesData;
import consulo.disposer.Disposable;
import consulo.application.ApplicationManager;
import consulo.logging.Logger;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.application.util.registry.Registry;
import consulo.ui.ex.awt.util.Alarm;
import jakarta.annotation.Nonnull;

import org.rust.cargo.CargoConstants;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.toolchain.BacktraceMode;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.openapiext.CommandLineExt;
import org.rust.openapiext.RsPathManager;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.RsResult;
import org.rust.toml.RsTomlBundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.annotation.component.ComponentScope;
import consulo.application.ApplicationPropertiesComponent;
import consulo.process.cmd.GeneralCommandLine;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;

@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public final class CratesLocalIndexUpdater implements Disposable {
    private static final Logger LOG = Logger.getInstance(CratesLocalIndexUpdater.class);
    private static final String CRATES_IO_INDEX_LAST_UPDATE = "CRATES_IO_INDEX_LAST_UPDATE";
    private static final int DEFAULT_UPDATE_INTERVAL_MIN = 60;
    private static final int MIN_UPDATE_INTERVAL_MIN = 1;
    private static final String UPDATE_PROJECT_DIR_NAME = "crate_index_update_project";

    private final Alarm myAlarm = new Alarm(this);
    private boolean myIsUpdating = false;

    public void updateCratesIoGitIndex() {
        OpenApiUtil.checkIsDispatchThread();
        if (myIsUpdating) return;
        if (!hasOpenRustProject()) return;

        long lastUpdate = ApplicationPropertiesComponent.getInstance().getLong(CRATES_IO_INDEX_LAST_UPDATE, 0);
        long sinceLastUpdate = System.currentTimeMillis() - lastUpdate;
        int interval = getUpdateIntervalMillis();

        if (sinceLastUpdate < interval) {
            if (myAlarm.isEmpty()) {
                scheduleUpdate(interval - (int) sinceLastUpdate);
            }
            return;
        }
        myIsUpdating = true;
        myAlarm.cancelAllRequests();

        new Task.Backgroundable(null, RsTomlBundle.message("rust.update.crates.index.progress.title")) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                LOG.info("crates.io index update started");
                boolean isSuccessful = updateCratesIoGitIndex(CratesLocalIndexUpdater.this);
                if (isSuccessful) {
                    ApplicationPropertiesComponent.getInstance().setValue(CRATES_IO_INDEX_LAST_UPDATE, String.valueOf(System.currentTimeMillis()));
                }
            }

            @Override
            public void onSuccess() {
                CratesLocalIndexService index = CratesLocalIndexService.getInstance();
                if (index instanceof CratesLocalIndexServiceImpl) {
                    ((CratesLocalIndexServiceImpl) index).recoverIfNeeded();
                }
            }

            @Override
            public void onFinished() {
                onUpdateFinished();
            }
        }.queue();
    }

    private void onUpdateFinished() {
        LOG.info("crates.io index update finished");
        myIsUpdating = false;
        scheduleUpdate(getUpdateIntervalMillis());
    }

    private void scheduleUpdate(int delay) {
        if (myAlarm.isEmpty()) {
            myAlarm.addRequest(this::updateCratesIoGitIndex, delay);
            LOG.info("crates.io index update is scheduled in " + delay + " ms");
        }
    }

    @Override
    public void dispose() {}

    @Nonnull
    public static CratesLocalIndexUpdater getInstance() {
        return ApplicationManager.getApplication().getService(CratesLocalIndexUpdater.class);
    }

    private static boolean hasOpenRustProject() {
        return openRustProject() != null;
    }

    @jakarta.annotation.Nullable
    private static Project openRustProject() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            CargoProjectsService service = project.getInstance(CargoProjectsService.class);
            if (service != null && !service.getAllProjects().isEmpty()) {
                return project;
            }
        }
        return null;
    }

    private static int getUpdateIntervalMillis() {
        int intervalMin = Math.max(MIN_UPDATE_INTERVAL_MIN,
            Registry.intValue("org.rust.crates.local.index.update.interval", DEFAULT_UPDATE_INTERVAL_MIN));
        long intervalMillis = TimeUnit.MINUTES.toMillis(intervalMin);
        return intervalMillis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) intervalMillis;
    }

    
    public static boolean updateCratesIoGitIndex(@Nonnull Disposable disposable) {
        Path projectPath = RsPathManager.pluginDirInSystem().resolve(UPDATE_PROJECT_DIR_NAME);
        boolean projectCreated = createUpdateProjectIfNeeded(projectPath);
        if (!projectCreated) return false;

        // The index refresh runs against a throwaway project of our own, but it must still use the
        // toolchain the user configured on their Rust module rather than whichever cargo is on PATH.
        Project project = openRustProject();
        RsToolchainBase toolchain = project == null
            ? null
            : RsToolchainLocator.getToolchain(project);
        if (toolchain == null) return false;
        return triggerCratesIoGitIndexUpdate(toolchain, disposable, projectPath);
    }

    private static boolean createUpdateProjectIfNeeded(@Nonnull Path projectPath) {
        try {
            Path cargoToml = projectPath.resolve(CargoConstants.MANIFEST_FILE);
            if (!Files.exists(cargoToml)) {
                Files.createDirectories(cargoToml.getParent());
                Files.writeString(cargoToml,
                    "[package]\nname = \"crate_index_update_project\"\nversion = \"0.1.0\"\nedition = \"2021\"\n\n[dependencies]\nserde = \"1.0\"\n");
            }
            Path libRs = projectPath.resolve("src/lib.rs");
            if (!Files.exists(libRs)) {
                Files.createDirectories(libRs.getParent());
                Files.writeString(libRs, "");
            }
            return true;
        } catch (IOException e) {
            LOG.error("Failed to create update project at " + projectPath, e);
            return false;
        }
    }

    private static boolean triggerCratesIoGitIndexUpdate(@Nonnull RsToolchainBase toolchain,
                                                          @Nonnull Disposable disposable,
                                                          @Nonnull Path projectPath) {
        EnvironmentVariablesData envs = EnvironmentVariablesData.create(
            Map.of("CARGO_REGISTRIES_CRATES_IO_PROTOCOL", "git"), true);

        GeneralCommandLine cmdLine = toolchain.createGeneralCommandLine(
            new Cargo(toolchain).getExecutable(),
            projectPath, null, BacktraceMode.FULL, envs,
            List.of("metadata", "--format-version", "1"),
            false, false
        );
        RsResult<?, ?> result = CommandLineExt.execute(cmdLine, disposable, null, null);

        if (result instanceof RsResult.Err) {
            Object err = ((RsResult.Err<?, ?>) result).getErr();
            if (err instanceof Throwable) {
                LOG.error("Failed to update crates.io index", (Throwable) err);
            } else {
                LOG.error("Failed to update crates.io index: " + err);
            }
        }
        return result.isOk();
    }
}
