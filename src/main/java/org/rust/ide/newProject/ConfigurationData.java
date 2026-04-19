/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject;

import org.jetbrains.annotations.NotNull;
import org.rust.cargo.project.settings.ui.RustProjectSettingsPanel;

import java.util.Objects;

public final class ConfigurationData {

    @NotNull
    private final RustProjectSettingsPanel.Data settings;
    @NotNull
    private final RsProjectTemplate template;

    public ConfigurationData(@NotNull RustProjectSettingsPanel.Data settings, @NotNull RsProjectTemplate template) {
        this.settings = settings;
        this.template = template;
    }

    @NotNull
    public RustProjectSettingsPanel.Data getSettings() {
        return settings;
    }

    @NotNull
    public RsProjectTemplate getTemplate() {
        return template;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigurationData that = (ConfigurationData) o;
        return Objects.equals(settings, that.settings) && Objects.equals(template, that.template);
    }

    @Override
    public int hashCode() {
        return Objects.hash(settings, template);
    }

    @Override
    public String toString() {
        return "ConfigurationData(" +
            "settings=" + settings +
            ", template=" + template +
            ')';
    }
}
