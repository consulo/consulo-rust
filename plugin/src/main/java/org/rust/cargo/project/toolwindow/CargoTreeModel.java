/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.toolwindow;


import consulo.logging.Logger;
import consulo.rust.icon.RustIconGroup;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.stdext.Utils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Supplies the Cargo tree: projects, their workspace members, and the targets of each.
 * Double-clicking a target runs the command that suits its kind.
 */
public class CargoTreeModel implements TreeModel<CargoTreeNode> {

    private static final Logger LOG = Logger.getInstance(CargoTreeModel.class);

    private volatile List<CargoProject> myCargoProjects = List.of();

    public void setCargoProjects(@Nonnull Collection<CargoProject> cargoProjects) {
        myCargoProjects = List.copyOf(cargoProjects);
    }

    @Override
    public void buildChildren(@Nonnull Function<CargoTreeNode, TreeNode<CargoTreeNode>> nodeFactory,
                              @Nullable CargoTreeNode parentValue) {
        if (parentValue == null) {
            myCargoProjects.stream()
                .sorted(Comparator.comparing(CargoProject::getPresentableName))
                .forEach(cargoProject -> node(nodeFactory, new CargoTreeNode.ProjectNode(cargoProject)));
            return;
        }

        switch (parentValue) {
            case CargoTreeNode.ProjectNode projectNode -> buildProjectChildren(nodeFactory, projectNode);
            case CargoTreeNode.MemberNode memberNode -> node(nodeFactory,
                new CargoTreeNode.TargetsNode(memberNode.cargoProject(), memberNode.pkg().getTargets()));
            case CargoTreeNode.TargetsNode targetsNode -> targetsNode.targets().stream()
                .sorted(Comparator.comparing(CargoWorkspace.Target::getName))
                .forEach(target -> node(nodeFactory, new CargoTreeNode.TargetNode(targetsNode.cargoProject(), target)));
            case CargoTreeNode.TargetNode ignored -> {
            }
        }
    }

    /**
     * The package rooted at the project's working directory contributes its targets directly, so the
     * common single-crate case does not nest them one level deeper than necessary.
     */
    private void buildProjectChildren(@Nonnull Function<CargoTreeNode, TreeNode<CargoTreeNode>> nodeFactory,
                                      @Nonnull CargoTreeNode.ProjectNode projectNode) {
        CargoProject cargoProject = projectNode.cargoProject();
        CargoWorkspace workspace = cargoProject.getWorkspace();
        if (workspace == null) {
            return;
        }

        Path workingDirectory = org.rust.cargo.project.model.CargoProjectLocator.getWorkingDirectory(cargoProject);
        List<CargoWorkspace.Package> members = new ArrayList<>();
        for (CargoWorkspace.Package pkg : workspace.getPackages()) {
            if (pkg.getOrigin() == PackageOrigin.WORKSPACE) {
                members.add(pkg);
            }
        }
        members.sort(Comparator.comparing(CargoWorkspace.Package::getName));

        List<CargoWorkspace.Package> others = new ArrayList<>();
        for (CargoWorkspace.Package pkg : members) {
            if (pkg.getRootDirectory().equals(workingDirectory)) {
                node(nodeFactory, new CargoTreeNode.TargetsNode(cargoProject, pkg.getTargets()));
            }
            else {
                others.add(pkg);
            }
        }
        for (CargoWorkspace.Package pkg : others) {
            node(nodeFactory, new CargoTreeNode.MemberNode(cargoProject, pkg));
        }
    }

    /**
     * A node shows an icon and its name. The update status of a Cargo project is not surfaced here:
     * the presentation carries an icon, text and a trailing suffix, with no tooltip or underline
     * attribute, so showing it would mean rendering the status as a suffix badge.
     */
    private static void node(@Nonnull Function<CargoTreeNode, TreeNode<CargoTreeNode>> nodeFactory,
                             @Nonnull CargoTreeNode value) {
        TreeNode<CargoTreeNode> node = nodeFactory.apply(value);
        node.setLeaf(value instanceof CargoTreeNode.TargetNode);
        node.setRenderer((item, presentation) -> {
            presentation.withIcon(iconOf(item));
            presentation.append(nameOf(item));
        });
    }

    @Override
    public boolean onDoubleClick(@Nonnull Tree<CargoTreeNode> tree, @Nonnull TreeNode<CargoTreeNode> node) {
        if (!(node.getValue() instanceof CargoTreeNode.TargetNode targetNode)) {
            return false;
        }
        CargoWorkspace.Target target = targetNode.target();
        String command = launchCommand(target);
        if (command == null) {
            LOG.warn("Can't create launch command for `" + target.getName() + "` target");
            return false;
        }
        String configurationName = Utils.capitalized(command) + " " + target.getName();
        CargoCommandLine.forTarget(target, command, Collections.emptyList())
            .run(targetNode.cargoProject(), configurationName);
        return true;
    }

    @Nonnull
    public static String nameOf(@Nonnull CargoTreeNode value) {
        return switch (value) {
            case CargoTreeNode.ProjectNode projectNode -> projectNode.cargoProject().getPresentableName();
            case CargoTreeNode.MemberNode memberNode -> memberNode.pkg().getName();
            case CargoTreeNode.TargetsNode ignored -> "targets";
            case CargoTreeNode.TargetNode targetNode -> targetNode.target().getName();
        };
    }

    @Nullable
    private static Image iconOf(@Nonnull CargoTreeNode value) {
        return switch (value) {
            case CargoTreeNode.ProjectNode ignored -> RustIconGroup.cargoproject();
            case CargoTreeNode.MemberNode ignored -> RustIconGroup.cargo();
            case CargoTreeNode.TargetsNode ignored -> RustIconGroup.targets();
            case CargoTreeNode.TargetNode targetNode -> targetIcon(targetNode.target());
        };
    }

    @Nullable
    private static Image targetIcon(@Nonnull CargoWorkspace.Target target) {
        CargoWorkspace.TargetKind kind = target.getKind();
        if (kind instanceof CargoWorkspace.TargetKind.Lib) return RustIconGroup.targetlib();
        if (kind instanceof CargoWorkspace.TargetKind.Bin) return RustIconGroup.targetbin();
        if (kind instanceof CargoWorkspace.TargetKind.Test) return RustIconGroup.targettest();
        if (kind instanceof CargoWorkspace.TargetKind.Bench) return RustIconGroup.targetbench();
        if (kind instanceof CargoWorkspace.TargetKind.ExampleBin) return RustIconGroup.targetexample();
        if (kind instanceof CargoWorkspace.TargetKind.ExampleLib) return RustIconGroup.targetexample();
        if (kind instanceof CargoWorkspace.TargetKind.CustomBuild) return RustIconGroup.targetcustombuild();
        return null;
    }

    @Nullable
    private static String launchCommand(@Nonnull CargoWorkspace.Target target) {
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
