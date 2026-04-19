/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.filters;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Detects messages about panics and adds source code links to them.
 */
public class RsPanicFilter extends RegexpFileLinkFilter {

    public RsPanicFilter(@NotNull Project project, @NotNull VirtualFile cargoProjectDir) {
        super(project, cargoProjectDir, "\\s*thread '.+' panicked at '.+', " + FILE_POSITION_RE);
    }
}
