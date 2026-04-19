/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.toolwindow;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.CachingSimpleNode;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.icons.CargoIcons;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

public class CargoProjectTreeStructure extends SimpleTreeStructure {

    private final StructureTreeModel<CargoProjectTreeStructure> treeModel;
    private List<CargoProject> cargoProjects;
    private Root root;

    public CargoProjectTreeStructure(@NotNull CargoProjectsTree tree, @NotNull Disposable parentDisposable,
                                     @NotNull List<CargoProject> cargoProjects) {
        this.cargoProjects = cargoProjects;
        this.treeModel = new StructureTreeModel<>(this, parentDisposable);
        this.root = new Root(cargoProjects);
        tree.setModel(new AsyncTreeModel(treeModel, parentDisposable));
    }

    public CargoProjectTreeStructure(@NotNull CargoProjectsTree tree, @NotNull Disposable parentDisposable) {
        this(tree, parentDisposable, Collections.emptyList());
    }

    @NotNull
    @Override
    public Object getRootElement() {
        return root;
    }

    public void updateCargoProjects(@NotNull List<CargoProject> cargoProjects) {
        this.cargoProjects = cargoProjects;
        root = new Root(cargoProjects);
        treeModel.invalidate();
    }

    // -- Node classes --

    public static abstract class CargoSimpleNode extends CachingSimpleNode {
        protected CargoSimpleNode(SimpleNode parent) {
            super(parent);
        }

        @NotNull
        public abstract String toTestString();
    }

    public static class Root extends CargoSimpleNode {
        private final List<CargoProject> cargoProjects;

        public Root(@NotNull List<CargoProject> cargoProjects) {
            super(null);
            this.cargoProjects = cargoProjects;
        }

        @Override
        protected SimpleNode[] buildChildren() {
            return cargoProjects.stream()
                .map(cp -> new Project(cp, this))
                .sorted(Comparator.comparing(SimpleNode::getName))
                .toArray(SimpleNode[]::new);
        }

        @Override
        public String getName() { return ""; }

        @NotNull
        @Override
        public String toTestString() { return "Root"; }
    }

    public static class Project extends CargoSimpleNode {
        @NotNull
        public final CargoProject cargoProject;

        public Project(@NotNull CargoProject cargoProject, @NotNull SimpleNode parent) {
            super(parent);
            this.cargoProject = cargoProject;
            setIcon(CargoIcons.ICON);
        }

        @Override
        protected SimpleNode[] buildChildren() {
            CargoWorkspace workspace = cargoProject.getWorkspace();
            if (workspace == null) return new SimpleNode[0];

            List<CargoWorkspace.Package> workspacePackages = workspace.getPackages().stream()
                .filter(p -> p.getOrigin() == PackageOrigin.WORKSPACE)
                .sorted(Comparator.comparing(CargoWorkspace.Package::getName))
                .collect(Collectors.toList());

            java.nio.file.Path workingDir = CargoCommandConfiguration.getWorkingDirectory(cargoProject);
            List<SimpleNode> children = new ArrayList<>();
            List<CargoWorkspace.Package> others = new ArrayList<>();

            for (CargoWorkspace.Package pkg : workspacePackages) {
                if (pkg.getRootDirectory().equals(workingDir)) {
                    children.add(new Targets(pkg.getTargets(), this));
                } else {
                    others.add(pkg);
                }
            }
            for (CargoWorkspace.Package pkg : others) {
                children.add(new WorkspaceMember(pkg, this));
            }
            return children.toArray(new SimpleNode[0]);
        }

        @Override
        public String getName() { return cargoProject.getPresentableName(); }

        @Override
        public void update(@NotNull PresentationData presentation) {
            SimpleTextAttributes attrs = SimpleTextAttributes.REGULAR_ATTRIBUTES;
            CargoProject.UpdateStatus status = cargoProject.getMergedStatus();
            if (status instanceof CargoProject.UpdateStatus.UpdateFailed failed) {
                attrs = attrs.derive(SimpleTextAttributes.STYLE_WAVED, null, null, JBColor.RED);
                presentation.setTooltip(failed.getReason());
            } else if (status instanceof CargoProject.UpdateStatus.NeedsUpdate) {
                attrs = attrs.derive(SimpleTextAttributes.STYLE_WAVED, null, null, JBColor.GRAY);
                presentation.setTooltip(RsBundle.message("tooltip.project.needs.update"));
            } else {
                presentation.setTooltip(RsBundle.message("tooltip.project.up.to.date"));
            }
            presentation.addText(cargoProject.getPresentableName(), attrs);
            presentation.setIcon(getIcon());
        }

        @NotNull
        @Override
        public String toTestString() { return "Project"; }
    }

    public static class WorkspaceMember extends CargoSimpleNode {
        @NotNull
        public final CargoWorkspace.Package pkg;

        public WorkspaceMember(@NotNull CargoWorkspace.Package pkg, @NotNull SimpleNode parent) {
            super(parent);
            this.pkg = pkg;
            setIcon(CargoIcons.ICON);
        }

        @Override
        protected SimpleNode[] buildChildren() {
            return new SimpleNode[]{new Targets(pkg.getTargets(), this)};
        }

        @Override
        public String getName() { return pkg.getName(); }

        @NotNull
        @Override
        public String toTestString() { return "WorkspaceMember(" + getName() + ")"; }
    }

    public static class Targets extends CargoSimpleNode {
        @NotNull
        public final Collection<CargoWorkspace.Target> targets;

        public Targets(@NotNull Collection<CargoWorkspace.Target> targets, @NotNull SimpleNode parent) {
            super(parent);
            this.targets = targets;
            setIcon(CargoIcons.TARGETS);
        }

        @Override
        protected SimpleNode[] buildChildren() {
            return targets.stream()
                .map(t -> new Target(t, this))
                .sorted(Comparator.comparing(SimpleNode::getName))
                .toArray(SimpleNode[]::new);
        }

        @Override
        public String getName() { return "targets"; }

        @NotNull
        @Override
        public String toTestString() { return "Targets"; }
    }

    public static class Target extends CargoSimpleNode {
        @NotNull
        public final CargoWorkspace.Target target;

        public Target(@NotNull CargoWorkspace.Target target, @NotNull SimpleNode parent) {
            super(parent);
            this.target = target;
            setIcon(getTargetIcon(target));
        }

        @Override
        protected SimpleNode[] buildChildren() { return new SimpleNode[0]; }

        @Override
        public String getName() { return target.getName(); }

        @Override
        public void update(@NotNull PresentationData presentation) {
            super.update(presentation);
            CargoWorkspace.TargetKind kind = target.getKind();
            if (!(kind instanceof CargoWorkspace.TargetKind.Unknown)) {
                presentation.setTooltip(RsBundle.message("tooltip.target",
                    StringUtil.capitalize(kind.getName()), getName()));
            }
        }

        @NotNull
        @Override
        public String toTestString() {
            return "Target(" + target.getName() + "[" + target.getKind().getName().toLowerCase() + "])";
        }

        @Nullable
        private static Icon getTargetIcon(@NotNull CargoWorkspace.Target target) {
            CargoWorkspace.TargetKind kind = target.getKind();
            if (kind instanceof CargoWorkspace.TargetKind.Lib) return CargoIcons.LIB_TARGET;
            if (kind instanceof CargoWorkspace.TargetKind.Bin) return CargoIcons.BIN_TARGET;
            if (kind instanceof CargoWorkspace.TargetKind.Test) return CargoIcons.TEST_TARGET;
            if (kind instanceof CargoWorkspace.TargetKind.Bench) return CargoIcons.BENCH_TARGET;
            if (kind instanceof CargoWorkspace.TargetKind.ExampleBin) return CargoIcons.EXAMPLE_TARGET;
            if (kind instanceof CargoWorkspace.TargetKind.ExampleLib) return CargoIcons.EXAMPLE_TARGET;
            if (kind instanceof CargoWorkspace.TargetKind.CustomBuild) return CargoIcons.CUSTOM_BUILD_TARGET;
            return null;
        }
    }
}
