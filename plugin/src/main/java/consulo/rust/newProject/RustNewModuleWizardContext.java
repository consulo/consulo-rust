/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.newProject;

import consulo.content.bundle.Sdk;
import consulo.module.creation.NewModuleWizardContextBase;
import jakarta.annotation.Nullable;

/**
 * Carries the toolchain bundle picked in the new module wizard.
 */
public class RustNewModuleWizardContext extends NewModuleWizardContextBase {
    @Nullable
    private Sdk myToolchainBundle;

    public RustNewModuleWizardContext(boolean isNewProject) {
        super(isNewProject);
    }

    @Nullable
    public Sdk getToolchainBundle() {
        return myToolchainBundle;
    }

    public void setToolchainBundle(@Nullable Sdk toolchainBundle) {
        myToolchainBundle = toolchainBundle;
    }
}
