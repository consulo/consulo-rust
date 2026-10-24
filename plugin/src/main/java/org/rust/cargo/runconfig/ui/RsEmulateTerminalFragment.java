/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;

import consulo.execution.configuration.fragment.TagRunConfigurationFragment;
import consulo.localize.LocalizeValue;
import consulo.rust.localize.RustLocalize;
import org.rust.cargo.runconfig.RsCommandConfiguration;

public class RsEmulateTerminalFragment<T extends RsCommandConfiguration> implements TagRunConfigurationFragment<T> {
    @Override
    public String getId() {
        return "rustEmulateTerminal";
    }

    @Override
    public LocalizeValue getName() {
        return RustLocalize.checkboxEmulateTerminalInOutputConsole();
    }

    @Override
    public boolean isSet(T target) {
        return target.getEmulateTerminal();
    }

    @Override
    public void set(T target, boolean value) {
        target.setEmulateTerminal(value);
    }
}
