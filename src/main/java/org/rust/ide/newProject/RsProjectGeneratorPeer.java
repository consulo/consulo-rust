/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.platform.GeneratorPeerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.newProject.ui.RsNewProjectPanel;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.rust.openapiext.UiUtil;
import org.rust.stdext.BuilderUtil;

public class RsProjectGeneratorPeer extends GeneratorPeerImpl<ConfigurationData> {

    private final RsNewProjectPanel newProjectPanel;
    @Nullable
    public Runnable checkValid;

    public RsProjectGeneratorPeer() {
        this(Paths.get("."));
    }

    public RsProjectGeneratorPeer(@NotNull Path cargoProjectDir) {
        newProjectPanel = new RsNewProjectPanel(true, cargoProjectDir, () -> {
            if (checkValid != null) {
                checkValid.run();
            }
        });
    }

    @NotNull
    @Override
    public ConfigurationData getSettings() {
        return newProjectPanel.getData();
    }

    @NotNull
    @Override
    public JComponent getComponent(@NotNull TextFieldWithBrowseButton myLocationField, @NotNull Runnable checkValid) {
        this.checkValid = checkValid;
        return super.getComponent(myLocationField, checkValid);
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return BuilderUtil.panel(builder -> {
            newProjectPanel.attachTo(builder);
            return null;
        });
    }

    @Nullable
    @Override
    public ValidationInfo validate() {
        try {
            newProjectPanel.validateSettings();
            return null;
        } catch (ConfigurationException e) {
            String message = e.getMessage();
            return new ValidationInfo(message != null ? message : "");
        }
    }
}
