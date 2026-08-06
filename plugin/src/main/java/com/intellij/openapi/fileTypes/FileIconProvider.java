/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.intellij.openapi.fileTypes;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import javax.swing.Icon;

/** IntelliJ-compat stub. */
public interface FileIconProvider {
    Icon getIcon(VirtualFile file, int flags, Project project);
}
