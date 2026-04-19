/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider;
import com.intellij.openapi.roots.SyntheticLibrary;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.rust.cargo.icons.CargoIcons;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.toolchain.impl.RustcVersion;
import org.rust.ide.icons.RsIcons;

import java.util.*;

public class RsAdditionalLibraryRootsProvider extends AdditionalLibraryRootsProvider {

    @Override
    public Collection<SyntheticLibrary> getAdditionalProjectLibraries(Project project) {
        Collection<CargoProject> allProjects = CargoProjectServiceUtil.getCargoProjects(project).getAllProjects();
        if (allProjects.isEmpty()) return Collections.emptyList();
        if (allProjects.size() == 1) {
            return getIdeaLibraries(allProjects.iterator().next());
        }
        List<SyntheticLibrary> result = new ArrayList<>();
        for (CargoProject cp : allProjects) {
            result.addAll(getIdeaLibraries(cp));
        }
        return result;
    }

    @Override
    public Collection<VirtualFile> getRootsToWatch(Project project) {
        Collection<SyntheticLibrary> libs = getAdditionalProjectLibraries(project);
        List<VirtualFile> roots = new ArrayList<>();
        for (SyntheticLibrary lib : libs) {
            roots.addAll(lib.getSourceRoots());
        }
        return roots;
    }

    private static Collection<SyntheticLibrary> getIdeaLibraries(CargoProject cargoProject) {
        CargoWorkspace workspace = cargoProject.getWorkspace();
        if (workspace == null) return Collections.emptyList();

        List<CargoWorkspace.Package> stdlibPackages = new ArrayList<>();
        List<CargoWorkspace.Package> dependencyPackages = new ArrayList<>();
        for (CargoWorkspace.Package pkg : workspace.getPackages()) {
            switch (pkg.getOrigin()) {
                case STDLIB:
                case STDLIB_DEPENDENCY:
                    stdlibPackages.add(pkg);
                    break;
                case DEPENDENCY:
                    dependencyPackages.add(pkg);
                    break;
                case WORKSPACE:
                    // skip
                    break;
            }
        }

        List<SyntheticLibrary> result = new ArrayList<>();

        RustcVersion rustcVersion = cargoProject.getRustcInfo() != null ? cargoProject.getRustcInfo().getVersion() : null;
        CargoLibrary stdlibLib = makeStdlibLibrary(stdlibPackages, rustcVersion);
        if (stdlibLib != null) {
            result.add(stdlibLib);
        }

        for (CargoWorkspace.Package pkg : dependencyPackages) {
            CargoLibrary lib = toCargoLibrary(pkg);
            if (lib != null) {
                result.add(lib);
            }
        }

        GeneratedCodeFakeLibrary fakeLib = GeneratedCodeFakeLibrary.create(cargoProject);
        if (fakeLib != null) {
            result.add(fakeLib);
        }

        return result;
    }

    private static CargoLibrary makeStdlibLibrary(List<CargoWorkspace.Package> packages, RustcVersion rustcVersion) {
        if (packages.isEmpty()) return null;
        Set<VirtualFile> sourceRoots = new LinkedHashSet<>();
        Set<VirtualFile> excludedRoots = new LinkedHashSet<>();
        for (CargoWorkspace.Package pkg : packages) {
            VirtualFile root = pkg.getContentRoot();
            if (root == null) continue;
            sourceRoots.add(root);
            sourceRoots.addAll(CargoWorkspace.additionalRoots(pkg));
        }

        for (VirtualFile root : sourceRoots) {
            addIfNotNull(excludedRoots, root.findChild("tests"));
            addIfNotNull(excludedRoots, root.findChild("benches"));
            addIfNotNull(excludedRoots, root.findChild("examples"));
            addIfNotNull(excludedRoots, root.findChild("ci"));
            addIfNotNull(excludedRoots, root.findChild(".github"));
            addIfNotNull(excludedRoots, root.findChild("libc-test"));
        }

        String version = (rustcVersion != null && rustcVersion.getSemver() != null)
            ? rustcVersion.getSemver().getParsedVersion()
            : null;
        return new CargoLibrary("stdlib", sourceRoots, excludedRoots, RsIcons.RUST, version);
    }

    private static void addIfNotNull(Set<VirtualFile> set, VirtualFile file) {
        if (file != null) set.add(file);
    }

    private static CargoLibrary toCargoLibrary(CargoWorkspace.Package pkg) {
        VirtualFile root = pkg.getContentRoot();
        if (root == null) return null;
        Set<VirtualFile> sourceRoots = new LinkedHashSet<>();
        Set<VirtualFile> excludedRoots = new LinkedHashSet<>();
        for (CargoWorkspace.Target target : pkg.getTargets()) {
            VirtualFile crateRoot = target.getCrateRoot();
            if (crateRoot == null) continue;
            if (target.getKind().isLib() || target.getKind().isCustomBuild()) {
                VirtualFile crateRootDir = crateRoot.getParent();
                VirtualFile commonAncestor = VfsUtilCore.getCommonAncestor(root, crateRootDir);
                if (root.equals(commonAncestor)) {
                    sourceRoots.add(root);
                } else if (crateRootDir.equals(commonAncestor)) {
                    sourceRoots.add(crateRootDir);
                } else {
                    sourceRoots.add(root);
                    sourceRoots.add(crateRootDir);
                }
            } else {
                // TODO exclude full module hierarchy instead of crate roots only
                excludedRoots.add(crateRoot);
            }
        }
        return new CargoLibrary(pkg.getName(), sourceRoots, excludedRoots, CargoIcons.ICON, pkg.getVersion());
    }
}
