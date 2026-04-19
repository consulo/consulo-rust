/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.command.CargoCommandConfigurationType;
import org.rust.cargo.runconfig.wasmpack.WasmPackCommandConfiguration;
import org.rust.cargo.runconfig.wasmpack.WasmPackCommandConfigurationType;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.stdext.RsResult;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.StdextUtil;

import java.nio.file.Path;
import org.rust.lang.core.psi.ext.RsPathUtil;

/**
 * Utilities for creating new Rust projects.
 */
public final class NewProjectUtil {

    private NewProjectUtil() {
    }

    @NotNull
    public static RsResult<Cargo.GeneratedFilesHolder, org.rust.openapiext.RsProcessExecutionException> makeProject(
        @NotNull Cargo cargo,
        @NotNull Project project,
        @NotNull Module module,
        @NotNull VirtualFile baseDir,
        @NotNull String name,
        @NotNull RsProjectTemplate template,
        @Nullable String vcs
    ) {
        if (template instanceof RsGenericTemplate) {
            boolean isBinary = (template == RsGenericTemplate.CargoBinaryTemplate);
            return cargo.init(project, module, baseDir, name, isBinary, vcs);
        } else if (template instanceof RsCustomTemplate) {
            return cargo.generate(project, module, baseDir, name, ((RsCustomTemplate) template).getUrl(), vcs);
        }
        throw new IllegalArgumentException("Unknown template type: " + template);
    }

    public static void openFiles(@NotNull Project project, @NotNull Cargo.GeneratedFilesHolder files) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!OpenApiUtil.isHeadlessEnvironment()) {
                PsiNavigationSupport navigation = PsiNavigationSupport.getInstance();
                navigation.createNavigatable(project, files.getManifest(), -1).navigate(false);
                for (VirtualFile file : files.getSourceFiles()) {
                    navigation.createNavigatable(project, file, -1).navigate(true);
                }
            }
        });
    }

    public static void makeDefaultRunConfiguration(@NotNull Project project, @NotNull RsProjectTemplate template) {
        RunManager runManager = RunManager.getInstance(project);
        String projectName = project.getName().replace(' ', '_');

        RunnerAndConfigurationSettings configuration;
        if (template == RsGenericTemplate.CargoBinaryTemplate) {
            configuration = createCargoRunConfiguration(runManager, project, projectName);
        } else if (template == RsGenericTemplate.CargoLibraryTemplate) {
            configuration = createCargoTestConfiguration(runManager, project, projectName);
        } else if (template == RsCustomTemplate.WasmPackTemplate) {
            configuration = createWasmPackBuildConfiguration(runManager, project);
        } else {
            return;
        }

        runManager.addConfiguration(configuration);
        runManager.setSelectedConfiguration(configuration);
    }

    @NotNull
    private static RunnerAndConfigurationSettings createCargoRunConfiguration(
        @NotNull RunManager runManager,
        @NotNull Project project,
        @NotNull String projectName
    ) {
        RunnerAndConfigurationSettings settings = runManager.createConfiguration(
            "Run", CargoCommandConfigurationType.getInstance().getFactory()
        );
        if (settings.getConfiguration() instanceof CargoCommandConfiguration) {
            CargoCommandConfiguration config = (CargoCommandConfiguration) settings.getConfiguration();
            config.setCommand("run --package " + projectName + " --bin " + projectName);
            String basePath = project.getBasePath();
            if (basePath != null) {
                config.setWorkingDirectory(Path.of(basePath));
            }
        }
        return settings;
    }

    @NotNull
    private static RunnerAndConfigurationSettings createCargoTestConfiguration(
        @NotNull RunManager runManager,
        @NotNull Project project,
        @NotNull String projectName
    ) {
        RunnerAndConfigurationSettings settings = runManager.createConfiguration(
            "Test", CargoCommandConfigurationType.getInstance().getFactory()
        );
        if (settings.getConfiguration() instanceof CargoCommandConfiguration) {
            CargoCommandConfiguration config = (CargoCommandConfiguration) settings.getConfiguration();
            config.setCommand("test --package " + projectName + " --lib tests");
            String basePath = project.getBasePath();
            if (basePath != null) {
                config.setWorkingDirectory(Path.of(basePath));
            }
        }
        return settings;
    }

    @NotNull
    private static RunnerAndConfigurationSettings createWasmPackBuildConfiguration(
        @NotNull RunManager runManager,
        @NotNull Project project
    ) {
        RunnerAndConfigurationSettings settings = runManager.createConfiguration(
            "Build", WasmPackCommandConfigurationType.getInstance().getFactory()
        );
        if (settings.getConfiguration() instanceof WasmPackCommandConfiguration) {
            WasmPackCommandConfiguration config = (WasmPackCommandConfiguration) settings.getConfiguration();
            String basePath = project.getBasePath();
            if (basePath != null) {
                config.setWorkingDirectory(Path.of(basePath));
            }
        }
        return settings;
    }
}
