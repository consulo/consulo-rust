/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.filters;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * Detects source code locations in rustc output and adds links to them.
 */
public class RsConsoleFilter extends RegexpFileLinkFilter {

    public RsConsoleFilter(@Nonnull Project project, @Nonnull VirtualFile cargoProjectDir) {
        super(project, cargoProjectDir, "(?:\\s+--> )?" + FILE_POSITION_RE + ".*");
    }
}
