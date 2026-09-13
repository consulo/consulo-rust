/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsService;

import consulo.process.cmd.ParametersListUtil;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.workspace.CargoWorkspace;

import java.util.ArrayList;
import java.util.Collection;
import java.nio.file.Paths;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.LocalFileSystem;
import org.rust.cargo.api.workspace.PackageOrigin;
import java.nio.file.Path;
import java.util.List;

/**
 * Locates a cargo project, package or target from a command line and a working directory.
 * <p>
 * These answer questions about the workspace alone. They used to be statics on the cargo run
 * configuration, which put the project model in the position of depending on the run configuration.
 */
public final class CargoProjectLocator {
    private CargoProjectLocator() {
    }

    @Nullable
    public static CargoProject findCargoProject(Project project, List<String> additionalArgs, @Nullable Path workingDirectory) {
        CargoProjectsService cargoProjects = CargoProjectServiceUtil.getCargoProjects(project);
        Collection<CargoProject> allProjects = cargoProjects.getAllProjects();
        if (allProjects.size() == 1) return allProjects.iterator().next();

        int idx = additionalArgs.indexOf("--manifest-path");
        Path manifestPath = null;
        if (idx != -1 && idx + 1 < additionalArgs.size()) {
            manifestPath = Paths.get(additionalArgs.get(idx + 1));
        }

        List<Path> dirs = new ArrayList<>();
        if (manifestPath != null && manifestPath.getParent() != null) dirs.add(manifestPath.getParent());
        if (workingDirectory != null) dirs.add(workingDirectory);

        for (Path dir : dirs) {
            VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(dir.toFile());
            if (vFile != null) {
                CargoProject found = cargoProjects.findProjectForFile(vFile);
                if (found != null) return found;
            }
        }
        return null;
    }

    @Nullable
    public static CargoProject findCargoProject(Project project, String cmd, @Nullable Path workingDirectory) {
        return findCargoProject(project, ParametersListUtil.parse(cmd), workingDirectory);
    }

    @Nullable
    public static CargoWorkspace.Package findCargoPackage(
        CargoProject cargoProject,
        List<String> additionalArgs,
        @Nullable Path workingDirectory
    ) {
        CargoWorkspace workspace = cargoProject.getWorkspace();
        if (workspace == null) return null;
        List<CargoWorkspace.Package> packages = new ArrayList<>();
        for (var pkg : workspace.getPackages()) {
            if (pkg.getOrigin() == PackageOrigin.WORKSPACE) {
                packages.add(pkg);
            }
        }
        if (packages.isEmpty()) return null;
        if (packages.size() == 1) return packages.get(0);

        int idx = additionalArgs.indexOf("--package");
        if (idx != -1 && idx + 1 < additionalArgs.size()) {
            String packageName = additionalArgs.get(idx + 1);
            for (var pkg : packages) {
                if (pkg.getName().equals(packageName)) return pkg;
            }
        }

        for (var pkg : packages) {
            if (pkg.getRootDirectory() != null && pkg.getRootDirectory().equals(workingDirectory)) return pkg;
        }
        return null;
    }

    public static List<CargoWorkspace.Target> findCargoTargets(
        CargoWorkspace.Package cargoPackage,
        List<String> additionalArgs
    ) {
        List<CargoWorkspace.Target> result = new ArrayList<>();
        for (CargoWorkspace.Target target : cargoPackage.getTargets()) {
            CargoWorkspace.TargetKind kind = target.getKind();
            boolean matches = false;
            if (kind == CargoWorkspace.TargetKind.Bin.INSTANCE) {
                matches = hasTarget(additionalArgs, "--bin", target.getName());
            } else if (kind == CargoWorkspace.TargetKind.Test.INSTANCE) {
                matches = hasTarget(additionalArgs, "--test", target.getName());
            } else if (kind == CargoWorkspace.TargetKind.ExampleBin.INSTANCE) {
                matches = hasTarget(additionalArgs, "--example", target.getName());
            } else if (kind == CargoWorkspace.TargetKind.Bench.INSTANCE) {
                matches = hasTarget(additionalArgs, "--bench", target.getName());
            } else if (kind.isLib()) {
                matches = additionalArgs.contains("--lib");
            } else if (kind instanceof CargoWorkspace.TargetKind.ExampleLib) {
                matches = hasTarget(additionalArgs, "--example", target.getName());
            }
            if (matches) result.add(target);
        }
        return result;
    }

    private static boolean hasTarget(List<String> args, String option, String name) {
        if (args.contains(option + "=" + name)) return true;
        for (int i = 0; i < args.size() - 1; i++) {
            if (args.get(i).equals(option) && args.get(i + 1).equals(name)) return true;
        }
        return false;
    }
    /**
     * Returns the working directory for a cargo project.
     * This is the parent directory of the project's manifest file (Cargo.toml).
     */
    @Nonnull
    public static Path getWorkingDirectory(@Nonnull CargoProject cargoProject) {
        return cargoProject.getManifest().getParent();
    }

}
