/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.toolwindow;

import consulo.rust.icon.RustIconGroup;
import consulo.ui.ex.tree.PresentationData;
import consulo.disposer.Disposable;
import consulo.util.lang.StringUtil;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.tree.AsyncTreeModel;
import consulo.ui.ex.awt.tree.StructureTreeModel;
import consulo.ui.ex.awt.tree.CachingSimpleNode;
import consulo.ui.ex.awt.tree.SimpleNode;
import consulo.ui.ex.awt.tree.SimpleTreeStructure;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;
import consulo.ui.image.Image;

public class CargoProjectTreeStructure extends SimpleTreeStructure {

    private final StructureTreeModel<CargoProjectTreeStructure> treeModel;
    private List<CargoProject> cargoProjects;
    private Root root;

    public CargoProjectTreeStructure(@Nonnull CargoProjectsTree tree, @Nonnull Disposable parentDisposable,
                                     @Nonnull List<CargoProject> cargoProjects) {
        this.cargoProjects = cargoProjects;
        this.treeModel = new StructureTreeModel<>(this, parentDisposable);
        this.root = new Root(cargoProjects);
        tree.setModel(new AsyncTreeModel(treeModel, parentDisposable));
    }

    public CargoProjectTreeStructure(@Nonnull CargoProjectsTree tree, @Nonnull Disposable parentDisposable) {
        this(tree, parentDisposable, Collections.emptyList());
    }

    @Nonnull
    @Override
    public Object getRootElement() {
        return root;
    }

    public void updateCargoProjects(@Nonnull List<CargoProject> cargoProjects) {
        this.cargoProjects = cargoProjects;
        root = new Root(cargoProjects);
        treeModel.invalidate();
    }

    // -- Node classes --

    public static abstract class CargoSimpleNode extends CachingSimpleNode {
        protected CargoSimpleNode(SimpleNode parent) {
            super(parent);
        }

        @Nonnull
        public abstract String toTestString();
    }

    public static class Root extends CargoSimpleNode {
        private final List<CargoProject> cargoProjects;

        public Root(@Nonnull List<CargoProject> cargoProjects) {
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

        @Nonnull
        @Override
        public String toTestString() { return "Root"; }
    }

    public static class Project extends CargoSimpleNode {
        @Nonnull
        public final CargoProject cargoProject;

        public Project(@Nonnull CargoProject cargoProject, @Nonnull SimpleNode parent) {
            super(parent);
            this.cargoProject = cargoProject;
            setIcon(RustIconGroup.cargoproject());
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
        public void update(@Nonnull PresentationData presentation) {
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

        @Nonnull
        @Override
        public String toTestString() { return "Project"; }
    }

    public static class WorkspaceMember extends CargoSimpleNode {
        @Nonnull
        public final CargoWorkspace.Package pkg;

        public WorkspaceMember(@Nonnull CargoWorkspace.Package pkg, @Nonnull SimpleNode parent) {
            super(parent);
            this.pkg = pkg;
            setIcon(RustIconGroup.cargo());
        }

        @Override
        protected SimpleNode[] buildChildren() {
            return new SimpleNode[]{new Targets(pkg.getTargets(), this)};
        }

        @Override
        public String getName() { return pkg.getName(); }

        @Nonnull
        @Override
        public String toTestString() { return "WorkspaceMember(" + getName() + ")"; }
    }

    public static class Targets extends CargoSimpleNode {
        @Nonnull
        public final Collection<CargoWorkspace.Target> targets;

        public Targets(@Nonnull Collection<CargoWorkspace.Target> targets, @Nonnull SimpleNode parent) {
            super(parent);
            this.targets = targets;
            setIcon(RustIconGroup.targets());
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

        @Nonnull
        @Override
        public String toTestString() { return "Targets"; }
    }

    public static class Target extends CargoSimpleNode {
        @Nonnull
        public final CargoWorkspace.Target target;

        public Target(@Nonnull CargoWorkspace.Target target, @Nonnull SimpleNode parent) {
            super(parent);
            this.target = target;
            setIcon(getTargetIcon(target));
        }

        @Override
        protected SimpleNode[] buildChildren() { return new SimpleNode[0]; }

        @Override
        public String getName() { return target.getName(); }

        @Override
        public void update(@Nonnull PresentationData presentation) {
            super.update(presentation);
            CargoWorkspace.TargetKind kind = target.getKind();
            if (!(kind instanceof CargoWorkspace.TargetKind.Unknown)) {
                presentation.setTooltip(RsBundle.message("tooltip.target",
                    StringUtil.capitalize(kind.getName()), getName()));
            }
        }

        @Nonnull
        @Override
        public String toTestString() {
            return "Target(" + target.getName() + "[" + target.getKind().getName().toLowerCase() + "])";
        }

        @Nullable
        private static consulo.ui.image.Image getTargetIcon(@Nonnull CargoWorkspace.Target target) {
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
    }
}
