/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.tools;

import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.CfgOptions;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.impl.RustcVersion;
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
        return FileUtil.join(sysroot, "lib/rustlib/src/rust");
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
