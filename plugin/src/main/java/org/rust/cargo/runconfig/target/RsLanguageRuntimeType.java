/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.target;

import consulo.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.target.LanguageRuntimeType;
import com.intellij.execution.target.TargetEnvironmentConfiguration;
import com.intellij.execution.target.TargetEnvironmentType;
import consulo.component.persist.PersistentStateComponent;
import consulo.configurable.Configurable;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.runconfig.RsCommandConfiguration;
import org.rust.icons.RsIcons;

import javax.swing.*;
import java.util.function.Supplier;
import consulo.ui.image.Image;

public class RsLanguageRuntimeType extends LanguageRuntimeType<RsLanguageRuntimeConfiguration> {

    public static final String TYPE_ID = "RsLanguageRuntime";

    public RsLanguageRuntimeType() {
        super(TYPE_ID);
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return "Rust";
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return RsIcons.RUST;
    }

    @Nonnull
    @Override
    public String getConfigurableDescription() {
        return "Rust Configuration";
    }

    @Nonnull
    @Override
    public String getLaunchDescription() {
        return "Run Rust Command";
    }

    @Nonnull
    @Override
    public PersistentStateComponent<?> createSerializer(@Nonnull RsLanguageRuntimeConfiguration config) {
        return config;
    }

    @Nonnull
    @Override
    public RsLanguageRuntimeConfiguration createDefaultConfig() {
        return new RsLanguageRuntimeConfiguration();
    }

    @Nonnull
    @Override
    public RsLanguageRuntimeConfiguration duplicateConfig(@Nonnull RsLanguageRuntimeConfiguration config) {
        return duplicatePersistentComponent(this, config);
    }

    @Nullable
    @Override
    public Introspector<RsLanguageRuntimeConfiguration> createIntrospector(@Nonnull RsLanguageRuntimeConfiguration config) {
        if (!config.getRustcPath().isBlank() && !config.getRustcVersion().isBlank() &&
            !config.getCargoPath().isBlank() && !config.getCargoVersion().isBlank()) {
            return null;
        }
        return new RsLanguageRuntimeIntrospector(config);
    }

    @Nonnull
    @Override
    public Configurable createConfigurable(
        @Nonnull Project project,
        @Nonnull RsLanguageRuntimeConfiguration config,
        @Nonnull TargetEnvironmentType<?> targetEnvironmentType,
        @Nonnull Supplier<TargetEnvironmentConfiguration> targetSupplier
    ) {
        return new RsLanguageRuntimeConfigurable(config);
    }

    @Nullable
    public RsLanguageRuntimeConfiguration findLanguageRuntime(@Nonnull TargetEnvironmentConfiguration target) {
        return target.getRuntimes().findByType(RsLanguageRuntimeConfiguration.class);
    }

    public boolean isApplicableTo(@Nonnull RunnerAndConfigurationSettings runConfig) {
        return runConfig.getConfiguration() instanceof RsCommandConfiguration;
    }
}
