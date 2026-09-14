/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.toolwindow;


import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.rust.icon.RustIconGroup;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.RustcInfo;
import consulo.application.WriteAction;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.rust.module.extension.RustModuleExtension;
import consulo.rust.module.extension.RustMutableModuleExtension;
import consulo.virtualFileSystem.VirtualFile;
import org.rust.cargo.api.model.CargoProjectsUtil;
import org.rust.cargo.api.toolchain.RustcVersion;
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
            case CargoTreeNode.PlatformsNode platformsNode -> buildPlatformChildren(nodeFactory, platformsNode);
            case CargoTreeNode.PlatformNode ignored -> {
            }
        }
    }

    /**
     * The triples the toolchain can compile for. The list comes from the toolchain itself, so it is
     * whatever {@code rustc --print target-list} reported when the project was last synced.
     */
    private static void buildPlatformChildren(@Nonnull Function<CargoTreeNode, TreeNode<CargoTreeNode>> nodeFactory,
                                              @Nonnull CargoTreeNode.PlatformsNode platformsNode) {
        CargoProject cargoProject = platformsNode.cargoProject();
        List<String> triples = availableTriples(cargoProject);
        String active = activeTriple(cargoProject);
        // The one in effect first, so it is visible without scrolling a list this long.
        triples.stream()
            .sorted(Comparator.comparing((String triple) -> !triple.equals(active)).thenComparing(triple -> triple))
            .forEach(triple -> node(nodeFactory, new CargoTreeNode.PlatformNode(cargoProject, triple)));
    }

    @Nonnull
    private static List<String> availableTriples(@Nonnull CargoProject cargoProject) {
        RustcInfo rustcInfo = cargoProject.getRustcInfo();
        List<String> targets = rustcInfo == null ? null : rustcInfo.getTargets();
        List<String> result = new ArrayList<>(targets == null ? List.of() : targets);
        String active = activeTriple(cargoProject);
        if (active != null && !result.contains(active)) {
            result.add(active);
        }
        return result;
    }

    /**
     * The triple the project is currently viewed as: the explicit choice when there is one, and
     * otherwise the toolchain host, which is what cargo falls back to.
     */
    @Nullable
    private static String activeTriple(@Nonnull CargoProject cargoProject) {
        String selected = selectedTriple(cargoProject);
        if (selected != null) {
            return selected;
        }
        RustcInfo rustcInfo = cargoProject.getRustcInfo();
        RustcVersion version = rustcInfo == null ? null : rustcInfo.getVersion();
        return version == null ? null : version.getHost();
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

        node(nodeFactory, new CargoTreeNode.PlatformsNode(cargoProject));

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
        node.setLeaf(value instanceof CargoTreeNode.TargetNode || value instanceof CargoTreeNode.PlatformNode);
        node.setRenderer((item, presentation) -> {
            presentation.withIcon(iconOf(item));
            if (item instanceof CargoTreeNode.PlatformNode platformNode
                && platformNode.triple().equals(activeTriple(platformNode.cargoProject()))) {
                presentation.append(nameOf(item), TextAttribute.REGULAR_BOLD);
                presentation.withSuffix(LocalizeValue.localizeTODO("active"), null);
            }
            else {
                presentation.append(nameOf(item));
            }
        });
    }

    /**
     * The result answers whether the tree should expand the row, so a node that acts on the click
     * reports {@code false} and every other node reports {@code true} to keep expanding.
     */
    @Override
    public boolean onDoubleClick(@Nonnull Tree<CargoTreeNode> tree, @Nonnull TreeNode<CargoTreeNode> node) {
        return switch (node.getValue()) {
            case CargoTreeNode.TargetNode targetNode -> {
                runTarget(targetNode);
                yield false;
            }
            case CargoTreeNode.PlatformNode platformNode -> {
                activatePlatform(platformNode);
                yield false;
            }
            default -> true;
        };
    }

    private static void runTarget(@Nonnull CargoTreeNode.TargetNode targetNode) {
        CargoWorkspace.Target target = targetNode.target();
        String command = launchCommand(target);
        if (command == null) {
            LOG.warn("Can't create launch command for `" + target.getName() + "` target");
            return;
        }
        String configurationName = Utils.capitalized(command) + " " + target.getName();
        CargoCommandLine.forTarget(target, command, Collections.emptyList())
            .run(targetNode.cargoProject(), configurationName);
    }

    /**
     * Makes the double-clicked triple the one the project is viewed as, or clears the choice when it
     * is already in effect so the project follows cargo's own configuration again. The settings
     * change is what triggers the re-sync that reloads dependencies and cfg for the new platform.
     */
    private static void activatePlatform(@Nonnull CargoTreeNode.PlatformNode platformNode) {
        CargoProject cargoProject = platformNode.cargoProject();
        Project project = cargoProject.getProject();
        String triple = platformNode.triple();
        String next = triple.equals(selectedTriple(cargoProject)) ? null : triple;

        Module module = moduleOf(cargoProject);
        if (module == null) {
            return;
        }
        ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
        RustMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(RustMutableModuleExtension.class);
        if (extension == null) {
            rootModel.dispose();
            return;
        }
        extension.setBuildTarget(next);
        // The model is created and edited without the lock; committing it is what needs one.
        WriteAction.run(rootModel::commit);

        // The module model carries no cargo-metadata signal of its own, so the reload is explicit.
        CargoProjectsUtil.getCargoProjects(project).refreshAllProjects();
    }

    @Nullable
    private static String selectedTriple(@Nonnull CargoProject cargoProject) {
        RustModuleExtension extension = RustModuleExtension.findExtension(cargoProject.getProject(), cargoProject.getRootDir());
        return extension == null ? null : extension.getBuildTarget();
    }

    @Nullable
    private static Module moduleOf(@Nonnull CargoProject cargoProject) {
        VirtualFile rootDir = cargoProject.getRootDir();
        return rootDir == null ? null : ModuleUtilCore.findModuleForFile(rootDir, cargoProject.getProject());
    }

    @Nonnull
    public static String nameOf(@Nonnull CargoTreeNode value) {
        return switch (value) {
            case CargoTreeNode.ProjectNode projectNode -> projectNode.cargoProject().getPresentableName();
            case CargoTreeNode.MemberNode memberNode -> memberNode.pkg().getName();
            case CargoTreeNode.TargetsNode ignored -> "targets";
            case CargoTreeNode.TargetNode targetNode -> targetNode.target().getName();
            case CargoTreeNode.PlatformsNode ignored -> "platforms";
            case CargoTreeNode.PlatformNode platformNode -> platformNode.triple();
        };
    }

    @Nullable
    private static Image iconOf(@Nonnull CargoTreeNode value) {
        return switch (value) {
            case CargoTreeNode.ProjectNode ignored -> RustIconGroup.cargoproject();
            case CargoTreeNode.MemberNode ignored -> RustIconGroup.cargo();
            case CargoTreeNode.TargetsNode ignored -> RustIconGroup.targets();
            case CargoTreeNode.TargetNode targetNode -> targetIcon(targetNode.target());
            case CargoTreeNode.PlatformsNode ignored -> PlatformIconGroup.generalGearplain()
            case CargoTreeNode.PlatformNode ignored -> null;
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
