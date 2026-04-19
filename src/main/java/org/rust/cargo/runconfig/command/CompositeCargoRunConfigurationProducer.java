/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;

import com.intellij.execution.PsiLocation;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.runconfig.test.CargoBenchRunConfigurationProducer;
import org.rust.cargo.runconfig.test.CargoTestRunConfigurationProducer;

import java.util.*;
import java.util.function.Function;

/**
 * This class aggregates other Rust run configuration {@link #myProducers} and manages the search & creation of run
 * configurations, taking into account configurations that other {@link #myProducers} can create.
 * The problem with the previous approach is that if there is an existing configuration that matches the context, the
 * platform does not compare this configuration with those that can be created by other producers, even if these
 * configurations are better matched with the context (see <a href="https://github.com/intellij-rust/intellij-rust/issues/1252">#1252</a>).
 */
public class CompositeCargoRunConfigurationProducer extends CargoRunConfigurationProducer {

    private final List<CargoRunConfigurationProducer> myProducers = Arrays.asList(
        new CargoExecutableRunConfigurationProducer(),
        new CargoTestRunConfigurationProducer(),
        new CargoBenchRunConfigurationProducer()
    );

    @Nullable
    @Override
    public RunnerAndConfigurationSettings findExistingConfiguration(@NotNull ConfigurationContext context) {
        ConfigurationFromContext preferredConfig = createPreferredConfigurationFromContext(context);
        if (preferredConfig == null) return null;
        RunManager runManager = RunManager.getInstance(context.getProject());
        List<RunnerAndConfigurationSettings> configurations = getConfigurationSettingsList(runManager);
        for (RunnerAndConfigurationSettings configurationSettings : configurations) {
            if (isSame(preferredConfig.getConfiguration(), configurationSettings.getConfiguration())) {
                return configurationSettings;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public ConfigurationFromContext findOrCreateConfigurationFromContext(@NotNull ConfigurationContext context) {
        ConfigurationFromContext preferredConfig = createPreferredConfigurationFromContext(context);
        if (preferredConfig == null) return null;
        PsiElement psiElement = preferredConfig.getSourceElement();
        com.intellij.execution.Location<?> locationFromContext = context.getLocation();
        if (locationFromContext == null) return null;
        Module module = locationFromContext.getModule();
        @SuppressWarnings("unchecked")
        com.intellij.execution.Location<?> locationFromElement = PsiLocation.fromPsiElement(psiElement, module);
        if (locationFromElement != null) {
            RunnerAndConfigurationSettings settings = findExistingConfiguration(context);
            if (settings != null && isSame(preferredConfig.getConfiguration(), settings.getConfiguration())) {
                preferredConfig.setConfigurationSettings(settings);
            } else {
                RunManager.getInstance(context.getProject()).setUniqueNameIfNeeded(preferredConfig.getConfiguration());
            }
        }
        return preferredConfig;
    }

    @Override
    public boolean isConfigurationFromContext(
        @NotNull CargoCommandConfiguration configuration,
        @NotNull ConfigurationContext context
    ) {
        for (CargoRunConfigurationProducer producer : myProducers) {
            if (producer.isConfigurationFromContext(configuration, context)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setupConfigurationFromContext(
        @NotNull CargoCommandConfiguration configuration,
        @NotNull ConfigurationContext context,
        @NotNull Ref<PsiElement> sourceElement
    ) {
        for (CargoRunConfigurationProducer producer : myProducers) {
            if (producer.setupConfigurationFromContext(configuration, context, sourceElement)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public RunConfiguration createLightConfiguration(@NotNull ConfigurationContext context) {
        CargoRunConfigurationProducer producer = getPreferredProducerForContext(context);
        if (producer == null) return null;
        CargoCommandConfiguration configuration =
            (CargoCommandConfiguration) getConfigurationFactory().createTemplateConfiguration(context.getProject());
        Ref<PsiElement> ref = new Ref<>(context.getPsiLocation());
        try {
            if (!producer.setupConfigurationFromContext(configuration, context, ref)) {
                return null;
            }
        } catch (ClassCastException e) {
            return null;
        }
        return configuration;
    }

    @Nullable
    private ConfigurationFromContext createPreferredConfigurationFromContext(@NotNull ConfigurationContext context) {
        List<ConfigurationFromContext> configs = new ArrayList<>();
        for (CargoRunConfigurationProducer producer : myProducers) {
            ConfigurationFromContext config = producer.createConfigurationFromContext(context);
            if (config != null) {
                configs.add(config);
            }
        }
        configs.sort(ConfigurationFromContext.COMPARATOR);
        return configs.isEmpty() ? null : configs.get(0);
    }

    @Nullable
    private CargoRunConfigurationProducer getPreferredProducerForContext(@NotNull ConfigurationContext context) {
        List<Map.Entry<ConfigurationFromContext, CargoRunConfigurationProducer>> pairs = new ArrayList<>();
        for (CargoRunConfigurationProducer producer : myProducers) {
            ConfigurationFromContext config = producer.createConfigurationFromContext(context);
            if (config != null) {
                pairs.add(new AbstractMap.SimpleEntry<>(config, producer));
            }
        }
        pairs.sort(Comparator.comparing(
            (Function<Map.Entry<ConfigurationFromContext, CargoRunConfigurationProducer>, ConfigurationFromContext>) Map.Entry::getKey,
            ConfigurationFromContext.COMPARATOR
        ));
        return pairs.isEmpty() ? null : pairs.get(0).getValue();
    }

    private static boolean isSame(@NotNull RunConfiguration self, @Nullable RunConfiguration other) {
        if (self == other) return true;
        if (!(self instanceof CargoCommandConfiguration) || !(other instanceof CargoCommandConfiguration)) {
            return self.equals(other);
        }
        CargoCommandConfiguration a = (CargoCommandConfiguration) self;
        CargoCommandConfiguration b = (CargoCommandConfiguration) other;
        if (!Objects.equals(a.getChannel(), b.getChannel())) return false;
        if (!Objects.equals(a.getCommand(), b.getCommand())) return false;
        if (!Objects.equals(a.getBacktrace(), b.getBacktrace())) return false;
        if (!Objects.equals(a.getWorkingDirectory(), b.getWorkingDirectory())) return false;
        return true;
    }
}
