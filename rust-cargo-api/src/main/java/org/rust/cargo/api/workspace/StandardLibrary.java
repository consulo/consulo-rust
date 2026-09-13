/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.api.workspace;

import org.rust.cargo.api.workspace.CargoWorkspace;

import org.rust.cargo.api.workspace.CargoWorkspaceData;
import org.rust.cargo.api.workspace.PackageOrigin;

import consulo.logging.Logger;
import consulo.project.Project;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.api.CargoConfig;
import org.rust.cargo.api.CfgOptions;
import org.rust.cargo.api.model.ProcessProgressListener;
import org.rust.cargo.api.model.RustcInfo;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;
import consulo.util.lang.SemVer;
import org.rust.cargo.api.toolchain.RustcVersion;
import org.rust.cargo.api.util.AutoInjectedCrates;
import org.rust.cargo.api.util.StdLibInfo;
import org.rust.experiments.RsExperiments;

public final class StandardLibrary {

    private static final Logger LOG = Logger.getInstance(StandardLibrary.class);

    private static final List<String> SRC_ROOTS = List.of("library", "src");
    private static final List<String> LIB_PATHS = List.of("src/lib.rs", "lib.rs");

    private final CargoWorkspaceData myWorkspaceData;
    private final boolean myIsHardcoded;
    private final boolean myIsPartOfCargoProject;

    public StandardLibrary(CargoWorkspaceData workspaceData, boolean isHardcoded, boolean isPartOfCargoProject) {
        myWorkspaceData = workspaceData;
        myIsHardcoded = isHardcoded;
        myIsPartOfCargoProject = isPartOfCargoProject;
    }

    public StandardLibrary(CargoWorkspaceData workspaceData, boolean isHardcoded) {
        this(workspaceData, isHardcoded, false);
    }

    public CargoWorkspaceData getWorkspaceData() {
        return myWorkspaceData;
    }

    public boolean isHardcoded() {
        return myIsHardcoded;
    }

    public boolean isPartOfCargoProject() {
        return myIsPartOfCargoProject;
    }

    public List<CargoWorkspaceData.Package> getCrates() {
        return myWorkspaceData.getPackages();
    }

    public StandardLibrary copy(CargoWorkspaceData workspaceData) {
        return new StandardLibrary(workspaceData, myIsHardcoded, myIsPartOfCargoProject);
    }





    
    @Nullable
    public static VirtualFile findSrcDir(VirtualFile sources) {
        if (!sources.isDirectory()) return null;
        if (SRC_ROOTS.contains(sources.getName())) {
            return sources;
        }
        VirtualFile found = findFirstFileByRelativePaths(sources, SRC_ROOTS);
        return found != null ? found : sources;
    }

    @Nullable
    public static StandardLibrary fetchHardcodedStdlib(VirtualFile srcDir) {
        Map<String, CargoWorkspaceData.Package> crates = new LinkedHashMap<>();

        for (org.rust.cargo.api.util.StdLibInfo libInfo : org.rust.cargo.api.util.AutoInjectedCrates.stdlibCrates) {
            List<String> packageSrcPaths = List.of(libInfo.name(), "lib" + libInfo.name());
            VirtualFile packageSrcDir = findFirstFileByRelativePaths(srcDir, packageSrcPaths);
            if (packageSrcDir != null) packageSrcDir = packageSrcDir.getCanonicalFile();
            VirtualFile libFile = (packageSrcDir != null) ? findFirstFileByRelativePaths(packageSrcDir, LIB_PATHS) : null;
            if (packageSrcDir != null && libFile != null) {
                CargoWorkspaceData.Package cratePkg = new CargoWorkspaceData.Package(
                    toStdlibId(libInfo.name()),
                    packageSrcDir.getUrl(),
                    libInfo.name(),
                    "",
                    Collections.singletonList(new CargoWorkspaceData.Target(
                        libFile.getUrl(),
                        libInfo.name(),
                        new CargoWorkspace.TargetKind.Lib(CargoWorkspace.LibKind.LIB),
                        CargoWorkspace.Edition.EDITION_2015,
                        true,
                        Collections.emptyList()
                    )),
                    null,
                    PackageOrigin.STDLIB,
                    CargoWorkspace.Edition.EDITION_2015,
                    Collections.emptyMap(),
                    Collections.emptySet(),
                    CfgOptions.EMPTY,
                    Collections.emptyMap(),
                    null
                );
                crates.put(cratePkg.getId(), cratePkg);
            }
        }

        Map<String, Set<CargoWorkspaceData.Dependency>> dependencies = new HashMap<>();
        List<CargoWorkspace.DepKindInfo> depKinds = Collections.singletonList(
            new CargoWorkspace.DepKindInfo(CargoWorkspace.DepKind.Stdlib)
        );

        for (org.rust.cargo.api.util.StdLibInfo libInfo : org.rust.cargo.api.util.AutoInjectedCrates.stdlibCrates) {
            String pkgId = toStdlibId(libInfo.name());
            if (!crates.containsKey(pkgId)) continue;

            for (String dependency : libInfo.dependencies()) {
                String dependencyId = toStdlibId(dependency);
                if (crates.containsKey(dependencyId)) {
                    dependencies.computeIfAbsent(pkgId, k -> new HashSet<>())
                        .add(new CargoWorkspaceData.Dependency(dependencyId, depKinds));
                }
            }
        }

        if (crates.isEmpty()) return null;
        CargoWorkspaceData data = new CargoWorkspaceData(new ArrayList<>(crates.values()), dependencies, Collections.emptyMap());
        return new StandardLibrary(data, true);
    }

    public static String toStdlibId(String packageId) {
        return "(stdlib) " + packageId;
    }

    @Nullable
    public static VirtualFile findFirstFileByRelativePaths(VirtualFile base, List<String> paths) {
        for (String path : paths) {
            VirtualFile file = base.findFileByRelativePath(path);
            if (file != null) return file;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StandardLibrary)) return false;
        StandardLibrary that = (StandardLibrary) o;
        return myIsHardcoded == that.myIsHardcoded &&
            myIsPartOfCargoProject == that.myIsPartOfCargoProject &&
            Objects.equals(myWorkspaceData, that.myWorkspaceData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myWorkspaceData, myIsHardcoded, myIsPartOfCargoProject);
    }

    @Override
    public String toString() {
        return "StandardLibrary(isHardcoded=" + myIsHardcoded + ", isPartOfCargoProject=" + myIsPartOfCargoProject + ")";
    }
}
