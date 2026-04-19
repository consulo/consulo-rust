/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.crates.local;

import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.toolchain.BacktraceMode;
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

@Service
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

        long lastUpdate = PropertiesComponent.getInstance().getLong(CRATES_IO_INDEX_LAST_UPDATE, 0);
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
            public void run(@NotNull ProgressIndicator indicator) {
                LOG.info("crates.io index update started");
                boolean isSuccessful = updateCratesIoGitIndex(CratesLocalIndexUpdater.this);
                if (isSuccessful) {
                    PropertiesComponent.getInstance().setValue(CRATES_IO_INDEX_LAST_UPDATE, String.valueOf(System.currentTimeMillis()));
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
            myAlarm.addRequest(this::updateCratesIoGitIndex, delay, true);
            LOG.info("crates.io index update is scheduled in " + delay + " ms");
        }
    }

    @Override
    public void dispose() {}

    @NotNull
    public static CratesLocalIndexUpdater getInstance() {
        return ApplicationManager.getApplication().getService(CratesLocalIndexUpdater.class);
    }

    private static boolean hasOpenRustProject() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            CargoProjectsService service = project.getServiceIfCreated(CargoProjectsService.class);
            if (service != null && !service.getAllProjects().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static int getUpdateIntervalMillis() {
        int intervalMin = Math.max(MIN_UPDATE_INTERVAL_MIN,
            Registry.intValue("org.rust.crates.local.index.update.interval", DEFAULT_UPDATE_INTERVAL_MIN));
        long intervalMillis = TimeUnit.MINUTES.toMillis(intervalMin);
        return intervalMillis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) intervalMillis;
    }

    @VisibleForTesting
    public static boolean updateCratesIoGitIndex(@NotNull Disposable disposable) {
        Path projectPath = RsPathManager.pluginDirInSystem().resolve(UPDATE_PROJECT_DIR_NAME);
        boolean projectCreated = createUpdateProjectIfNeeded(projectPath);
        if (!projectCreated) return false;

        RsToolchainBase toolchain = RsToolchainBase.suggest(projectPath);
        if (toolchain == null) return false;
        return triggerCratesIoGitIndexUpdate(toolchain, disposable, projectPath);
    }

    private static boolean createUpdateProjectIfNeeded(@NotNull Path projectPath) {
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

    private static boolean triggerCratesIoGitIndexUpdate(@NotNull RsToolchainBase toolchain,
                                                          @NotNull Disposable disposable,
                                                          @NotNull Path projectPath) {
        EnvironmentVariablesData envs = EnvironmentVariablesData.create(
            Map.of("CARGO_REGISTRIES_CRATES_IO_PROTOCOL", "git"), true);

        com.intellij.execution.configurations.GeneralCommandLine cmdLine = toolchain.createGeneralCommandLine(
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
