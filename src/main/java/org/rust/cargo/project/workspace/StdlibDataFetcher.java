/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.model.ProcessProgressListener;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.impl.CargoMetadata;
import org.rust.cargo.toolchain.impl.CargoMetadataException;
import org.rust.cargo.toolchain.impl.RustcVersion;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.cargo.toolchain.tools.CargoExtUtil;
import org.rust.cargo.util.AutoInjectedCrates;
import org.rust.cargo.util.StdLibType;
import org.rust.cargo.util.ToolchainUtil;
import org.rust.openapiext.RsPathManager;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.HashCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class StdlibDataFetcher {

    private static final Logger LOG = Logger.getInstance(StdlibDataFetcher.class);
    private static final com.intellij.util.text.SemVer RUSTC_1_72_BETA = ToolchainUtil.parseSemVer("1.72.0-beta");

    private final Project myProject;
    private final Cargo myCargo;
    private final VirtualFile mySrcDir;
    private final RustcVersion myVersion;
    private final VirtualFile myTestPackageSrcDir;
    private final Path myStdlibDependenciesDir;
    private final List<String> myBuildTargets;
    @Nullable
    private final String myActiveToolchain;
    @Nullable
    private final ProcessProgressListener myListener;

    private final List<String> myWorkspaceMembers = new ArrayList<>();
    private final Set<String> myVisitedPackages = new HashSet<>();
    private final List<CargoMetadata.Package> myAllPackages = new ArrayList<>();
    private final List<CargoMetadata.ResolveNode> myAllNodes = new ArrayList<>();

    private StdlibDataFetcher(
        Project project,
        Cargo cargo,
        VirtualFile srcDir,
        RustcVersion version,
        VirtualFile testPackageSrcDir,
        Path stdlibDependenciesDir,
        List<String> buildTargets,
        @Nullable String activeToolchain,
        @Nullable ProcessProgressListener listener
    ) {
        myProject = project;
        myCargo = cargo;
        mySrcDir = srcDir;
        myVersion = version;
        myTestPackageSrcDir = testPackageSrcDir;
        myStdlibDependenciesDir = stdlibDependenciesDir;
        myBuildTargets = buildTargets;
        myActiveToolchain = activeToolchain;
        myListener = listener;
    }

    public StandardLibrary fetchStdlibData() {
        // `test` package depends on all other stdlib packages from AutoInjectedCrates (at least at moment of writing)
        // so let's collect its metadata first to avoid redundant calls of `cargo metadata`
        collectPackageMetadata(myTestPackageSrcDir);
        // if there is a package that is not in dependencies of `test` package,
        // collect its metadata manually
        for (org.rust.cargo.util.StdLibInfo libInfo : AutoInjectedCrates.stdlibCrates) {
            if (libInfo.type() == StdLibType.DEPENDENCY) continue;
            List<String> packageSrcPaths = List.of(libInfo.name(), "lib" + libInfo.name());
            VirtualFile packageSrcDir = StandardLibrary.findFirstFileByRelativePaths(mySrcDir, packageSrcPaths);
            if (packageSrcDir != null) packageSrcDir = packageSrcDir.getCanonicalFile();
            if (packageSrcDir == null) continue;

            String packageManifestPath = Paths.get(packageSrcDir.getPath()).resolve(CargoConstants.MANIFEST_FILE).toString();
            CargoMetadata.Package pkg = null;
            for (CargoMetadata.Package p : myAllPackages) {
                if (p.getManifest_path().equals(packageManifestPath)) {
                    pkg = p;
                    break;
                }
            }
            if (pkg == null) {
                collectPackageMetadata(packageSrcDir);
            } else {
                myWorkspaceMembers.add(pkg.getId());
            }
        }

        CargoMetadata.Project stdlibMetadataProject = new CargoMetadata.Project(
            myAllPackages,
            new CargoMetadata.Resolve(myAllNodes),
            1,
            myWorkspaceMembers,
            mySrcDir.getPath()
        );
        CargoWorkspaceData stdlibWorkspaceData = CargoMetadata.clean(stdlibMetadataProject);
        List<CargoWorkspaceData.Package> stdlibPackages = new ArrayList<>();
        for (CargoWorkspaceData.Package pkg : stdlibWorkspaceData.getPackages()) {
            PackageOrigin newOrigin = pkg.getSource() == null ? PackageOrigin.STDLIB : PackageOrigin.STDLIB_DEPENDENCY;
            stdlibPackages.add(pkg.copyWithOrigin(newOrigin));
        }
        return new StandardLibrary(stdlibWorkspaceData.copyWithPackages(stdlibPackages), false);
    }

    private String remapPath(String pathStr, String libName, String version) {
        Path path = org.rust.stdext.PathUtil.toPath(pathStr);
        for (int i = path.getNameCount() - 1; i >= 0; i--) {
            String fileName = path.getName(i).getFileName().toString();
            if (fileName.startsWith(libName) && fileName.endsWith(version)) {
                Path subpath = path.subpath(i + 1, path.getNameCount());
                return myStdlibDependenciesDir.resolve(libName).resolve(subpath).toString();
            }
        }
        throw new IllegalStateException("Failed to remap `" + pathStr + "`");
    }

    private void walk(CargoMetadata.Project project, String id, boolean root) {
        if (myVisitedPackages.contains(id)) return;
        String stdlibId = StandardLibrary.toStdlibId(id);

        if (root) {
            myWorkspaceMembers.add(stdlibId);
        }

        myVisitedPackages.add(id);

        CargoMetadata.Package pkg = null;
        for (CargoMetadata.Package p : project.getPackages()) {
            if (p.getId().equals(id)) { pkg = p; break; }
        }
        if (pkg == null) return;

        CargoMetadata.ResolveNode pkgNode = null;
        for (CargoMetadata.ResolveNode n : project.getResolve().getNodes()) {
            if (n.getId().equals(id)) { pkgNode = n; break; }
        }
        if (pkgNode == null) return;

        List<CargoMetadata.Dep> nodeDeps = new ArrayList<>();
        List<String> nodeDependencies = new ArrayList<>();

        if (pkgNode.getDeps() != null) {
            for (CargoMetadata.Dep dep : pkgNode.getDeps()) {
                List<CargoMetadata.DepKindInfo> depKinds = new ArrayList<>();
                if (dep.getDep_kinds() != null) {
                    for (CargoMetadata.DepKindInfo dk : dep.getDep_kinds()) {
                        if (dk.getKind() == null) depKinds.add(dk);
                    }
                }
                if (!depKinds.isEmpty()) {
                    nodeDependencies.add(StandardLibrary.toStdlibId(dep.getPkg()));
                    nodeDeps.add(dep.copy(StandardLibrary.toStdlibId(dep.getPkg()), depKinds));
                    walk(project, dep.getPkg(), false);
                }
            }
        }

        CargoMetadata.ResolveNode newNode = pkgNode.copy(stdlibId, nodeDependencies, nodeDeps);
        myAllNodes.add(newNode);

        String newManifestPath;
        List<CargoMetadata.Target> newTargets;
        if (pkg.getSource() != null) {
            newTargets = new ArrayList<>();
            for (CargoMetadata.Target t : pkg.getTargets()) {
                newTargets.add(t.copy(remapPath(t.getSrc_path(), pkg.getName(), pkg.getVersion())));
            }
            newManifestPath = remapPath(pkg.getManifest_path(), pkg.getName(), pkg.getVersion());
        } else {
            newManifestPath = pkg.getManifest_path();
            newTargets = pkg.getTargets();
        }

        List<CargoMetadata.RawDependency> filteredDeps = new ArrayList<>();
        for (CargoMetadata.RawDependency d : pkg.getDependencies()) {
            if (d.getKind() == null) filteredDeps.add(d);
        }

        CargoMetadata.Package newPkg = pkg.copy(stdlibId, newManifestPath, newTargets, filteredDeps);
        myAllPackages.add(newPkg);
    }

    private void collectPackageMetadata(VirtualFile dir) {
        VirtualFile manifest = dir.findChild(CargoConstants.MANIFEST_FILE);
        // Don't try to get metadata without Cargo.toml, it will fail anyway
        if (manifest == null) {
            LOG.warn("There isn't `" + CargoConstants.MANIFEST_FILE + "` in `" + dir.getPath() + "` directory");
            return;
        }

        Path dirPath = Paths.get(dir.getPath());
        org.rust.stdext.RsResult<CargoMetadata.Project, ?> result = myCargo.fetchMetadata(
            myProject,
            dirPath,
            myBuildTargets,
            myActiveToolchain,
            additionalEnvVariables(myVersion),
            myListener
        );
        if (result instanceof org.rust.stdext.RsResult.Err) {
            org.rust.stdext.RsResult.Err<?, ?> err = (org.rust.stdext.RsResult.Err<?, ?>) result;
            if (myListener != null) {
                myListener.error(
                    RsBundle.message("build.event.title.failed.to.fetch.stdlib.package.info"),
                    err.err() != null ? err.err().toString() : ""
                );
            }
            throw new RuntimeException((Throwable) err.err());
        }

        CargoMetadata.Project metadataProject = ((org.rust.stdext.RsResult.Ok<CargoMetadata.Project, ?>) result).ok();
        String rootPackageId = metadataProject.getWorkspace_members().get(0);
        walk(metadataProject, rootPackageId, true);
    }

    @Nullable
    public static StdlibDataFetcher create(
        Project project,
        VirtualFile srcDir,
        RustcVersion version,
        List<String> buildTargets,
        @Nullable String activeToolchain,
        @Nullable ProcessProgressListener listener,
        boolean cleanVendorDir
    ) {
        RsToolchainBase toolchain = RsProjectSettingsServiceUtil.getToolchain(project);
        if (toolchain == null) return null;
        Cargo cargo = CargoExtUtil.cargo(toolchain);

        List<String> testPackageSrcPaths = List.of(AutoInjectedCrates.TEST, "lib" + AutoInjectedCrates.TEST);
        VirtualFile testPackageSrcDir = StandardLibrary.findFirstFileByRelativePaths(srcDir, testPackageSrcPaths);
        if (testPackageSrcDir != null) testPackageSrcDir = testPackageSrcDir.getCanonicalFile();
        if (testPackageSrcDir == null) return null;

        Path stdlibDependenciesDir = findStdlibDependencyDirectory(
            project, cargo, srcDir, testPackageSrcDir, version, activeToolchain, listener, cleanVendorDir
        );
        if (stdlibDependenciesDir == null) return null;

        return new StdlibDataFetcher(
            project, cargo, srcDir, version, testPackageSrcDir,
            stdlibDependenciesDir, buildTargets, activeToolchain, listener
        );
    }

    @Nullable
    static StandardLibrary fetchActualStdlib(
        Project project,
        VirtualFile srcDir,
        RustcVersion version,
        List<String> buildTargets,
        @Nullable String activeToolchain,
        @Nullable ProcessProgressListener listener,
        boolean cleanVendorDir
    ) {
        try {
            StdlibDataFetcher fetcher = create(project, srcDir, version, buildTargets, activeToolchain, listener, cleanVendorDir);
            if (fetcher == null) return null;
            return fetcher.fetchStdlibData();
        } catch (Throwable e) {
            if (OpenApiUtil.isUnitTestMode()) {
                // Don't fail a test - we have some tests that check error recovery during stdlib fetching
                LOG.warn(e);
            } else {
                LOG.error(e);
            }
            if (!cleanVendorDir && e instanceof CargoMetadataException) {
                return fetchActualStdlib(project, srcDir, version, buildTargets, activeToolchain, listener, true);
            }
        }
        return null;
    }

    @VisibleForTesting
    public static Path stdlibVendorDir(VirtualFile srcDir, RustcVersion version) {
        String stdlibHash = stdlibHash(srcDir, version);
        return RsPathManager.stdlibDependenciesDir().resolve(version.getSemver().getParsedVersion() + "-" + stdlibHash + "/vendor");
    }

    @Nullable
    private static Path findStdlibDependencyDirectory(
        Project project,
        Cargo cargo,
        VirtualFile srcDir,
        VirtualFile testPackageSrcDir,
        RustcVersion version,
        @Nullable String activeToolchain,
        @Nullable ProcessProgressListener listener,
        boolean cleanVendorDir
    ) {
        Path stdlibVendor = stdlibVendorDir(srcDir, version);
        boolean stdlibVendorExists = Files.exists(stdlibVendor);
        if (stdlibVendorExists && cleanVendorDir) {
            try {
                com.intellij.openapi.util.io.FileUtil.delete(stdlibVendor);
            } catch (IOException e) {
                LOG.error(e);
                return null;
            }
            stdlibVendorExists = false;
        }

        if (!stdlibVendorExists) {
            // `test` package depends on all other stdlib packages,
            // so it's enough to vendor only its dependencies
            Path testPath = Paths.get(testPackageSrcDir.getPath());
            org.rust.stdext.RsResult<?, ?> result = cargo.vendorDependencies(
                project, testPath, stdlibVendor, activeToolchain,
                additionalEnvVariables(version), listener
            );
            if (result instanceof org.rust.stdext.RsResult.Err) {
                org.rust.stdext.RsResult.Err<?, ?> err = (org.rust.stdext.RsResult.Err<?, ?>) result;
                if (listener != null) {
                    listener.error(
                        RsBundle.message("build.event.title.failed.to.load.stdlib.dependencies"),
                        err.err() != null ? err.err().toString() : ""
                    );
                }
                LOG.error((Throwable) err.err());
                return null;
            }
        }
        return stdlibVendor;
    }

    private static String stdlibHash(VirtualFile srcDir, RustcVersion version) {
        HashCode pathHash = HashCode.compute(srcDir.getPath());
        String versionStr = version.getCommitHash() != null ? version.getCommitHash() : version.getSemver().getParsedVersion();
        HashCode versionHash = HashCode.compute(versionStr);
        return HashCode.mix(pathHash, versionHash).toString();
    }

    private static EnvironmentVariablesData additionalEnvVariables(RustcVersion version) {
        // Starting from 1.72.0-beta.1 there is a nightly Cargo feature usage in the stdlib:
        // `cargo-features = ["public-dependency"]`
        boolean addRustcBootstrap = version.getSemver().compareTo(RUSTC_1_72_BETA) >= 0;
        if (addRustcBootstrap) {
            return EnvironmentVariablesData.create(
                Collections.singletonMap(RsToolchainBase.RUSTC_BOOTSTRAP, "1"),
                true
            );
        } else {
            return EnvironmentVariablesData.DEFAULT;
        }
    }
}
