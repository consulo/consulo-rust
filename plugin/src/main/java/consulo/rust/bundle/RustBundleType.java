/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.bundle;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.content.RootProvider;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.bundle.PlatformAwareSdkType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModificator;
import consulo.content.bundle.SdkType;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.util.CapturingProcessUtil;
import consulo.process.util.ProcessOutput;
import consulo.rust.icon.RustIconGroup;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.RsToolchainProvider;
import org.rust.cargo.toolchain.flavors.RsToolchainFlavor;
import org.rust.cargo.toolchain.impl.RustcVersion;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.cargo.toolchain.tools.Rustc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Bundle type describing a Rust toolchain. Its home directory is the directory which directly
 * contains the {@code rustc} and {@code cargo} executables - for example {@code ~/.cargo/bin}
 * or {@code /usr/bin}.
 */
@ExtensionImpl
public class RustBundleType extends PlatformAwareSdkType {
    private static final Logger LOG = Logger.getInstance(RustBundleType.class);

    private static final String ID = "RUST";

    private static final Set<String> ALLOWED_ROOT_TYPE_IDS = Set.of(BinariesOrderRootType.ID, SourcesOrderRootType.ID);

    private static final int SYSROOT_TIMEOUT_MS = 10_000;

    @Nonnull
    public static RustBundleType getInstance() {
        return Application.get().getExtensionPoint(SdkType.class).findExtensionOrFail(RustBundleType.class);
    }

    /**
     * Builds a toolchain out of the home directory of {@code sdk}, or returns {@code null} when
     * {@code sdk} is not a Rust bundle or its home directory does not hold a usable toolchain.
     */
    @Nullable
    public static RsToolchainBase toToolchain(@Nullable Sdk sdk) {
        if (sdk == null || !(sdk.getSdkType() instanceof RustBundleType)) {
            return null;
        }
        String homePath = sdk.getHomePath();
        if (homePath == null || homePath.isEmpty()) {
            return null;
        }
        return RsToolchainProvider.getToolchainStatic(Paths.get(homePath));
    }

    public RustBundleType() {
        super(ID, LocalizeValue.localizeTODO("Rust"), RustIconGroup.rust());
    }

    @Override
    public void collectHomePaths(@Nonnull Platform platform, @Nonnull Consumer<Path> consumer) {
        if (!Platform.LOCAL.equals(platform.getId())) {
            return;
        }

        Set<Path> paths = new LinkedHashSet<>();
        for (RsToolchainFlavor flavor : RsToolchainFlavor.getApplicableFlavors()) {
            flavor.suggestHomePaths().forEach(paths::add);
        }
        paths.forEach(consumer);
    }

    /**
     * Environment variables whose value points at a toolchain root. {@code CARGO_HOME} normally holds
     * something like {@code ~/.cargo}, and {@link #adjustSelectedSdkHome} descends into its {@code bin}
     * directory to reach the executables.
     */
    @Nonnull
    @Override
    public Set<String> getEnvironmentVariables(@Nonnull Platform platform) {
        return Set.of("CARGO_HOME");
    }

    @Override
    public boolean canCreatePredefinedSdks(@Nonnull Platform platform) {
        return Platform.LOCAL.equals(platform.getId());
    }

    @Override
    public boolean isValidSdkHome(@Nonnull Platform platform, @Nonnull Path path) {
        return Files.isDirectory(path)
            && hasExecutable(platform, path, Rustc.NAME)
            && hasExecutable(platform, path, Cargo.NAME);
    }

    /**
     * Accepts a toolchain root such as {@code ~/.cargo} by descending into its {@code bin} directory.
     */
    @Nonnull
    @Override
    public Path adjustSelectedSdkHome(@Nonnull Platform platform, @Nonnull Path homePath) {
        if (isValidSdkHome(platform, homePath)) {
            return homePath;
        }
        Path binPath = homePath.resolve("bin");
        return isValidSdkHome(platform, binPath) ? binPath : homePath;
    }

    @Nullable
    @Override
    public String getVersionString(@Nonnull Platform platform, @Nonnull Path path) {
        Path rustcPath = pathToExecutable(platform, path, Rustc.NAME);

        try {
            GeneralCommandLine commandLine = new GeneralCommandLine();
            commandLine.withExecutablePath(rustcPath);
            commandLine.withWorkingDirectory(path);
            commandLine.addParameters("--version", "--verbose");
            commandLine.withPlatform(platform);

            ProcessOutput processOutput = CapturingProcessUtil.execAndGetOutput(commandLine);

            RustcVersion version = RustcVersion.parseRustcVersion(processOutput.getStdoutLines());
            if (version != null) {
                return version.getSemver().getRawVersion();
            }

            List<String> stdoutLines = processOutput.getStdoutLines();
            return stdoutLines.isEmpty() ? null : stdoutLines.get(0).trim();
        }
        catch (ExecutionException e) {
            LOG.warn("Failed to execute " + rustcPath, e);
            return null;
        }
    }

    /**
     * Attaches the standard library sources of the toolchain to the bundle, so that every module bound
     * to it indexes, resolves and navigates into the standard library without any per-project setup.
     * <p>
     * The sources live under the sysroot the toolchain reports and are only present once the
     * {@code rust-src} component has been installed; a toolchain without it is left without roots and
     * picks them up the next time the bundle is set up.
     * <p>
     * The sysroot is queried without a project directory, so a directory override or a
     * {@code rust-toolchain.toml} of some project cannot redirect the bundle at a different toolchain.
     */
    @Override
    public void setupSdkPaths(@Nonnull Sdk sdk) {
        try {
            String homePath = sdk.getHomePath();
            if (homePath == null || homePath.isEmpty()) {
                return;
            }

            Platform platform = sdk.getPlatform();
            Path home = platform.fs().getPath(homePath);

            String sysroot = querySysroot(platform, home);
            if (sysroot == null) {
                // The toolchain could not be asked - a timeout, a cancellation, or a home directory
                // that no longer holds one. Roots the bundle already carries are kept rather than
                // dropped on a transient failure; the next setup replaces them once an answer arrives.
                return;
            }

            String stdlibPath = stdlibSourcePath(platform, sysroot);
            String url = null;
            if (stdlibPath != null) {
                // The path is converted to system independent form before the url is built: pathToUrl
                // does not touch what it is handed, while a bundle stores its roots normalized, so a
                // url carrying native separators could never compare equal to a stored root and the
                // whole root set would be rewritten on every setup.
                url = VirtualFileUtil.pathToUrl(FileUtil.toSystemIndependentName(stdlibPath));
            }
            if (hasExactly(sdk.getRootProvider(), url)) {
                return;
            }

            SdkModificator modificator = sdk.getSdkModificator();
            modificator.removeRoots(SourcesOrderRootType.ID);
            modificator.removeRoots(BinariesOrderRootType.ID);
            if (url != null) {
                // The url form is used rather than the VirtualFile one so that the directory does not
                // have to be in the virtual file system yet. The same directory is registered under both
                // root types: the source root is what gets indexed, while the search scope a module
                // builds out of its order entries is assembled from binaries roots only.
                modificator.addRoot(url, SourcesOrderRootType.ID);
                modificator.addRoot(url, BinariesOrderRootType.ID);
            }
            modificator.commitChanges();
        }
        catch (Exception e) {
            LOG.warn("Failed to set up the roots of Rust bundle " + sdk.getName(), e);
        }
    }

    /**
     * Whether {@code rootProvider} already carries {@code url}, and nothing else, under both root types.
     */
    private static boolean hasExactly(@Nonnull RootProvider rootProvider, @Nullable String url) {
        String[] expected = url == null ? ArrayUtil.EMPTY_STRING_ARRAY : new String[]{url};
        return Arrays.equals(rootProvider.getUrls(SourcesOrderRootType.ID), expected)
            && Arrays.equals(rootProvider.getUrls(BinariesOrderRootType.ID), expected);
    }

    /**
     * Asks the toolchain in {@code home} where its sysroot is, or returns {@code null} when it cannot
     * be asked - which is deliberately distinct from a toolchain that answers but carries no standard
     * library sources, so that a failed query does not read as "this toolchain has none".
     * <p>
     * The command line is built here rather than through {@link Rustc} because bundle setup also runs
     * on the UI thread, which the toolchain tools refuse to run on, and because only this form can
     * address a toolchain living on a non-local platform.
     */
    @Nullable
    private static String querySysroot(@Nonnull Platform platform, @Nonnull Path home) {
        Path rustcPath = pathToExecutable(platform, home, Rustc.NAME);

        try {
            GeneralCommandLine commandLine = new GeneralCommandLine();
            commandLine.withExecutablePath(rustcPath);
            commandLine.withWorkingDirectory(home);
            commandLine.addParameters("--print", "sysroot");
            commandLine.withPlatform(platform);

            ProcessOutput processOutput = CapturingProcessUtil.execAndGetOutput(commandLine, SYSROOT_TIMEOUT_MS);
            if (processOutput.isTimeout() || processOutput.isCancelled()
                || !processOutput.isExitCodeSet() || processOutput.getExitCode() != 0) {
                return null;
            }

            String sysroot = processOutput.getStdout().trim();
            return sysroot.isEmpty() ? null : sysroot;
        }
        catch (ExecutionException e) {
            LOG.warn("Failed to execute " + rustcPath, e);
            return null;
        }
    }

    /**
     * Standard library source directory inside {@code sysroot}, or {@code null} when the
     * {@code rust-src} component is not installed and the directory therefore does not exist.
     */
    @Nullable
    private static String stdlibSourcePath(@Nonnull Platform platform, @Nonnull String sysroot) {
        String stdlibPath = Rustc.stdlibPathFromSysroot(sysroot);
        return Files.isDirectory(platform.fs().getPath(stdlibPath)) ? stdlibPath : null;
    }

    @Override
    public boolean isRootTypeApplicable(@Nonnull String type) {
        return ALLOWED_ROOT_TYPE_IDS.contains(type);
    }

    @Nonnull
    private static Path pathToExecutable(@Nonnull Platform platform, @Nonnull Path home, @Nonnull String toolName) {
        String exeName = platform.os().isWindows() ? toolName + ".exe" : toolName;
        return home.resolve(exeName).toAbsolutePath();
    }

    private static boolean hasExecutable(@Nonnull Platform platform, @Nonnull Path home, @Nonnull String toolName) {
        return Files.isExecutable(pathToExecutable(platform, home, toolName));
    }
}
