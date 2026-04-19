/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.tools;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.project.workspace.CargoWorkspace.Edition;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.RustChannel;
import org.rust.ide.actions.RustfmtEditSettingsAction;
import org.rust.ide.notifications.NotificationUtils;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.*;
import org.rust.stdext.RsResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Rustfmt extends RustupComponent {

    public static final String NAME = "rustfmt";
    private static final List<String> CONFIG_FILES = List.of("rustfmt.toml", ".rustfmt.toml");

    public Rustfmt(RsToolchainBase toolchain) {
        super(NAME, toolchain);
    }

    @Nullable
    public String reformatDocumentTextOrNull(CargoProject cargoProject, Document document) {
        Project project = cargoProject.getProject();
        RsResult<ProcessOutput, RsProcessExecutionException> result = reformatTextDocument(cargoProject, document, project);
        if (result == null) return null;
        if (result instanceof RsResult.Err) {
            RsProcessExecutionException err = ((RsResult.Err<ProcessOutput, RsProcessExecutionException>) result).getErr();
            showRustfmtError(err, project);
            if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) throw new RuntimeException(err);
            return null;
        }
        String stdout = ((RsResult.Ok<ProcessOutput, RsProcessExecutionException>) result).getOk().getStdout();
        return (stdout != null && !stdout.isEmpty()) ? stdout : null;
    }

    @Nullable
    public RsResult<ProcessOutput, RsProcessExecutionException> reformatTextDocument(
        CargoProject cargoProject,
        Document document,
        Project project
    ) {
        GeneralCommandLine commandLine = createCommandLine(cargoProject, document);
        if (commandLine == null) return null;
        return CommandLineExt.execute(
            commandLine,
            project,
            document.getText().getBytes(),
            null
        );
    }

    @Nullable
    public GeneralCommandLine createCommandLine(CargoProject cargoProject, Document document) {
        VirtualFile file = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(document);
        if (file == null) return null;
        if (RsFile.isNotRustFile(file) || !file.isValid()) return null;

        Project project = cargoProject.getProject();
        org.rust.cargo.project.settings.RustfmtProjectSettingsService settings = RsProjectSettingsServiceUtil.getRustfmtSettings(project);

        List<String> arguments = ParametersListUtil.parse(settings.getAdditionalArguments());
        List<String> cleanArguments = new ArrayList<>();

        String toolchainArg = (!arguments.isEmpty() && arguments.get(0).startsWith("+"))
            ? arguments.get(0) : null;

        if (settings.getChannel() != RustChannel.DEFAULT) {
            cleanArguments.add("+" + settings.getChannel());
        } else if (toolchainArg != null) {
            cleanArguments.add(toolchainArg);
        }

        int idx = toolchainArg == null ? 0 : 1;
        while (idx < arguments.size()) {
            String arg = arguments.get(idx);
            if ("--emit".equals(arg)) {
                idx += 2;
            } else if (arg.startsWith("--emit")) {
                idx += 1;
            } else {
                cleanArguments.add(arg);
                idx += 1;
            }
        }
        cleanArguments.add("--emit=stdout");

        addArgument(cleanArguments, "config-path", () -> {
            Path configPath = findConfigPathRecursively(
                file.getParent(),
                CargoCommandConfiguration.getWorkingDirectory(cargoProject)
            );
            return configPath != null ? configPath.toString() : null;
        });

        addArgument(cleanArguments, "edition", () -> {
            if (cargoProject.getRustcInfo() == null || cargoProject.getRustcInfo().getVersion() == null) return null;
            Edition edition = ReadAction.compute(() -> {
                com.intellij.psi.PsiFile psiFile = org.rust.openapiext.OpenApiUtil.toPsiFile(file, project);
                if (psiFile != null) {
                    return RsElementUtil.getEdition(psiFile);
                }
                return Edition.DEFAULT;
            });
            return edition.getPresentation();
        });

        return createBaseCommandLine(cleanArguments, CargoCommandConfiguration.getWorkingDirectory(cargoProject), settings.getEnvs());
    }

    public RsResult<Void, RsProcessExecutionException> reformatCargoProject(
        CargoProject cargoProject,
        Disposable owner
    ) {
        Project project = cargoProject.getProject();
        org.rust.cargo.project.settings.RustfmtProjectSettingsService settings = RsProjectSettingsServiceUtil.getRustfmtSettings(project);
        List<String> arguments = new ArrayList<>(ParametersListUtil.parse(settings.getAdditionalArguments()));
        String toolchain = null;
        if (!arguments.isEmpty() && arguments.get(0).startsWith("+")) {
            toolchain = arguments.remove(0);
        }

        List<String> fmtArgs = new ArrayList<>();
        fmtArgs.add("--all");
        fmtArgs.add("--");
        fmtArgs.addAll(arguments);

        CargoCommandLine commandLine = CargoCommandLine.forProject(
            cargoProject,
            "fmt",
            fmtArgs,
            false,
            toolchain,
            settings.getChannel(),
            EnvironmentVariablesData.create(settings.getEnvs(), true)
        );

        String finalToolchain = toolchain;
        return org.rust.openapiext.OpenApiUtil.computeWithCancelableProgress(
            project,
            RsBundle.message("progress.title.reformatting.cargo.project.with.rustfmt"),
            () -> {
                RsToolchainBase tc = RsProjectSettingsServiceUtil.getToolchain(project);
                if (tc == null) return new RsResult.Ok<Void, RsProcessExecutionException>(null);
                Cargo cargo = Cargo.cargoOrWrapper(tc, CargoCommandConfiguration.getWorkingDirectory(cargoProject));
                GeneralCommandLine gcl = cargo.toGeneralCommandLine(project, commandLine);
                return CommandLineExt.execute(
                    gcl,
                    owner,
                    null,
                    null
                )
                    .<Void>map(output -> null)
                    .mapErr(e -> {
                        showRustfmtError(e, project);
                        return e;
                    });
            }
        );
    }

    public RsResult<Void, RsProcessExecutionException> reformatCargoProject(CargoProject cargoProject) {
        return reformatCargoProject(cargoProject, cargoProject.getProject());
    }

    private static void addArgument(List<String> args, String flagName, java.util.function.Supplier<String> valueSupplier) {
        for (String arg : args) {
            if (arg.startsWith("--" + flagName)) return;
        }
        String value = valueSupplier.get();
        if (value == null) return;
        args.add("--" + flagName + "=" + value);
    }

    @Nullable
    private static Path findConfigPathRecursively(VirtualFile directory, Path stopAt) {
        Path path = org.rust.openapiext.OpenApiUtil.getPathAsPath(directory);
        if (!path.startsWith(stopAt) || path.equals(stopAt)) return null;
        for (VirtualFile child : directory.getChildren()) {
            if (CONFIG_FILES.contains(child.getName())) return path;
        }
        return findConfigPathRecursively(directory.getParent(), stopAt);
    }

    private static void showRustfmtError(RsProcessExecutionException exception, Project project) {
        String message = exception.getMessage();
        if (message == null) message = "";
        message = message.trim();
        if (!message.isEmpty()) {
            String html = "<html>" + org.rust.openapiext.OpenApiUtil.getEscaped(message).replace("\n", "<br>") + "</html>";
            NotificationUtils.showBalloon(
                project,
                RsBundle.message("notification.title.rustfmt"),
                html,
                NotificationType.ERROR,
                new RustfmtEditSettingsAction(RsBundle.message("action.show.settings.text")),
                null
            );
        }
    }

    public static Rustfmt create(RsToolchainBase toolchain) {
        return new Rustfmt(toolchain);
    }

    @org.jetbrains.annotations.Nullable
    public static Rustfmt rustfmt(@org.jetbrains.annotations.Nullable RsToolchainBase toolchain) {
        if (toolchain == null) return null;
        return new Rustfmt(toolchain);
    }
}
