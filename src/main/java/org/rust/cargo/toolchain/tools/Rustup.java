/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.tools;

import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.runconfig.wasmpack.WasmPackBuildTaskProvider;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.util.DownloadResult;
import org.rust.ide.actions.InstallComponentAction;
import org.rust.ide.actions.InstallTargetAction;
import org.rust.ide.notifications.NotificationUtils;
import org.rust.openapiext.*;
import org.rust.stdext.RsResult;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Rustup extends RsTool {

    private static final Logger LOG = Logger.getInstance(Rustup.class);

    public static final String NAME = "rustup";

    private final Path projectDirectory;

    public Rustup(RsToolchainBase toolchain, Path projectDirectory) {
        super(NAME, toolchain);
        this.projectDirectory = projectDirectory;
    }

    // ---- Component ----

    public static final class Component {
        private final String name;
        private final boolean isInstalled;

        public Component(String name, boolean isInstalled) {
            this.name = name;
            this.isInstalled = isInstalled;
        }

        public String getName() { return name; }
        public boolean isInstalled() { return isInstalled; }

        public static Component from(String line) {
            String name = line.contains(" ") ? line.substring(0, line.indexOf(' ')) : line;
            String rest = line.contains(" ") ? line.substring(line.indexOf(' ') + 1) : "";
            boolean isInstalled = "(installed)".equals(rest) || "(default)".equals(rest);
            return new Component(name, isInstalled);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Component)) return false;
            Component that = (Component) o;
            return isInstalled == that.isInstalled && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, isInstalled);
        }
    }

    // ---- Target ----

    public static final class Target {
        private final String name;
        private final boolean isInstalled;

        public Target(String name, boolean isInstalled) {
            this.name = name;
            this.isInstalled = isInstalled;
        }

        public String getName() { return name; }
        public boolean isInstalled() { return isInstalled; }

        public static Target from(String line) {
            String name = line.contains(" ") ? line.substring(0, line.indexOf(' ')) : line;
            String rest = line.contains(" ") ? line.substring(line.indexOf(' ') + 1) : "";
            boolean isInstalled = "(installed)".equals(rest) || "(default)".equals(rest);
            return new Target(name, isInstalled);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Target)) return false;
            Target that = (Target) o;
            return isInstalled == that.isInstalled && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, isInstalled);
        }
    }

    private List<Component> listComponents() {
        ProcessOutput output = CommandLineExt.execute(
            createBaseCommandLine(
                new String[]{"component", "list"},
                projectDirectory,
                Collections.emptyMap()
            ),
            (Integer) getToolchain().getExecutionTimeoutInMilliseconds()
        );
        if (output == null) return Collections.emptyList();
        return output.getStdoutLines().stream()
            .map(Component::from)
            .collect(Collectors.toList());
    }

    private List<Target> listTargets() {
        ProcessOutput output = CommandLineExt.execute(
            createBaseCommandLine(
                new String[]{"target", "list"},
                projectDirectory,
                Collections.emptyMap()
            ),
            (Integer) getToolchain().getExecutionTimeoutInMilliseconds()
        );
        if (output == null) return Collections.emptyList();
        return output.getStdoutLines().stream()
            .map(Target::from)
            .collect(Collectors.toList());
    }

    public DownloadResult<VirtualFile> downloadStdlib(@Nullable Disposable owner, @Nullable ProcessListener listener) {
        // Sometimes we have stdlib but don't have write access to install it
        if (needInstallComponent("rust-src")) {
            com.intellij.execution.configurations.GeneralCommandLine commandLine = createBaseCommandLine(
                new String[]{"component", "add", "rust-src"},
                projectDirectory,
                Collections.emptyMap()
            );

            ProcessOutput downloadProcessOutput;
            if (owner == null) {
                downloadProcessOutput = CommandLineExt.execute(commandLine, (Integer) null);
            } else {
                RsResult<ProcessOutput, RsProcessExecutionException> result =
                    CommandLineExt.execute(
                        commandLine,
                        owner,
                        null,
                        listener
                    );
                downloadProcessOutput = result instanceof RsResult.Ok
                    ? ((RsResult.Ok<ProcessOutput, RsProcessExecutionException>) result).getOk()
                    : null;
            }

            if (downloadProcessOutput == null || !CommandLineExt.isSuccess(downloadProcessOutput)) {
                String stderr = downloadProcessOutput != null ? downloadProcessOutput.getStderr() : "";
                String message = RsBundle.message("notification.content.rustup.failed2", stderr);
                LOG.warn(message);
                return new DownloadResult.Err<>(message);
            }
        }

        Rustc rustc = Rustc.create(getToolchain());
        VirtualFile sources = rustc.getStdlibFromSysroot(projectDirectory);
        if (sources == null) {
            return new DownloadResult.Err<>(RsBundle.message("notification.content.failed.to.find.stdlib.in.sysroot"));
        }
        LOG.info("stdlib path: " + sources.getPath());
        org.rust.openapiext.OpenApiUtil.fullyRefreshDirectory(sources);
        return new DownloadResult.Ok<>(sources);
    }

    public DownloadResult<Void> downloadComponent(Disposable owner, String componentName) {
        return convertResult(
            CommandLineExt.execute(
                createBaseCommandLine(
                    new String[]{"component", "add", componentName},
                    projectDirectory,
                    Collections.emptyMap()
                ),
                owner,
                null,
                null
            )
        );
    }

    public DownloadResult<Void> downloadTarget(Disposable owner, String targetName) {
        return convertResult(
            CommandLineExt.execute(
                createBaseCommandLine(
                    new String[]{"target", "add", targetName},
                    projectDirectory,
                    Collections.emptyMap()
                ),
                owner,
                null,
                null
            )
        );
    }

    @Nullable
    public String activeToolchainName() {
        ProcessOutput output = CommandLineExt.execute(
            createBaseCommandLine(
                new String[]{"show", "active-toolchain"},
                projectDirectory,
                Collections.emptyMap()
            ),
            (Integer) getToolchain().getExecutionTimeoutInMilliseconds()
        );
        if (output == null || !CommandLineExt.isSuccess(output)) return null;
        return output.getStdout().contains("(")
            ? output.getStdout().substring(0, output.getStdout().indexOf('(')).trim()
            : output.getStdout().trim();
    }

    private <T> DownloadResult<Void> convertResult(RsResult<ProcessOutput, RsProcessExecutionException> result) {
        if (result instanceof RsResult.Ok) {
            return new DownloadResult.Ok<>(null);
        } else {
            @SuppressWarnings("unchecked")
            RsProcessExecutionException err = ((RsResult.Err<ProcessOutput, RsProcessExecutionException>) result).err();
            String message = RsBundle.message("notification.content.rustup.failed", err.getMessage() != null ? err.getMessage() : "");
            LOG.warn(message);
            return new DownloadResult.Err<>(message);
        }
    }

    private boolean needInstallComponent(String componentName) {
        Boolean isInstalled = listComponents().stream()
            .filter(c -> c.getName().startsWith(componentName))
            .map(Component::isInstalled)
            .findFirst()
            .orElse(null);
        if (isInstalled == null) return false;
        return !isInstalled;
    }

    private boolean needInstallTarget(String targetName) {
        Boolean isInstalled = listTargets().stream()
            .filter(t -> t.getName().equals(targetName))
            .map(Target::isInstalled)
            .findFirst()
            .orElse(null);
        if (isInstalled == null) return false;
        return !isInstalled;
    }

    // ---- Static methods ----

    public static boolean isRustupAvailable(RsToolchainBase toolchain) {
        return toolchain.hasExecutable(NAME);
    }

    @Nullable
    public static Rustup create(RsToolchainBase toolchain, Path cargoProjectDirectory) {
        if (!isRustupAvailable(toolchain)) return null;
        return new Rustup(toolchain, cargoProjectDirectory);
    }

    public static boolean checkNeedInstallClippy(Project project, Path cargoProjectDirectory) {
        return checkNeedInstallComponent(project, cargoProjectDirectory, "clippy");
    }

    public static boolean checkNeedInstallRustfmt(Project project, Path cargoProjectDirectory) {
        return checkNeedInstallComponent(project, cargoProjectDirectory, "rustfmt");
    }

    public static boolean checkNeedInstallLlvmTools(Project project, Path cargoProjectDirectory) {
        return checkNeedInstallComponent(project, cargoProjectDirectory, "llvm-tools-preview", "llvm-tools");
    }

    public static boolean checkNeedInstallWasmTarget(Project project, Path cargoProjectDirectory) {
        return checkNeedInstallTarget(project, cargoProjectDirectory, WasmPackBuildTaskProvider.WASM_TARGET);
    }

    private static boolean checkNeedInstallComponent(
        Project project,
        Path cargoProjectDirectory,
        String componentName
    ) {
        return checkNeedInstallComponent(project, cargoProjectDirectory, componentName, org.rust.stdext.Utils.capitalized(componentName));
    }

    private static boolean checkNeedInstallComponent(
        Project project,
        Path cargoProjectDirectory,
        String componentName,
        String componentPresentableName
    ) {
        RsToolchainBase toolchain = RsProjectSettingsServiceUtil.getToolchain(project);
        if (toolchain == null) return false;
        Rustup rustup = create(toolchain, cargoProjectDirectory);
        if (rustup == null) return false;
        boolean needInstall = rustup.needInstallComponent(componentName);

        if (needInstall) {
            NotificationUtils.showBalloon(
                project,
                RsBundle.message("notification.content.not.installed", componentPresentableName),
                NotificationType.ERROR,
                new InstallComponentAction(cargoProjectDirectory, componentName)
            );
        }

        return needInstall;
    }

    public static boolean checkNeedInstallTarget(
        Project project,
        Path cargoProjectDirectory,
        String targetName
    ) {
        RsToolchainBase toolchain = RsProjectSettingsServiceUtil.getToolchain(project);
        if (toolchain == null) return false;
        Rustup rustup = create(toolchain, cargoProjectDirectory);
        if (rustup == null) return false;
        boolean needInstall = rustup.needInstallTarget(targetName);

        if (needInstall) {
            NotificationUtils.showBalloon(
                project,
                RsBundle.message("notification.content.target.not.installed", targetName),
                NotificationType.ERROR,
                new InstallTargetAction(cargoProjectDirectory, targetName)
            );
        }

        return needInstall;
    }
}
