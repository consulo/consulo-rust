/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;

import consulo.execution.configuration.fragment.TagRunConfigurationFragment;
import consulo.localize.LocalizeValue;
import consulo.rust.localize.RustLocalize;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

public class CargoRequiredFeaturesFragment implements TagRunConfigurationFragment<CargoCommandConfiguration> {
    @Override
    public String getId() {
        return "cargoRequiredFeatures";
    }

    @Override
    public LocalizeValue getName() {
        return RustLocalize.checkboxImplicitlyAddRequiredFeaturesIfPossible();
    }

    @Override
    public boolean isSet(CargoCommandConfiguration target) {
        return target.getRequiredFeatures();
    }

    @Override
    public void set(CargoCommandConfiguration target, boolean value) {
        target.setRequiredFeatures(value);
    }
}
