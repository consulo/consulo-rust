/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.startup.BackgroundStartupActivity;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;

/**
 * Looks for a Cargo project once the project is open. Discovery reads the module content roots, so it
 * has to wait until the module model is loaded rather than run while the tool window is being built.
 */
@ExtensionImpl
public class RsProjectDiscoveryStartupActivity implements BackgroundStartupActivity {

    @Override
    public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        // Explicit: this is the project-open setup, and a cargo project restored from disk carries only
        // its manifest until the first refresh fills in the packages.
        CargoProjectServiceUtil.guessAndSetupRustProject(project, true);
    }
}
