package org.rust.cargo.project.workspace;

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
import org.rust.cargo.api.workspace.StandardLibrary;
import org.rust.cargo.api.model.ProcessProgressListener;

/** Builds a {@link StandardLibrary} by asking the toolchain for the stdlib sources. */
public final class StandardLibraryFactory {
    private static final Logger LOG = Logger.getInstance(StandardLibraryFactory.class);

    private StandardLibraryFactory() {
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
        VirtualFile srcDir = StandardLibrary.findSrcDir(sources);
        if (srcDir == null) return null;

        StandardLibrary stdlib;
        if (OpenApiUtil.isFeatureEnabled(org.rust.experiments.RsExperiments.FETCH_ACTUAL_STDLIB_METADATA) && !isPartOfCargoProject) {
            org.rust.cargo.api.toolchain.RustcVersion rustcVersion = (rustcInfo != null) ? rustcInfo.getVersion() : null;
            consulo.util.lang.SemVer semverVersion = (rustcVersion != null) ? rustcVersion.getSemver() : null;
            if (semverVersion == null) {
                String message = RsBundle.message("toolchain.version.is.unknown.hardcoded.stdlib.structure.will.be.used");
                LOG.warn(message);
                if (listener != null) listener.warning(message, "");
                stdlib = StandardLibrary.fetchHardcodedStdlib(srcDir);
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
                stdlib = result != null ? result : StandardLibrary.fetchHardcodedStdlib(srcDir);
            }
        } else {
            stdlib = StandardLibrary.fetchHardcodedStdlib(srcDir);
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
}
