/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.projectView;

import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.RsConstants;
import org.rust.lang.core.psi.RsFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Moves {@code mod.rs} files and crate roots on top
 */
public class RsTreeStructureProvider implements TreeStructureProvider, DumbAware {

    @NotNull
    @Override
    public Collection<AbstractTreeNode<?>> modify(
        @NotNull AbstractTreeNode<?> parent,
        @NotNull Collection<AbstractTreeNode<?>> children,
        @Nullable ViewSettings settings
    ) {
        List<AbstractTreeNode<?>> result = new ArrayList<>(children.size());
        for (AbstractTreeNode<?> child : children) {
            if (child instanceof PsiFileNode psiFileNode && psiFileNode.getValue() instanceof RsFile) {
                result.add(new RsPsiFileNode(psiFileNode, settings));
            } else {
                result.add(child);
            }
        }
        return result;
    }

    private static class RsPsiFileNode extends PsiFileNode {
        RsPsiFileNode(@NotNull PsiFileNode original, @Nullable ViewSettings viewSettings) {
            super(original.getProject(), original.getValue(), viewSettings);
        }

        @Override
        public Comparable<?> getSortKey() {
            PsiFile value = getValue();
            if (value != null && RsConstants.MOD_RS_FILE.equals(value.getName())) {
                return -2;
            }
            if (value instanceof RsFile rsFile && rsFile.isCrateRoot()) {
                return -1;
            }
            return 0;
        }
    }
}
