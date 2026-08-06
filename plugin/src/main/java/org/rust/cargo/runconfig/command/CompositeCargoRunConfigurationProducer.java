/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;
import consulo.execution.action.Location;

import consulo.execution.action.PsiLocation;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.ConfigurationFromContext;
import consulo.execution.configuration.RunConfiguration;
import consulo.module.Module;
import consulo.util.lang.ref.SimpleReference;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
    public RunnerAndConfigurationSettings findExistingConfiguration(@Nonnull ConfigurationContext context) {
        ConfigurationFromContext preferredConfig = createPreferredConfigurationFromContext(context);
        if (preferredConfig == null) return null;
        RunManager runManager = RunManager.getInstance(context.getProject());
        List<RunnerAndConfigurationSettings> configurations = runManager.getConfigurationSettingsList(getConfigurationType());
        for (RunnerAndConfigurationSettings configurationSettings : configurations) {
            if (isSame(preferredConfig.getConfiguration(), configurationSettings.getConfiguration())) {
                return configurationSettings;
            }
        }
        return null;
    }

    @Nullable
    public ConfigurationFromContext findOrCreateConfigurationFromContext(@Nonnull ConfigurationContext context) {
        ConfigurationFromContext preferredConfig = createPreferredConfigurationFromContext(context);
        if (preferredConfig == null) return null;
        PsiElement psiElement = preferredConfig.getSourceElement();
        consulo.execution.action.Location<?> locationFromContext = context.getLocation();
        if (locationFromContext == null) return null;
        Module module = locationFromContext.getModule();
        @SuppressWarnings("unchecked")
        consulo.execution.action.Location<?> locationFromElement = PsiLocation.fromPsiElement(psiElement, module);
        if (locationFromElement != null) {
            RunnerAndConfigurationSettings settings = findExistingConfiguration(context);
            if (settings != null && isSame(preferredConfig.getConfiguration(), settings.getConfiguration())) {
                preferredConfig.setConfigurationSettings(settings);
            } else {
                // setUniqueNameIfNeeded isn't exposed in Consulo RunManager; skip renaming
            }
        }
        return preferredConfig;
    }

    @Override
    public boolean isConfigurationFromContext(
        @Nonnull CargoCommandConfiguration configuration,
        @Nonnull ConfigurationContext context
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
        @Nonnull CargoCommandConfiguration configuration,
        @Nonnull ConfigurationContext context,
        @Nonnull SimpleReference<PsiElement> sourceElement
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
    public RunConfiguration createLightConfiguration(@Nonnull ConfigurationContext context) {
        CargoRunConfigurationProducer producer = getPreferredProducerForContext(context);
        if (producer == null) return null;
        CargoCommandConfiguration configuration =
            (CargoCommandConfiguration) getConfigurationFactory().createTemplateConfiguration(context.getProject());
        SimpleReference<PsiElement> ref = SimpleReference.create(context.getPsiLocation());
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
    private ConfigurationFromContext createPreferredConfigurationFromContext(@Nonnull ConfigurationContext context) {
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
    private CargoRunConfigurationProducer getPreferredProducerForContext(@Nonnull ConfigurationContext context) {
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

    private static boolean isSame(@Nonnull RunConfiguration self, @Nullable RunConfiguration other) {
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
