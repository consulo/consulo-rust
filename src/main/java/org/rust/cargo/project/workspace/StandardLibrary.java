/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.CargoConfig;
import org.rust.cargo.CfgOptions;
import org.rust.cargo.project.model.ProcessProgressListener;
import org.rust.cargo.project.model.RustcInfo;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;

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
    public static StandardLibrary fromPath(
        Project project,
        String path,
        @Nullable RustcInfo rustcInfo,
        CargoConfig cargoConfig,
        boolean isPartOfCargoProject,
        @Nullable ProcessProgressListener listener
    ) {
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
        if (file == null) return null;
        return fromFile(project, file, rustcInfo, cargoConfig, isPartOfCargoProject, listener);
    }

    @Nullable
    public static StandardLibrary fromPath(
        Project project,
        String path,
        @Nullable RustcInfo rustcInfo
    ) {
        return fromPath(project, path, rustcInfo, CargoConfig.DEFAULT, false, null);
    }

    @Nullable
    public static StandardLibrary fromFile(
        Project project,
        VirtualFile sources,
        @Nullable RustcInfo rustcInfo,
        CargoConfig cargoConfig,
        boolean isPartOfCargoProject,
        @Nullable ProcessProgressListener listener
    ) {
        VirtualFile srcDir = findSrcDir(sources);
        if (srcDir == null) return null;

        StandardLibrary stdlib;
        if (OpenApiUtil.isFeatureEnabled(org.rust.ide.experiments.RsExperiments.FETCH_ACTUAL_STDLIB_METADATA) && !isPartOfCargoProject) {
            org.rust.cargo.toolchain.impl.RustcVersion rustcVersion = (rustcInfo != null) ? rustcInfo.getVersion() : null;
            com.intellij.util.text.SemVer semverVersion = (rustcVersion != null) ? rustcVersion.getSemver() : null;
            if (semverVersion == null) {
                String message = RsBundle.message("toolchain.version.is.unknown.hardcoded.stdlib.structure.will.be.used");
                LOG.warn(message);
                if (listener != null) listener.warning(message, "");
                stdlib = fetchHardcodedStdlib(srcDir);
            } else {
                List<String> buildTargets = cargoConfig.buildTargets().isEmpty()
                    ? (rustcVersion.getHost() != null ? Collections.singletonList(rustcVersion.getHost()) : Collections.emptyList())
                    : cargoConfig.buildTargets();
                String activeToolchain = (rustcInfo != null) ? rustcInfo.getRustupActiveToolchain() : null;
                StandardLibrary result = StdlibDataFetcher.fetchActualStdlib(project, srcDir, rustcVersion, buildTargets, activeToolchain, listener, false);
                if (result == null) {
                    String message = RsBundle.message("fetching.actual.stdlib.info.failed.hardcoded.stdlib.structure.will.be.used");
                    LOG.warn(message);
                    if (listener != null) listener.warning(message, "");
                }
                stdlib = result != null ? result : fetchHardcodedStdlib(srcDir);
            }
        } else {
            stdlib = fetchHardcodedStdlib(srcDir);
        }

        if (stdlib == null) return null;
        return new StandardLibrary(stdlib.getWorkspaceData(), stdlib.isHardcoded(), isPartOfCargoProject);
    }

    @Nullable
    public static StandardLibrary fromFile(
        Project project,
        VirtualFile sources,
        @Nullable RustcInfo rustcInfo
    ) {
        return fromFile(project, sources, rustcInfo, CargoConfig.DEFAULT, false, null);
    }

    @VisibleForTesting
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
    static StandardLibrary fetchHardcodedStdlib(VirtualFile srcDir) {
        Map<String, CargoWorkspaceData.Package> crates = new LinkedHashMap<>();

        for (org.rust.cargo.util.StdLibInfo libInfo : org.rust.cargo.util.AutoInjectedCrates.stdlibCrates) {
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

        for (org.rust.cargo.util.StdLibInfo libInfo : org.rust.cargo.util.AutoInjectedCrates.stdlibCrates) {
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

    static String toStdlibId(String packageId) {
        return "(stdlib) " + packageId;
    }

    @Nullable
    static VirtualFile findFirstFileByRelativePaths(VirtualFile base, List<String> paths) {
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
