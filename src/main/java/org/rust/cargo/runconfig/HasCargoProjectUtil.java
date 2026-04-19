/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Bridge class delegating to {@link RunConfigUtil}.
 */
public final class HasCargoProjectUtil {
    private HasCargoProjectUtil() {
    }

    public static boolean getHasCargoProject(@NotNull Project project) {
        return RunConfigUtil.hasCargoProject(project);
    }
}
