/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.indexing;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.stub.IndexableSetContributor;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.macros.MacroExpansionManager;
import org.rust.lang.core.macros.MacroExpansionManagerUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ExtensionImpl
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

        MacroExpansionManager manager = MacroExpansionManagerUtil.getMacroExpansionManagerIfCreated(project);
        if (manager != null) {
            VirtualFile dir = manager.getIndexableDirectory();
            if (dir != null) {
                additionalProjectRootsToIndex.add(dir);
            }
        }

        return additionalProjectRootsToIndex;
    }
}
