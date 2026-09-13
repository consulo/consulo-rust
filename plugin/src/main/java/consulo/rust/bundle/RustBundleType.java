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
import org.rust.cargo.api.toolchain.RustcVersion;
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

    /** Where the rust-src component puts the standard library, relative to a toolchain root. */
    private static final String STDLIB_SOURCES_RELATIVE = "lib/rustlib/src/rust";

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
        return RsToolchainProvider.getToolchainStatic(binDirectory(Paths.get(homePath)));
    }

    public RustBundleType() {
        super(ID, LocalizeValue.localizeTODO("Rust"), RustIconGroup.rust());
    }

    /**
     * Candidates are toolchain roots - the directory holding both {@code bin} and the
     * {@code lib/rustlib/src/rust} sources - never a directory of executables such as {@code /usr/bin},
     * which the bundle would then have to watch in its entirety.
     */
    @Override
    public void collectHomePaths(@Nonnull Platform platform, @Nonnull Consumer<Path> consumer) {
        if (!Platform.LOCAL.equals(platform.getId())) {
            return;
        }

        Set<Path> paths = new LinkedHashSet<>();
        for (Path toolchainsDir : rustupToolchainsDirs(platform)) {
            try (java.util.stream.Stream<Path> children = Files.list(toolchainsDir)) {
                children.filter(Files::isDirectory).forEach(paths::add);
            }
            catch (java.io.IOException ignored) {
            }
        }

        // Whatever `rustc` is on the path answers with the toolchain it belongs to, which covers both a
        // rustup shim and a toolchain installed outside rustup.
        for (RsToolchainFlavor flavor : RsToolchainFlavor.getApplicableFlavors()) {
            flavor.suggestHomePaths().forEach(binDir -> {
                // Ask only a directory that actually holds a rustc - the flavors offer every PATH entry,
                // and running a program that is not there just fails once per directory.
                if (!hasExecutable(platform, binDir, Rustc.NAME)) return;
                String sysroot = querySysroot(platform, binDir);
                if (sysroot != null) {
                    paths.add(platform.fs().getPath(sysroot));
                }
            });
        }

        paths.forEach(consumer);
    }

    @Nonnull
    private static java.util.List<Path> rustupToolchainsDirs(@Nonnull Platform platform) {
        java.util.List<Path> result = new java.util.ArrayList<>();
        String rustupHome = platform.os().getEnvironmentVariable("RUSTUP_HOME");
        if (rustupHome != null && !rustupHome.isBlank()) {
            result.add(platform.fs().getPath(rustupHome).resolve("toolchains"));
        }
        String userHome = platform.user().homePath().toString();
        if (!userHome.isBlank()) {
            result.add(platform.fs().getPath(userHome).resolve(".rustup").resolve("toolchains"));
        }
        return result;
    }

    @Nonnull
    private static Path binDirectory(@Nonnull Path toolchainRoot) {
        return toolchainRoot.resolve("bin");
    }

    /**
     * Environment variables whose value points at a toolchain root. {@code CARGO_HOME} normally holds
     * something like {@code ~/.cargo}, and {@link #adjustSelectedSdkHome} descends into its {@code bin}
     * directory to reach the executables.
     */
    @Nonnull
    @Override
    public Set<String> getEnvironmentVariables(@Nonnull Platform platform) {
        // No variable names a toolchain root: CARGO_HOME points at the package cache and RUSTUP_HOME at
        // the directory of all toolchains, both of which collectHomePaths expands itself.
        return Set.of();
    }

    @Override
    public boolean canCreatePredefinedSdks(@Nonnull Platform platform) {
        return Platform.LOCAL.equals(platform.getId());
    }

    @Override
    public boolean isValidSdkHome(@Nonnull Platform platform, @Nonnull Path path) {
        if (!Files.isDirectory(path)) return false;
        Path bin = binDirectory(path);
        return hasExecutable(platform, bin, Rustc.NAME)
            && hasExecutable(platform, bin, Cargo.NAME)
            && Files.isDirectory(path.resolve(STDLIB_SOURCES_RELATIVE));
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
        // A directory of executables was chosen - the toolchain it belongs to is its parent.
        Path parent = homePath.getParent();
        if (parent != null && isValidSdkHome(platform, parent)) {
            return parent;
        }
        // Or a directory holding a toolchain, in which case ask it where its own root is.
        String sysroot = querySysroot(platform, homePath);
        if (sysroot != null) {
            Path root = platform.fs().getPath(sysroot);
            if (isValidSdkHome(platform, root)) return root;
        }
        return homePath;
    }

    @Nullable
    @Override
    public String getVersionString(@Nonnull Platform platform, @Nonnull Path path) {
        Path rustcPath = pathToExecutable(platform, binDirectory(path), Rustc.NAME);

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

            // The standard library sources are what the bundle exists to carry. The executables are not
            // rooted: their directory would then be watched and indexed in full, and on a system install
            // that is the whole of /usr/bin.
            Path stdlibSources = home.resolve(STDLIB_SOURCES_RELATIVE);
            Path stdlibCrates = stdlibSources.resolve("library");
            if (Files.isDirectory(stdlibCrates)) {
                stdlibSources = stdlibCrates;
            }
            if (!Files.isDirectory(stdlibSources)) {
                // rust-src is not installed. Roots the bundle already carries are kept rather than
                // dropped, so that adding the component later is all that is needed.
                return;
            }

            String sourcesUrl = VirtualFileUtil.pathToUrl(
                FileUtil.toSystemIndependentName(stdlibSources.toString()));

            if (hasExactly(sdk.getRootProvider(), sourcesUrl)) {
                return;
            }

            SdkModificator modificator = sdk.getSdkModificator();
            modificator.removeRoots(SourcesOrderRootType.ID);
            modificator.removeRoots(BinariesOrderRootType.ID);
            // The url form is used rather than the VirtualFile one so that a directory does not have to
            // be in the virtual file system yet.
            modificator.addRoot(sourcesUrl, SourcesOrderRootType.ID);
            modificator.commitChanges();
        }
        catch (Exception e) {
            LOG.warn("Failed to set up the roots of Rust bundle " + sdk.getName(), e);
        }
    }

    /**
     * Whether {@code rootProvider} already carries {@code url}, and nothing else, under both root types.
     */
    private static boolean hasExactly(@Nonnull RootProvider rootProvider, @Nonnull String sourcesUrl) {
        return Arrays.equals(rootProvider.getUrls(SourcesOrderRootType.ID), new String[]{sourcesUrl})
            && rootProvider.getUrls(BinariesOrderRootType.ID).length == 0;
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
