/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.security;

import com.intellij.ide.impl.TrustStateListener;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.rust.cargo.project.model.CargoProjectServiceUtil;

@SuppressWarnings("UnstableApiUsage")
public class RsTrustStateListener implements TrustStateListener {

    @Override
    public void onProjectTrusted(@Nonnull Project project) {
        // Since 2023.1, this callback can be called in the background thread which EDT waits for.
        // In combination with fact that we call `invokeAndWaitIfNeeded` inside `refreshAllProjects`,
        // it may lead to deadlock.
        // Let's postpone invocation of `refreshAllProjects` to next event if this code is executed in background thread
        ApplicationManager.getApplication().invokeLater(() -> {
            // Load project model that wasn't load before because project wasn't trusted
            CargoProjectServiceUtil.getCargoProjects(project).refreshAllProjects();
        });
    }
}
