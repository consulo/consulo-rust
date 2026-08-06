/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import consulo.project.Project;
import jakarta.annotation.Nonnull;

/**
 * Bridge class delegating to {@link RunConfigUtil}.
 */
public final class HasCargoProjectUtil {
    private HasCargoProjectUtil() {
    }

    public static boolean getHasCargoProject(@Nonnull Project project) {
        return RunConfigUtil.hasCargoProject(project);
    }
}
