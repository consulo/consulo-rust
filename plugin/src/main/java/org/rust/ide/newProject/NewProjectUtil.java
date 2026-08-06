/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject;

import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.language.psi.PsiNavigationSupport;
import consulo.application.ApplicationManager;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

    @Nonnull
    public static RsResult<Cargo.GeneratedFilesHolder, org.rust.openapiext.RsProcessExecutionException> makeProject(
        @Nonnull Cargo cargo,
        @Nonnull Project project,
        @Nonnull Module module,
        @Nonnull VirtualFile baseDir,
        @Nonnull String name,
        @Nonnull RsProjectTemplate template,
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

    public static void openFiles(@Nonnull Project project, @Nonnull Cargo.GeneratedFilesHolder files) {
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

    public static void makeDefaultRunConfiguration(@Nonnull Project project, @Nonnull RsProjectTemplate template) {
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

    @Nonnull
    private static RunnerAndConfigurationSettings createCargoRunConfiguration(
        @Nonnull RunManager runManager,
        @Nonnull Project project,
        @Nonnull String projectName
    ) {
        RunnerAndConfigurationSettings settings = runManager.createRunConfiguration(
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

    @Nonnull
    private static RunnerAndConfigurationSettings createCargoTestConfiguration(
        @Nonnull RunManager runManager,
        @Nonnull Project project,
        @Nonnull String projectName
    ) {
        RunnerAndConfigurationSettings settings = runManager.createRunConfiguration(
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

    @Nonnull
    private static RunnerAndConfigurationSettings createWasmPackBuildConfiguration(
        @Nonnull RunManager runManager,
        @Nonnull Project project
    ) {
        RunnerAndConfigurationSettings settings = runManager.createRunConfiguration(
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
