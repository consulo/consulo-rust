/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.target;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.target.LanguageRuntimeType;
import com.intellij.execution.target.TargetEnvironmentConfiguration;
import com.intellij.execution.target.TargetEnvironmentType;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.runconfig.RsCommandConfiguration;
import org.rust.ide.icons.RsIcons;

import javax.swing.*;
import java.util.function.Supplier;

public class RsLanguageRuntimeType extends LanguageRuntimeType<RsLanguageRuntimeConfiguration> {

    public static final String TYPE_ID = "RsLanguageRuntime";

    public RsLanguageRuntimeType() {
        super(TYPE_ID);
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Rust";
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return RsIcons.RUST;
    }

    @NotNull
    @Override
    public String getConfigurableDescription() {
        return "Rust Configuration";
    }

    @NotNull
    @Override
    public String getLaunchDescription() {
        return "Run Rust Command";
    }

    @NotNull
    @Override
    public PersistentStateComponent<?> createSerializer(@NotNull RsLanguageRuntimeConfiguration config) {
        return config;
    }

    @NotNull
    @Override
    public RsLanguageRuntimeConfiguration createDefaultConfig() {
        return new RsLanguageRuntimeConfiguration();
    }

    @NotNull
    @Override
    public RsLanguageRuntimeConfiguration duplicateConfig(@NotNull RsLanguageRuntimeConfiguration config) {
        return duplicatePersistentComponent(this, config);
    }

    @Nullable
    @Override
    public Introspector<RsLanguageRuntimeConfiguration> createIntrospector(@NotNull RsLanguageRuntimeConfiguration config) {
        if (!config.getRustcPath().isBlank() && !config.getRustcVersion().isBlank() &&
            !config.getCargoPath().isBlank() && !config.getCargoVersion().isBlank()) {
            return null;
        }
        return new RsLanguageRuntimeIntrospector(config);
    }

    @NotNull
    @Override
    public Configurable createConfigurable(
        @NotNull Project project,
        @NotNull RsLanguageRuntimeConfiguration config,
        @NotNull TargetEnvironmentType<?> targetEnvironmentType,
        @NotNull Supplier<TargetEnvironmentConfiguration> targetSupplier
    ) {
        return new RsLanguageRuntimeConfigurable(config);
    }

    @Nullable
    @Override
    public RsLanguageRuntimeConfiguration findLanguageRuntime(@NotNull TargetEnvironmentConfiguration target) {
        return target.getRuntimes().findByType(RsLanguageRuntimeConfiguration.class);
    }

    @Override
    public boolean isApplicableTo(@NotNull RunnerAndConfigurationSettings runConfig) {
        return runConfig.getConfiguration() instanceof RsCommandConfiguration;
    }
}
