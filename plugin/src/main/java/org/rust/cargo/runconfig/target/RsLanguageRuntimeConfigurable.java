/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.target;

import com.intellij.openapi.options.BoundConfigurable;
import com.intellij.openapi.ui.DialogPanel;
import consulo.ui.ex.awt.JBTextField;
import consulo.ui.ex.awt.FormBuilder;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;

import java.awt.BorderLayout;
import java.util.Objects;

public class RsLanguageRuntimeConfigurable extends BoundConfigurable {

    private final RsLanguageRuntimeConfiguration myConfig;

    private JBTextField rustcPath;
    private JBTextField rustcVersion;
    private JBTextField cargoPath;
    private JBTextField cargoVersion;
    private JBTextField localBuildArgs;

    public RsLanguageRuntimeConfigurable(@Nonnull RsLanguageRuntimeConfiguration config) {
        super("Rust", null);
        myConfig = config;
    }

    @Nonnull
    public RsLanguageRuntimeConfiguration getConfig() {
        return myConfig;
    }

    /**
     * exposes commit/reset hooks via {@link #apply()}/{@link #reset()}/{@link #isModified()}.
     */
    @Nonnull
    @Override
    public DialogPanel createPanel() {
        rustcPath = new JBTextField();
        rustcVersion = new JBTextField();
        rustcVersion.setEditable(false);
        cargoPath = new JBTextField();
        cargoVersion = new JBTextField();
        cargoVersion.setEditable(false);
        localBuildArgs = new JBTextField();
        localBuildArgs.getEmptyText().setText("e.g. --target=x86_64-unknown-linux-gnu");

        FormBuilder builder = FormBuilder.createFormBuilder()
            .addLabeledComponent(RsBundle.message("run.target.rustc.executable.path.label"), rustcPath)
            .addLabeledComponent(RsBundle.message("run.target.rustc.executable.version.label"), rustcVersion)
            .addLabeledComponent(RsBundle.message("run.target.cargo.executable.path.label"), cargoPath)
            .addLabeledComponent(RsBundle.message("run.target.cargo.executable.version.label"), cargoVersion)
            .addLabeledComponent(RsBundle.message("run.target.build.arguments.label"), localBuildArgs)
            .addComponentToRightColumn(new javax.swing.JLabel(RsBundle.message("run.target.build.arguments.comment")));

        DialogPanel panel = new DialogPanel(new BorderLayout());
        panel.add(builder.getPanel(), BorderLayout.CENTER);
        reset();
        return panel;
    }

    @Override
    public void apply() {
        if (rustcPath != null) myConfig.setRustcPath(safeText(rustcPath));
        if (rustcVersion != null) myConfig.setRustcVersion(safeText(rustcVersion));
        if (cargoPath != null) myConfig.setCargoPath(safeText(cargoPath));
        if (cargoVersion != null) myConfig.setCargoVersion(safeText(cargoVersion));
        if (localBuildArgs != null) myConfig.setLocalBuildArgs(safeText(localBuildArgs));
    }

    @Override
    public void reset() {
        if (rustcPath != null) rustcPath.setText(myConfig.getRustcPath());
        if (rustcVersion != null) rustcVersion.setText(myConfig.getRustcVersion());
        if (cargoPath != null) cargoPath.setText(myConfig.getCargoPath());
        if (cargoVersion != null) cargoVersion.setText(myConfig.getCargoVersion());
        if (localBuildArgs != null) localBuildArgs.setText(myConfig.getLocalBuildArgs());
    }

    @Override
    public boolean isModified() {
        if (rustcPath == null) return false;
        return !Objects.equals(safeText(rustcPath), myConfig.getRustcPath())
            || !Objects.equals(safeText(cargoPath), myConfig.getCargoPath())
            || !Objects.equals(safeText(localBuildArgs), myConfig.getLocalBuildArgs());
    }

    @Nonnull
    private static String safeText(@Nonnull JBTextField field) {
        String text = field.getText();
        return text != null ? text : "";
    }
}
