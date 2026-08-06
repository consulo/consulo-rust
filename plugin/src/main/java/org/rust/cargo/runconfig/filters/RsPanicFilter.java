/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.filters;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * Detects messages about panics and adds source code links to them.
 */
public class RsPanicFilter extends RegexpFileLinkFilter {

    public RsPanicFilter(@Nonnull Project project, @Nonnull VirtualFile cargoProjectDir) {
        super(project, cargoProjectDir, "\\s*thread '.+' panicked at '.+', " + FILE_POSITION_RE);
    }
}
