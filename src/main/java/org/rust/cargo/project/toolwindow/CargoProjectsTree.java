/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.toolwindow;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.stdext.Utils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CargoProjectsTree extends SimpleTree {

    private static final Logger LOG = Logger.getInstance(CargoProjectsTree.class);

    @Nullable
    public CargoProject getSelectedProject() {
        javax.swing.tree.TreePath path = getSelectionPath();
        if (path == null || path.getPathCount() < 2) return null;
        Object treeNode = path.getPathComponent(1);
        if (!(treeNode instanceof DefaultMutableTreeNode mutableNode)) return null;
        if (mutableNode.getUserObject() instanceof CargoProjectTreeStructure.Project projectNode) {
            return projectNode.cargoProject;
        }
        return null;
    }

    public CargoProjectsTree() {
        setRootVisible(false);
        setShowsRootHandles(true);
        getEmptyText().setText("There are no Cargo projects to display.");
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() < 2 || !SwingUtilities.isLeftMouseButton(e)) return;
                if (!(e.getSource() instanceof CargoProjectsTree tree)) return;
                javax.swing.tree.TreePath node = tree.getSelectionModel().getSelectionPath();
                if (node == null) return;
                Object lastComponent = node.getLastPathComponent();
                if (!(lastComponent instanceof DefaultMutableTreeNode mutableNode)) return;
                if (!(mutableNode.getUserObject() instanceof CargoProjectTreeStructure.Target targetNode)) return;
                CargoWorkspace.Target target = targetNode.target;
                String command = launchCommand(target);
                if (command == null) {
                    LOG.warn("Can't create launch command for `" + target.getName() + "` target");
                    return;
                }
                CargoProject cargoProject = getSelectedProject();
                if (cargoProject == null) return;
                String configurationName = Utils.capitalized(command) + " " + target.getName();
                run(CargoCommandLine.forTarget(target, command, java.util.Collections.emptyList()), cargoProject, configurationName);
            }
        });
    }

    protected void run(@NotNull CargoCommandLine commandLine, @NotNull CargoProject project, @NotNull String name) {
        commandLine.run(project, name);
    }

    @Nullable
    private static String launchCommand(@NotNull CargoWorkspace.Target target) {
        CargoWorkspace.TargetKind kind = target.getKind();
        if (kind instanceof CargoWorkspace.TargetKind.Bin) return "run";
        if (kind instanceof CargoWorkspace.TargetKind.Lib) return "build";
        if (kind instanceof CargoWorkspace.TargetKind.Test) return "test";
        if (kind instanceof CargoWorkspace.TargetKind.Bench) return "bench";
        if (kind instanceof CargoWorkspace.TargetKind.ExampleBin) return "run";
        if (kind instanceof CargoWorkspace.TargetKind.ExampleLib) return "build";
        return null;
    }
}
