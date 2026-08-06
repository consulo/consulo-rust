/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;

import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.RunConfigurationProducer;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.util.lang.ref.SimpleReference;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

public abstract class CargoRunConfigurationProducer extends RunConfigurationProducer<CargoCommandConfiguration> {

    protected CargoRunConfigurationProducer() {
        super(CargoCommandConfigurationType.getInstance().getFactory());
    }

    @Nonnull
    @Override
    public ConfigurationFactory getConfigurationFactory() {
        return CargoCommandConfigurationType.getInstance().getFactory();
    }

    public abstract boolean setupConfigurationFromContext(
        @Nonnull CargoCommandConfiguration configuration,
        @Nonnull ConfigurationContext context,
        @Nonnull SimpleReference<PsiElement> sourceElement
    );
}
