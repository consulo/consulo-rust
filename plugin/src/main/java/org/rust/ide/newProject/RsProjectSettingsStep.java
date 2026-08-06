/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject;

import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.platform.ProjectSettingsStepBase;
import com.intellij.platform.DirectoryProjectGenerator;
import jakarta.annotation.Nonnull;

public class RsProjectSettingsStep extends ProjectSettingsStepBase<ConfigurationData> {

    public RsProjectSettingsStep(@Nonnull DirectoryProjectGenerator<ConfigurationData> generator) {
        super(generator, new AbstractNewProjectStep.AbstractCallback<>());
    }
}
