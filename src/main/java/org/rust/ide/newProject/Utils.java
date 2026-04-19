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
import org.rust.cargo.toolchain.tools.Cargo.GeneratedFilesHolder;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.PathUtil;
import org.rust.stdext.RsResult;

import java.nio.file.Path;
import org.rust.lang.core.psi.ext.RsPathUtil;

public final class Utils {

    private Utils() {
    }

    @NotNull
    public static RsResult<GeneratedFilesHolder, org.rust.openapiext.RsProcessExecutionException> makeProject(
        @NotNull Cargo cargo,
        @NotNull Project project,
        @NotNull Module module,
        @NotNull VirtualFile baseDir,
        @NotNull String name,
        @NotNull RsProjectTemplate template,
        @Nullable String vcs
    ) {
        if (template instanceof RsGenericTemplate) {
            return cargo.init(project, module, baseDir, name, template.isBinary(), vcs);
        } else if (template instanceof RsCustomTemplate) {
            return cargo.generate(project, module, baseDir, name, ((RsCustomTemplate) template).getUrl(), vcs);
        }
        throw new IllegalArgumentException("Unknown template type: " + template);
    }

    @NotNull
    public static RsResult<GeneratedFilesHolder, org.rust.openapiext.RsProcessExecutionException> makeProject(
        @NotNull Cargo cargo,
        @NotNull Project project,
        @NotNull Module module,
        @NotNull VirtualFile baseDir,
        @NotNull String name,
        @NotNull RsProjectTemplate template
    ) {
        return makeProject(cargo, project, module, baseDir, name, template, null);
    }

    public static void openFiles(@NotNull Project project, @NotNull GeneratedFilesHolder files) {
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
        String cargoProjectName = project.getName().replace(' ', '_');

        RunnerAndConfigurationSettings configuration;
        if (template == RsGenericTemplate.CargoBinaryTemplate) {
            configuration = createCargoRunConfiguration(runManager, project, cargoProjectName);
        } else if (template == RsGenericTemplate.CargoLibraryTemplate) {
            configuration = createCargoTestConfiguration(runManager, project, cargoProjectName);
        } else if (template == RsCustomTemplate.WasmPackTemplate) {
            configuration = createWasmPackBuildConfiguration(runManager, project);
        } else if (template instanceof RsCustomTemplate) {
            return;
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
        @NotNull String cargoProjectName
    ) {
        RunnerAndConfigurationSettings settings =
            runManager.createConfiguration("Run", CargoCommandConfigurationType.getInstance().getFactory());
        if (settings.getConfiguration() instanceof CargoCommandConfiguration cargoConfig) {
            cargoConfig.setCommand("run --package " + cargoProjectName + " --bin " + cargoProjectName);
            String basePath = project.getBasePath();
            if (basePath != null) {
                cargoConfig.setWorkingDirectory(PathUtil.toPath(basePath));
            }
        }
        return settings;
    }

    @NotNull
    private static RunnerAndConfigurationSettings createCargoTestConfiguration(
        @NotNull RunManager runManager,
        @NotNull Project project,
        @NotNull String cargoProjectName
    ) {
        RunnerAndConfigurationSettings settings =
            runManager.createConfiguration("Test", CargoCommandConfigurationType.getInstance().getFactory());
        if (settings.getConfiguration() instanceof CargoCommandConfiguration cargoConfig) {
            cargoConfig.setCommand("test --package " + cargoProjectName + " --lib tests");
            String basePath = project.getBasePath();
            if (basePath != null) {
                cargoConfig.setWorkingDirectory(PathUtil.toPath(basePath));
            }
        }
        return settings;
    }

    @NotNull
    private static RunnerAndConfigurationSettings createWasmPackBuildConfiguration(
        @NotNull RunManager runManager,
        @NotNull Project project
    ) {
        RunnerAndConfigurationSettings settings =
            runManager.createConfiguration("Build", WasmPackCommandConfigurationType.getInstance().getFactory());
        if (settings.getConfiguration() instanceof WasmPackCommandConfiguration wasmConfig) {
            String basePath = project.getBasePath();
            if (basePath != null) {
                wasmConfig.setWorkingDirectory(PathUtil.toPath(basePath));
            }
        }
        return settings;
    }
}
