/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.indexing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.IndexableSetContributor;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.macros.MacroExpansionManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RsIndexableSetContributor extends IndexableSetContributor {

    @NotNull
    @Override
    public Set<VirtualFile> getAdditionalRootsToIndex() {
        return Collections.emptySet();
    }

    @NotNull
    @Override
    public Set<VirtualFile> getAdditionalProjectRootsToIndex(@NotNull Project project) {
        HashSet<VirtualFile> additionalProjectRootsToIndex = new HashSet<>();

        MacroExpansionManager manager = project.getServiceIfCreated(MacroExpansionManager.class);
        if (manager != null) {
            VirtualFile dir = manager.getIndexableDirectory();
            if (dir != null) {
                additionalProjectRootsToIndex.add(dir);
            }
        }

        return additionalProjectRootsToIndex;
    }
}
