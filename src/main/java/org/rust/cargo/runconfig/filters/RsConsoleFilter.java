/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.filters;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Detects source code locations in rustc output and adds links to them.
 */
public class RsConsoleFilter extends RegexpFileLinkFilter {

    public RsConsoleFilter(@NotNull Project project, @NotNull VirtualFile cargoProjectDir) {
        super(project, cargoProjectDir, "(?:\\s+--> )?" + FILE_POSITION_RE + ".*");
    }
}
