/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.filters;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Detects source code locations in dbg! macro output and adds links to them
 */
public class RsDbgFilter extends RegexpFileLinkFilter {

    public RsDbgFilter(@NotNull Project project, @NotNull VirtualFile cargoProjectDir) {
        super(project, cargoProjectDir, "\\s*\\[" + FILE_POSITION_RE + "].*");
    }
}
