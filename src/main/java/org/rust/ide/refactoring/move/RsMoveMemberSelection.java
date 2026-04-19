/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vcs.changes.ui.*;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SeparatorFactory;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class RsMoveMemberSelection {

    private RsMoveMemberSelection() {
    }

    public interface RsMoveNodeInfo {
        void render(@NotNull ColoredTreeCellRenderer renderer);

        @Nullable
        default Icon getIcon() {
            return null;
        }

        @NotNull
        default List<RsMoveNodeInfo> getChildren() {
            return Collections.emptyList();
        }
    }

    public static class RsMoveMemberSelectionPanel extends JPanel {

        @NotNull
        private final RsMoveMemberSelectionTree tree;

        public RsMoveMemberSelectionPanel(
            @NotNull Project project,
            @NlsContexts.Separator @NotNull String title,
            @NotNull List<RsMoveNodeInfo> nodesAll,
            @NotNull List<RsMoveNodeInfo> nodesSelected
        ) {
            this.tree = new RsMoveMemberSelectionTree(project, nodesAll, nodesSelected);
            setLayout(new BorderLayout());
            JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(tree);
            add(SeparatorFactory.createSeparator(title, tree), BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
        }

        @NotNull
        public RsMoveMemberSelectionTree getTree() {
            return tree;
        }
    }

    public static class RsMoveMemberSelectionTree extends ChangesTreeImpl<RsMoveNodeInfo> {

        public RsMoveMemberSelectionTree(
            @NotNull Project project,
            @NotNull List<RsMoveNodeInfo> nodesAll,
            @NotNull List<RsMoveNodeInfo> nodesSelected
        ) {
            super(project, true, false, RsMoveNodeInfo.class);
            setIncludedChanges(nodesSelected);
            setChangesToDisplay(nodesAll);
            TreeUtil.collapseAll(this, 0);
        }

        @Override
        protected @NotNull DefaultTreeModel buildTreeModel(@NotNull List<? extends RsMoveNodeInfo> nodeInfos) {
            return new RsMoveMemberSelectionModelBuilder(getProject(), getGrouping()).buildTreeModel(nodeInfos);
        }
    }

    private static class RsMoveMemberSelectionModelBuilder extends TreeModelBuilder {

        RsMoveMemberSelectionModelBuilder(@NotNull Project project, @NotNull ChangesGroupingPolicyFactory grouping) {
            super(project, grouping);
        }

        @NotNull
        DefaultTreeModel buildTreeModel(@NotNull List<? extends RsMoveNodeInfo> nodeInfos) {
            for (RsMoveNodeInfo nodeInfo : nodeInfos) {
                addNode(nodeInfo, myRoot);
            }
            return build();
        }

        private void addNode(@NotNull RsMoveNodeInfo nodeInfo, @NotNull ChangesBrowserNode<?> root) {
            RsMoveMemberSelectionNode node = new RsMoveMemberSelectionNode(nodeInfo);
            myModel.insertNodeInto(node, root, root.getChildCount());

            List<RsMoveNodeInfo> children = nodeInfo.getChildren();
            if (!children.isEmpty()) {
                node.markAsHelperNode();
                for (RsMoveNodeInfo child : children) {
                    addNode(child, node);
                }
            }
        }
    }

    private static class RsMoveMemberSelectionNode extends ChangesBrowserNode<RsMoveNodeInfo> {
        @NotNull
        private final RsMoveNodeInfo info;

        RsMoveMemberSelectionNode(@NotNull RsMoveNodeInfo info) {
            super(info);
            this.info = info;
        }

        @Override
        public void render(
            @NotNull ChangesBrowserNodeRenderer renderer,
            boolean selected,
            boolean expanded,
            boolean hasFocus
        ) {
            info.render(renderer);
            renderer.setIcon(info.getIcon());
        }
    }
}
