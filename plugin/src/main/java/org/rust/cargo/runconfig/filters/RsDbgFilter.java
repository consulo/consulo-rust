/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.filters;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * Detects source code locations in dbg! macro output and adds links to them
 */
public class RsDbgFilter extends RegexpFileLinkFilter {

    public RsDbgFilter(@Nonnull Project project, @Nonnull VirtualFile cargoProjectDir) {
        super(project, cargoProjectDir, "\\s*\\[" + FILE_POSITION_RE + "].*");
    }
}
