/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;

import consulo.execution.configuration.fragment.TagRunConfigurationFragment;
import consulo.localize.LocalizeValue;
import consulo.rust.localize.RustLocalize;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

public class CargoAllFeaturesFragment implements TagRunConfigurationFragment<CargoCommandConfiguration> {
    @Override
    public String getId() {
        return "cargoAllFeatures";
    }

    @Override
    public LocalizeValue getName() {
        return RustLocalize.checkboxUseAllFeaturesInTests();
    }

    @Override
    public boolean isSet(CargoCommandConfiguration target) {
        return target.getAllFeatures();
    }

    @Override
    public void set(CargoCommandConfiguration target, boolean value) {
        target.setAllFeatures(value);
    }
}
