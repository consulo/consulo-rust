/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move;

import consulo.project.Project;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JPanel;
import javax.swing.JTree;
import java.awt.BorderLayout;
import java.util.Collections;
import java.util.List;
import consulo.ui.image.Image;

/**
 * Member-selection UI for the Move refactoring. The tree is a placeholder: it shows no nodes
 * and has no checkbox selection; the initially-selected nodes are returned unchanged.
 */
public class RsMoveMemberSelection {

    private RsMoveMemberSelection() {
    }

    public interface RsMoveNodeInfo {
        void render(@Nonnull ColoredTreeCellRenderer renderer);

        @Nullable
        default consulo.ui.image.Image getIcon() {
            return null;
        }

        @Nonnull
        default List<RsMoveNodeInfo> getChildren() {
            return Collections.emptyList();
        }
    }

    public static class RsMoveMemberSelectionPanel extends JPanel {

        @Nonnull
        private final RsMoveMemberSelectionTree tree;

        public RsMoveMemberSelectionPanel(
            @Nonnull Project project,
            @Nonnull String title,
            @Nonnull List<RsMoveNodeInfo> nodesAll,
            @Nonnull List<RsMoveNodeInfo> nodesSelected
        ) {
            this.tree = new RsMoveMemberSelectionTree(project, nodesAll, nodesSelected);
            setLayout(new BorderLayout());
        }

        @Nonnull
        public RsMoveMemberSelectionTree getTree() {
            return tree;
        }
    }

    /** Stub tree with no selection semantics. */
    public static class RsMoveMemberSelectionTree extends JTree {

        @Nonnull
        private final List<RsMoveNodeInfo> includedChanges;

        public RsMoveMemberSelectionTree(
            @Nonnull Project project,
            @Nonnull List<RsMoveNodeInfo> nodesAll,
            @Nonnull List<RsMoveNodeInfo> nodesSelected
        ) {
            this.includedChanges = nodesSelected;
        }

        @Nonnull
        public List<RsMoveNodeInfo> getIncludedChanges() {
            return includedChanges;
        }

        @Nonnull
        public java.util.Set<Object> getIncludedSet() {
            return new java.util.LinkedHashSet<>(includedChanges);
        }

        public void setInclusionListener(@Nullable Runnable listener) {
            // no-op stub
        }
    }
}
