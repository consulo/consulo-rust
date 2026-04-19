/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public abstract class CargoRunConfigurationProducer extends LazyRunConfigurationProducer<CargoCommandConfiguration> {

    @NotNull
    @Override
    public ConfigurationFactory getConfigurationFactory() {
        return CargoCommandConfigurationType.getInstance().getFactory();
    }

    @Override
    public abstract boolean setupConfigurationFromContext(
        @NotNull CargoCommandConfiguration configuration,
        @NotNull ConfigurationContext context,
        @NotNull Ref<PsiElement> sourceElement
    );
}
