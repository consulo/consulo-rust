/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.settings.ui;

import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.wsl.WslPath;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.dsl.builder.Panel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.RsToolchainPathChoosingComboBox;
import org.rust.cargo.project.settings.RustProjectSettingsService;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.RsToolchainProvider;
import org.rust.cargo.toolchain.flavors.RsToolchainFlavor;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.openapiext.UiDebouncer;
import org.rust.openapiext.UiUtil;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;

public class RustProjectSettingsPanel implements Disposable {

    private final Path cargoProjectDir;
    @Nullable
    private final Runnable updateListener;
    private final UiDebouncer versionUpdateDebouncer;
    private final RsToolchainPathChoosingComboBox pathToToolchainComboBox;
    private final com.intellij.openapi.ui.TextFieldWithBrowseButton pathToStdlibField;
    private String fetchedSysroot;
    private final ActionLink downloadStdlibLink;
    private final JLabel toolchainVersion;

    public RustProjectSettingsPanel(@NotNull Path cargoProjectDir, @Nullable Runnable updateListener) {
        this.cargoProjectDir = cargoProjectDir;
        this.updateListener = updateListener;
        this.versionUpdateDebouncer = new UiDebouncer(this, 200);
        this.pathToToolchainComboBox = new RsToolchainPathChoosingComboBox(() -> update());
        this.pathToStdlibField = UiUtil.pathToDirectoryTextField(this,
            RsBundle.message("settings.rust.toolchain.select.standard.library.dialog.title"));
        this.downloadStdlibLink = new ActionLink(RsBundle.message("settings.rust.toolchain.download.rustup.link"), e -> {
            // download stdlib action
        });
        this.downloadStdlibLink.setVisible(false);
        this.toolchainVersion = new JLabel();
    }

    public RustProjectSettingsPanel() {
        this(Paths.get("."), null);
    }

    @Override
    public void dispose() {
        Disposer.dispose(pathToToolchainComboBox);
    }

    @NotNull
    public Data getData() {
        RsToolchainBase toolchain = null;
        Path selectedPath = pathToToolchainComboBox.getSelectedPath();
        if (selectedPath != null) {
            toolchain = RsToolchainProvider.getToolchainStatic(selectedPath);
        }
        String stdlibText = pathToStdlibField.getText();
        String explicitPathToStdlib = (stdlibText != null && !stdlibText.isBlank())
            ? (toolchain != null && !Objects.equals(stdlibText, fetchedSysroot) ? stdlibText : null)
            : null;

        return new Data(toolchain, explicitPathToStdlib);
    }

    public void setData(@NotNull Data value) {
        if (value.toolchain != null) {
            pathToToolchainComboBox.setSelectedPath(value.toolchain.getLocation());
        }
        pathToStdlibField.setText(value.explicitPathToStdlib != null ? value.explicitPathToStdlib : "");
        update();
    }

    public void attachTo(@NotNull Panel panel) {
        RustProjectSettingsService service = ProjectManager.getInstance().getDefaultProject()
            .getService(RustProjectSettingsService.class);

        Data data = new Data(
            service != null ? service.getToolchain() : RsToolchainBase.suggest(cargoProjectDir),
            null
        );
        setData(data);

        pathToToolchainComboBox.addToolchainsAsync(() -> {
            LinkedHashSet<Path> paths = new LinkedHashSet<>();
            for (RsToolchainFlavor flavor : RsToolchainFlavor.getApplicableFlavors()) {
                java.util.Iterator<Path> iterator = flavor.suggestHomePaths().iterator();
                while (iterator.hasNext()) {
                    paths.add(iterator.next());
                }
            }
            return new ArrayList<>(paths);
        });
    }

    public void validateSettings() throws ConfigurationException {
        Data data = getData();
        if (data.toolchain == null) return;
        if (!data.toolchain.looksLikeValidToolchain()) {
            throw new ConfigurationException(
                RsBundle.message("settings.rust.toolchain.invalid.toolchain.error", data.toolchain.getLocation())
            );
        }
    }

    private void update() {
        // Simplified update logic
        if (updateListener != null) {
            updateListener.run();
        }
    }

    public static class Data {
        @Nullable
        public final RsToolchainBase toolchain;
        @Nullable
        public final String explicitPathToStdlib;

        public Data(@Nullable RsToolchainBase toolchain, @Nullable String explicitPathToStdlib) {
            this.toolchain = toolchain;
            this.explicitPathToStdlib = explicitPathToStdlib;
        }

        @Nullable
        public RsToolchainBase getToolchain() {
            return toolchain;
        }

        @Nullable
        public String getExplicitPathToStdlib() {
            return explicitPathToStdlib;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Data data)) return false;
            return Objects.equals(toolchain, data.toolchain)
                && Objects.equals(explicitPathToStdlib, data.explicitPathToStdlib);
        }

        @Override
        public int hashCode() {
            return Objects.hash(toolchain, explicitPathToStdlib);
        }
    }
}
