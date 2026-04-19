/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.WritingAccessProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Protect macro expansions from being edited (e.g. by rename)
 */
public class RsMacroExpansionWritingAccessProvider extends WritingAccessProvider {
    private final Project myProject;

    public RsMacroExpansionWritingAccessProvider(Project project) {
        myProject = project;
    }

    /**
     * @return set of files that cannot be accessed
     */
    @NotNull
    @Override
    public Collection<VirtualFile> requestWriting(@NotNull Collection<? extends VirtualFile> files) {
        return files.stream()
            .filter(MacroExpansionManager::isExpansionFile)
            .collect(Collectors.toList());
    }

    @Override
    public boolean isPotentiallyWritable(@NotNull VirtualFile file) {
        return !MacroExpansionManager.isExpansionFile(file);
    }
}
