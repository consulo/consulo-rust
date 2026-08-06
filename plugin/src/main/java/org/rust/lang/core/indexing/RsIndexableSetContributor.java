/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.indexing;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.stub.IndexableSetContributor;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.macros.MacroExpansionManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RsIndexableSetContributor extends IndexableSetContributor {

    @Nonnull
    @Override
    public Set<VirtualFile> getAdditionalRootsToIndex() {
        return Collections.emptySet();
    }

    @Nonnull
    @Override
    public Set<VirtualFile> getAdditionalProjectRootsToIndex(@Nonnull Project project) {
        HashSet<VirtualFile> additionalProjectRootsToIndex = new HashSet<>();

        MacroExpansionManager manager = project.getInstance(MacroExpansionManager.class);
        if (manager != null) {
            VirtualFile dir = manager.getIndexableDirectory();
            if (dir != null) {
                additionalProjectRootsToIndex.add(dir);
            }
        }

        return additionalProjectRootsToIndex;
    }
}
