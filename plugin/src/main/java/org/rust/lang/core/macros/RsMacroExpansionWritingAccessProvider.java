/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.WritingAccessProvider;
import jakarta.annotation.Nonnull;

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
    @Nonnull
    @Override
    public Collection<VirtualFile> requestWriting(VirtualFile... files) {
        return java.util.Arrays.stream(files)
            .filter(MacroExpansionManager::isExpansionFile)
            .collect(Collectors.toList());
    }

    @Override
    public boolean isPotentiallyWritable(@Nonnull VirtualFile file) {
        return !MacroExpansionManager.isExpansionFile(file);
    }
}
