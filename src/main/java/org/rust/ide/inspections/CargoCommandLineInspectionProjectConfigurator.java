/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.ide.CommandLineInspectionProgressReporter;
import com.intellij.ide.CommandLineInspectionProjectConfigurator;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import org.rust.RsBundle;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.impl.CargoProjectsServiceImpl;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.lang.core.macros.MacroExpansionTaskListener;
import org.rust.RsProjectTaskQueueService;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;

public class CargoCommandLineInspectionProjectConfigurator implements CommandLineInspectionProjectConfigurator {

    @NotNull
    @Override
    public String getName() {
        return "cargo";
    }

    @NotNull
    @Override
    public String getDescription() {
        return RsBundle.message("cargo.commandline.description");
    }

    @Override
    public boolean isApplicable(@NotNull ConfiguratorContext context) {
        // TODO: find all Cargo.toml in the project
        return Files.exists(context.getProjectPath().resolve(CargoConstants.MANIFEST_FILE));
    }

    @Override
    public void configureEnvironment(@NotNull ConfiguratorContext context) {
        System.setProperty(CargoProjectsServiceImpl.CARGO_DISABLE_PROJECT_REFRESH_ON_CREATION, "true");
        // See `com.intellij.openapi.externalSystem.autoimport.AutoImportProjectTracker.isDisabledAutoReload`
        Registry.get("external.system.auto.import.disabled").setValue(true);
    }

    @Override
    public void preConfigureProject(@NotNull Project project, @NotNull ConfiguratorContext context) {
        RsProjectSettingsServiceUtil.getRustSettings(project).modify(it -> {
        });
    }

    @Override
    public void configureProject(@NotNull Project project, @NotNull ConfiguratorContext context) {
        LoggerWrapper logger = new LoggerWrapper(context.getLogger(), Logger.getInstance(CargoCommandLineInspectionProjectConfigurator.class));

        CountDownLatch refreshStarted = new CountDownLatch(1);
        CountDownLatch refreshFinished = new CountDownLatch(1);
        CountDownLatch macroExpansionFinished = new CountDownLatch(1);
        var connection = project.getMessageBus().connect();
        connection.subscribe(
            CargoProjectsService.CARGO_PROJECTS_REFRESH_TOPIC,
            new CargoProjectsService.CargoProjectsRefreshListener() {
                @Override
                public void onRefreshStarted() {
                    logger.info("Cargo project model loading...");
                    refreshStarted.countDown();
                }

                @Override
                public void onRefreshFinished(@NotNull CargoProjectsService.CargoRefreshStatus status) {
                    logger.info("Cargo project model loading finished: " + status);
                    refreshFinished.countDown();
                }
            }
        );
        connection.subscribe(
            MacroExpansionTaskListener.MACRO_EXPANSION_TASK_TOPIC,
            new MacroExpansionTaskListener() {
                @Override
                public void onMacroExpansionTaskFinished() {
                    macroExpansionFinished.countDown();
                }
            }
        );

        CargoProjectsService cargoProjectsService = CargoProjectServiceUtil.getCargoProjects(project);

        boolean result = CargoProjectServiceUtil.guessAndSetupRustProject(project, true);
        if (!result) {
            if (cargoProjectsService.getHasAtLeastOneValidProject()) {
                cargoProjectsService.refreshAllProjects();
            } else {
                logger.error("Cargo project model loading failed to start");
                return;
            }
        }

        ProgressIndicatorUtils.awaitWithCheckCanceled(refreshStarted);
        ProgressIndicatorUtils.awaitWithCheckCanceled(refreshFinished);

        for (CargoProject cargoProject : cargoProjectsService.getAllProjects()) {
            CargoProject.UpdateStatus status = cargoProject.getMergedStatus();
            if (status instanceof CargoProject.UpdateStatus.UpdateFailed) {
                logger.error(((CargoProject.UpdateStatus.UpdateFailed) status).getReason());
            }
        }

        logger.info("Expanding Rust macros...");
        ProgressIndicatorUtils.awaitWithCheckCanceled(macroExpansionFinished);

        // Ensure all Rust plugin tasks has been finished
        var taskQueue = RsProjectTaskQueueService.getInstance(project);
        if (!taskQueue.isEmpty()) {
            while (!taskQueue.isEmpty()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        logger.info("Rust macro expansion has been finished");
    }

    private static class LoggerWrapper {
        private final CommandLineInspectionProgressReporter inspectionProgressReporter;
        private final Logger logger;

        LoggerWrapper(CommandLineInspectionProgressReporter inspectionProgressReporter, Logger logger) {
            this.inspectionProgressReporter = inspectionProgressReporter;
            this.logger = logger;
        }

        void info(String message) {
            logger.info(message);
            inspectionProgressReporter.reportMessage(1, message);
        }

        void error(String message) {
            logger.error(message);
            inspectionProgressReporter.reportError(message);
        }
    }
}
