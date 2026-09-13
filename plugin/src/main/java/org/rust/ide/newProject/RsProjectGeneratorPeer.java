/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject;

import consulo.configurable.ConfigurationException;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.ui.ex.awt.ValidationInfo;
import com.intellij.platform.GeneratorPeerImpl;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.newProject.ui.RsNewProjectPanel;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.rust.openapiext.ui.UiUtil;
import org.rust.stdext.BuilderUtil;

public class RsProjectGeneratorPeer extends GeneratorPeerImpl<ConfigurationData> {

    private final RsNewProjectPanel newProjectPanel;
    @Nullable
    public Runnable checkValid;

    public RsProjectGeneratorPeer() {
        this(Paths.get("."));
    }

    public RsProjectGeneratorPeer(@Nonnull Path cargoProjectDir) {
        newProjectPanel = new RsNewProjectPanel(true, cargoProjectDir, () -> {
            if (checkValid != null) {
                checkValid.run();
            }
        });
    }

    @Nonnull
    @Override
    public ConfigurationData getSettings() {
        return newProjectPanel.getData();
    }

    @Nonnull
    public JComponent getComponent(@Nonnull TextFieldWithBrowseButton myLocationField, @Nonnull Runnable checkValid) {
        this.checkValid = checkValid;
        return super.getComponent();
    }

    @Nonnull
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
