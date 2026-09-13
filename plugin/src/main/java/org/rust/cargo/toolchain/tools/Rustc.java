/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.tools;

import consulo.process.event.ProcessListener;
import consulo.process.util.ProcessOutput;
import consulo.disposer.Disposable;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.CfgOptions;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.api.toolchain.RustcVersion;
import org.rust.openapiext.CommandLineExt;
import org.rust.openapiext.OpenApiUtil;
import org.rust.openapiext.RsProcessExecutionException;
import org.rust.stdext.RsResult;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Rustc extends RustupComponent {

    public static final String NAME = "rustc";

    /**
     * Location of the standard library sources inside a sysroot. The directory only exists when the
     * {@code rust-src} component is installed.
     */
    public static final String STDLIB_SOURCES_IN_SYSROOT = "lib/rustlib/src/rust";

    public Rustc(RsToolchainBase toolchain) {
        super(NAME, toolchain);
    }

    @Nullable
    public RustcVersion queryVersion(@Nullable Path workingDirectory) {
        if (!OpenApiUtil.isUnitTestMode()) {
            OpenApiUtil.checkIsBackgroundThread();
        }
        ProcessOutput output = CommandLineExt.execute(
            createBaseCommandLine(
                new String[]{"--version", "--verbose"},
                workingDirectory,
                Collections.emptyMap()
            ),
            (Integer) getToolchain().getExecutionTimeoutInMilliseconds()
        );
        if (output == null) return null;
        return RustcVersion.parseRustcVersion(output.getStdoutLines());
    }

    @Nullable
    public RustcVersion queryVersion() {
        return queryVersion(null);
    }

    public RsResult<RustcVersion, RsProcessExecutionException> queryVersion(
        Path workingDirectory,
        Disposable owner,
        ProcessListener listener
    ) {
        if (!OpenApiUtil.isUnitTestMode()) {
            OpenApiUtil.checkIsBackgroundThread();
        }
        return CommandLineExt.execute(
            createBaseCommandLine(
                new String[]{"--version", "--verbose"},
                workingDirectory,
                Collections.emptyMap()
            ),
            owner,
            null,
            listener
        ).map(output -> RustcVersion.parseRustcVersion(output.getStdoutLines()));
    }

    @Nullable
    public String getSysroot(Path projectDirectory) {
        if (!OpenApiUtil.isUnitTestMode()) {
            OpenApiUtil.checkIsBackgroundThread();
        }
        int timeoutMs = 10000;
        ProcessOutput output = CommandLineExt.execute(
            createBaseCommandLine(
                new String[]{"--print", "sysroot"},
                projectDirectory,
                Collections.emptyMap()
            ),
            (Integer) timeoutMs
        );

        if (output == null || !CommandLineExt.isSuccess(output)) return null;
        return getToolchain().toLocalPath(output.getStdout().trim());
    }

    @Nullable
    public String getStdlibPathFromSysroot(Path projectDirectory) {
        String sysroot = getSysroot(projectDirectory);
        if (sysroot == null) return null;
        return stdlibPathFromSysroot(sysroot);
    }

    /**
     * Path of the standard library sources belonging to {@code sysroot}, in system independent form:
     * {@code /} separators and no trailing one. That is the shape the virtual file system and bundle
     * roots are keyed by, while {@code rustc} prints the sysroot with the native separator, so the
     * conversion happens here rather than at every call site.
     */
    public static String stdlibPathFromSysroot(String sysroot) {
        String root = StringUtil.trimTrailing(FileUtil.toSystemIndependentName(sysroot), '/');
        return root + "/" + STDLIB_SOURCES_IN_SYSROOT;
    }

    @Nullable
    public VirtualFile getStdlibFromSysroot(Path projectDirectory) {
        String stdlibPath = getStdlibPathFromSysroot(projectDirectory);
        if (stdlibPath == null) return null;
        LocalFileSystem fs = LocalFileSystem.getInstance();
        return fs.refreshAndFindFileByPath(stdlibPath);
    }

    @Nullable
    private List<String> getRawCfgOption(@Nullable Path projectDirectory) {
        int timeoutMs = 10000;
        ProcessOutput output = CommandLineExt.execute(
            createBaseCommandLine(
                new String[]{"--print", "cfg"},
                projectDirectory,
                Map.of(RsToolchainBase.RUSTC_BOOTSTRAP, "1")
            ),
            (Integer) timeoutMs
        );
        return (output != null && CommandLineExt.isSuccess(output)) ? output.getStdoutLines() : null;
    }

    public CfgOptions getCfgOptions(@Nullable Path projectDirectory) {
        List<String> rawCfgOptions = getRawCfgOption(projectDirectory);
        if (rawCfgOptions == null) rawCfgOptions = Collections.emptyList();
        return CfgOptions.parse(rawCfgOptions);
    }

    @Nullable
    public List<String> getTargets(@Nullable Path projectDirectory) {
        if (!OpenApiUtil.isUnitTestMode()) {
            OpenApiUtil.checkIsBackgroundThread();
        }
        int timeoutMs = 10000;
        ProcessOutput output = CommandLineExt.execute(
            createBaseCommandLine(
                new String[]{"--print", "target-list"},
                projectDirectory,
                Collections.emptyMap()
            ),
            (Integer) timeoutMs
        );
        if (output != null && CommandLineExt.isSuccess(output)) {
            return List.of(output.getStdout().trim().split("\n"));
        }
        return null;
    }

    public static Rustc create(RsToolchainBase toolchain) {
        return new Rustc(toolchain);
    }
}
