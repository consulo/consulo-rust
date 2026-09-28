/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

import org.rust.cargo.api.workspace.CargoWorkspace;

import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Derives the set of {@link CargoLibrary} instances a Cargo project contributes: every package of the
 * resolved workspace that is not a workspace member itself, and the build script output directories.
 * <p>
 * The standard library crates are the one exception - they belong to the toolchain rather than to a
 * Cargo project, and reach a module through the Rust bundle its module extension is bound to. The
 * crates the standard library itself depends on are not part of a toolchain installation: the
 * workspace refresh vendors them into the IDE system directory, keyed by the standard library sources
 * they were resolved from, so no bundle can carry them and they are libraries of the Cargo project
 * that pulled them in.
 * <p>
 * Results are cached on the {@link CargoProject} itself, which is replaced wholesale on every refresh,
 * so the cache expires together with the workspace it was computed from.
 */
public final class CargoLibraries {

    private static final Key<Map<String, CargoLibrary>> LIBRARIES = Key.create("org.rust.cargo.libraries");

    private static final String GENERATED_NAME = "generated code";

    private CargoLibraries() {
    }

    /**
     * Every library of {@code cargoProject}, in a stable order: the dependencies as the workspace lists
     * them, then the generated code.
     */
    @Nonnull
    public static List<CargoLibrary> libraries(@Nonnull CargoProject cargoProject) {
        return new ArrayList<>(libraryMap(cargoProject).values());
    }

    /**
     * Every library of every Cargo project attached to {@code project}.
     */
    @Nonnull
    public static List<CargoLibrary> libraries(@Nonnull Project project) {
        List<CargoLibrary> result = new ArrayList<>();
        for (CargoProject cargoProject : CargoProjectServiceUtil.getCargoProjects(project).getAllProjects()) {
            result.addAll(libraryMap(cargoProject).values());
        }
        return result;
    }

    /**
     * Looks a library up by the coordinates an order entry stores.
     */
    @Nullable
    public static CargoLibrary find(@Nonnull Project project, @Nonnull CargoLibrary.Kind kind, @Nonnull String id) {
        String key = key(kind, id);
        for (CargoProject cargoProject : CargoProjectServiceUtil.getCargoProjects(project).getAllProjects()) {
            CargoLibrary library = libraryMap(cargoProject).get(key);
            if (library != null) {
                return library;
            }
        }
        return null;
    }

    @Nonnull
    private static Map<String, CargoLibrary> libraryMap(@Nonnull CargoProject cargoProject) {
        Map<String, CargoLibrary> cached = cargoProject.getUserData(LIBRARIES);
        if (cached != null) {
            return cached;
        }
        return cargoProject.putUserDataIfAbsent(LIBRARIES, compute(cargoProject));
    }

    @Nonnull
    private static String key(@Nonnull CargoLibrary.Kind kind, @Nonnull String id) {
        return kind.name() + " " + id;
    }

    @Nonnull
    private static Map<String, CargoLibrary> compute(@Nonnull CargoProject cargoProject) {
        CargoWorkspace workspace = cargoProject.getWorkspace();
        if (workspace == null) {
            return Collections.emptyMap();
        }

        List<CargoWorkspace.Package> dependencyPackages = new ArrayList<>();
        List<CargoWorkspace.Package> stdlibPackages = new ArrayList<>();
        for (CargoWorkspace.Package pkg : workspace.getPackages()) {
            switch (pkg.getOrigin()) {
                case DEPENDENCY:
                case STDLIB_DEPENDENCY:
                    dependencyPackages.add(pkg);
                    break;
                case STDLIB:
                    stdlibPackages.add(pkg);
                    break;
                case WORKSPACE:
                    // workspace members are modules of their own
                    break;
            }
        }

        Map<String, CargoLibrary> result = new LinkedHashMap<>();

        for (CargoWorkspace.Package pkg : dependencyPackages) {
            CargoLibrary library = makeDependencyLibrary(pkg);
            if (library != null) {
                result.put(key(library.getKind(), library.getId()), library);
            }
        }

        for (CargoWorkspace.Package pkg : stdlibPackages) {
            CargoLibrary library = makeStdlibLibrary(pkg);
            if (library != null) {
                result.put(key(library.getKind(), library.getId()), library);
            }
        }

        for (CargoWorkspace.Package pkg : workspace.getPackages()) {
            CargoLibrary generated = makeGeneratedCodeLibrary(pkg);
            if (generated != null) {
                result.put(key(generated.getKind(), generated.getId()), generated);
            }
        }

        return result;
    }

    @Nullable
    private static CargoLibrary makeDependencyLibrary(@Nonnull CargoWorkspace.Package pkg) {
        VirtualFile root = pkg.getContentRoot();
        if (root == null) return null;

        Set<VirtualFile> sourceRoots = new LinkedHashSet<>();
        Set<VirtualFile> reachableCrateRoots = new LinkedHashSet<>();
        List<VirtualFile> unreachableCrateRoots = new ArrayList<>();
        for (CargoWorkspace.Target target : pkg.getTargets()) {
            VirtualFile crateRoot = target.getCrateRoot();
            if (crateRoot == null) continue;
            if (target.getKind().isLib() || target.getKind().isCustomBuild()) {
                reachableCrateRoots.add(crateRoot);
                VirtualFile crateRootDir = crateRoot.getParent();
                VirtualFile commonAncestor = VirtualFileUtil.getCommonAncestor(root, crateRootDir);
                if (root.equals(commonAncestor)) {
                    sourceRoots.add(root);
                }
                else if (crateRootDir.equals(commonAncestor)) {
                    sourceRoots.add(crateRootDir);
                }
                else {
                    sourceRoots.add(root);
                    sourceRoots.add(crateRootDir);
                }
            }
            else {
                unreachableCrateRoots.add(crateRoot);
            }
        }

        Set<VirtualFile> excludedRoots = new LinkedHashSet<>();
        for (VirtualFile crateRoot : unreachableCrateRoots) {
            // the whole directory of a test, benchmark or example target, so that the modules it pulls
            // in are left out as well and not only the crate root itself
            VirtualFile directory = crateRoot.getParent();
            if (directory != null && excludable(directory, sourceRoots, reachableCrateRoots)) {
                excludedRoots.add(directory);
            }
            else if (excludable(crateRoot, sourceRoots, reachableCrateRoots)) {
                excludedRoots.add(crateRoot);
            }
        }

        return new CargoLibrary(
            CargoLibrary.Kind.DEPENDENCY,
            pkg.getId(),
            pkg.getName(),
            pkg.getVersion(),
            sourceRoots,
            excludedRoots
        );
    }

    /**
     * Whether {@code candidate} can be taken out of the library without taking anything the rest of the
     * project refers to with it. Excluding a source root, or a directory holding one, hides the whole
     * library from the index, and a bin target sharing its directory with the lib target of the same
     * package would do exactly that.
     */
    private static boolean excludable(
        @Nonnull VirtualFile candidate,
        @Nonnull Set<VirtualFile> sourceRoots,
        @Nonnull Set<VirtualFile> reachableCrateRoots
    ) {
        boolean belowSourceRoot = false;
        for (VirtualFile sourceRoot : sourceRoots) {
            if (VirtualFileUtil.isAncestor(candidate, sourceRoot, false)) return false;
            if (VirtualFileUtil.isAncestor(sourceRoot, candidate, true)) {
                belowSourceRoot = true;
            }
        }
        if (!belowSourceRoot) return false;

        for (VirtualFile crateRoot : reachableCrateRoots) {
            if (VirtualFileUtil.isAncestor(candidate, crateRoot, false)) return false;
        }
        return true;
    }

    /**
     * One crate of the standard library. It has no exclusions - the whole crate is meant to be
     * navigable - and its roots are re-derived from the bundle when the entry is read back, so the
     * roots given here only matter for the session that computed them.
     */
    @Nullable
    private static CargoLibrary makeStdlibLibrary(@Nonnull CargoWorkspace.Package pkg) {
        VirtualFile root = pkg.getContentRoot();
        if (root == null) return null;

        Set<VirtualFile> sourceRoots = new LinkedHashSet<>();
        sourceRoots.add(root);
        sourceRoots.addAll(CargoWorkspace.additionalRoots(pkg));

        String version = pkg.getVersion();
        return new CargoLibrary(
            CargoLibrary.Kind.STDLIB,
            pkg.getId(),
            pkg.getName(),
            version.isEmpty() ? null : version,
            sourceRoots,
            Collections.emptySet()
        );
    }

    /**
     * The build script output of one package. There is an entry per package rather than one per Cargo
     * project, because the entry names the package it belongs to and so answers which package a file
     * under {@code OUT_DIR} belongs to.
     */
    @Nullable
    private static CargoLibrary makeGeneratedCodeLibrary(@Nonnull CargoWorkspace.Package pkg) {
        VirtualFile outDir = pkg.getOutDir();
        if (outDir == null) return null;
        return new CargoLibrary(
            CargoLibrary.Kind.GENERATED,
            pkg.getId(),
            GENERATED_NAME + " (" + pkg.getName() + ")",
            null,
            Set.of(outDir),
            Collections.emptySet()
        );
    }
}
