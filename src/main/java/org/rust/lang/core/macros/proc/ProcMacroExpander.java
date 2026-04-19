/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.proc;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.settings.RustProjectSettingsServiceUtil;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.util.ToolchainUtil;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.macros.*;
import org.rust.lang.core.macros.errors.ProcMacroExpansionError;
import org.rust.lang.core.macros.tt.*;
import org.rust.lang.core.parser.ParserUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwner;
// import removed - placeholder
import org.rust.openapiext.RsPathManager;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.RsResult;
import com.intellij.util.text.SemVer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Procedural macro expander.
 */
public class ProcMacroExpander extends MacroExpander<RsProcMacroData, ProcMacroExpansionError> {

    public static final int EXPANDER_VERSION = 11;
    private static final SemVer MIN_RUSTC_VERSION_WITH_EXPANDER_VERSION_CHECK =
        ToolchainUtil.parseSemVer("1.70.0");

    @NotNull
    private final Project myProject;
    @Nullable
    private final RsToolchainBase myToolchain;
    @Nullable
    private final ProcMacroServerPool myServer;
    private final long myTimeout;
    private final boolean myIsEnabled;

    private ProcMacroExpander(
        @NotNull Project project,
        @Nullable RsToolchainBase toolchain,
        @Nullable ProcMacroServerPool server,
        long timeout
    ) {
        myProject = project;
        myToolchain = toolchain;
        myServer = server;
        myTimeout = timeout;
        myIsEnabled = server != null || ProcMacroApplicationService.isAnyEnabled();
    }

    private ProcMacroExpander(
        @NotNull Project project,
        @Nullable RsToolchainBase toolchain,
        @Nullable ProcMacroServerPool server
    ) {
        this(project, toolchain, server, Registry.get("org.rust.macros.proc.timeout").asInteger());
    }

    @NotNull
    private RsResult<ProcMacroServerPool, ProcMacroExpansionError> serverOrErr() {
        if (myServer != null) {
            return new RsResult.Ok<>(myServer);
        }
        if (myIsEnabled) {
            return new RsResult.Err<>(ProcMacroExpansionError.ExecutableNotFound);
        }
        return new RsResult.Err<>(ProcMacroExpansionError.ProcMacroExpansionIsDisabled);
    }

    @NotNull
    @Override
    public RsResult<Pair<CharSequence, RangeMap>, ProcMacroExpansionError> expandMacroAsTextWithErr(
        @NotNull RsProcMacroData def,
        @NotNull RsMacroCallData call
    ) {
        RsResult<ProcMacroServerPool, ProcMacroExpansionError> serverResult = serverOrErr();
        if (serverResult instanceof RsResult.Err) {
            @SuppressWarnings("unchecked")
            RsResult.Err<Pair<CharSequence, RangeMap>, ProcMacroExpansionError> err =
                new RsResult.Err<>(((RsResult.Err<ProcMacroServerPool, ProcMacroExpansionError>) serverResult).getErr());
            return err;
        }
        ProcMacroServerPool server = ((RsResult.Ok<ProcMacroServerPool, ProcMacroExpansionError>) serverResult).getOk();

        MacroCallBody macroBody = call.getMacroBody();
        if (macroBody instanceof MacroCallBody.FunctionLike && !ProcMacroApplicationService.isFunctionLikeEnabled()) {
            return new RsResult.Err<>(ProcMacroExpansionError.ProcMacroExpansionIsDisabled);
        }

        // The full implementation involves:
        // 1. Extracting macro body text and attribute text
        // 2. Lowering doc comments
        // 3. Parsing into subtrees
        // 4. Sending to the proc macro expander process
        // 5. Converting the response back to text with range maps
        //
        // This is a structural placeholder.
        return new RsResult.Err<>(ProcMacroExpansionError.ExecutableNotFound);
    }

    @NotNull
    public RsResult<TokenTree.Subtree, ProcMacroExpansionError> expandMacroAsTtWithErr(
        @NotNull TokenTree.Subtree macroCallBody,
        @Nullable TokenTree.Subtree attributes,
        @NotNull String macroName,
        @NotNull String lib,
        @NotNull Map<String, String> env
    ) {
        RsResult<ProcMacroServerPool, ProcMacroExpansionError> serverResult = serverOrErr();
        if (serverResult instanceof RsResult.Err) {
            @SuppressWarnings("unchecked")
            RsResult.Err<TokenTree.Subtree, ProcMacroExpansionError> err =
                new RsResult.Err<>(((RsResult.Err<ProcMacroServerPool, ProcMacroExpansionError>) serverResult).getErr());
            return err;
        }
        ProcMacroServerPool server = ((RsResult.Ok<ProcMacroServerPool, ProcMacroExpansionError>) serverResult).getOk();
        return expandMacroAsTtWithErrInternal(server, macroCallBody, attributes, macroName, lib, env);
    }

    @NotNull
    private RsResult<TokenTree.Subtree, ProcMacroExpansionError> expandMacroAsTtWithErrInternal(
        @NotNull ProcMacroServerPool server,
        @NotNull TokenTree.Subtree macroCallBody,
        @Nullable TokenTree.Subtree attributes,
        @NotNull String macroName,
        @NotNull String lib,
        @NotNull Map<String, String> env
    ) {
        String remoteLib = myToolchain != null ? myToolchain.toRemotePath(lib) : lib;
        Map<String, String> envMapped = new HashMap<>();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            envMapped.put(entry.getKey(), myToolchain != null ? myToolchain.toRemotePath(entry.getValue()) : entry.getValue());
        }

        RsResult<ProMacroExpanderVersion, RequestSendError> versionResult = server.requestExpanderVersion();
        if (versionResult instanceof RsResult.Err) {
            return new RsResult.Err<>(toProcMacroExpansionError(((RsResult.Err<ProMacroExpanderVersion, RequestSendError>) versionResult).getErr()));
        }
        ProMacroExpanderVersion version = ((RsResult.Ok<ProMacroExpanderVersion, RequestSendError>) versionResult).getOk();

        List<List<String>> envList = envMapped.entrySet().stream()
            .map(e -> Arrays.asList(e.getKey(), e.getValue()))
            .collect(Collectors.toList());

        Request request = new Request.ExpandMacro(
            FlatTree.fromSubtree(macroCallBody, version),
            macroName,
            attributes != null ? FlatTree.fromSubtree(attributes, version) : null,
            remoteLib,
            envList,
            envMapped.get("CARGO_MANIFEST_DIR")
        );

        RsResult<Response, RequestSendError> responseResult = server.send(request, myTimeout);
        if (responseResult instanceof RsResult.Err) {
            return new RsResult.Err<>(toProcMacroExpansionError(((RsResult.Err<Response, RequestSendError>) responseResult).getErr()));
        }
        Response response = ((RsResult.Ok<Response, RequestSendError>) responseResult).getOk();
        if (!(response instanceof Response.ExpandMacro)) {
            throw new IllegalStateException("Expected ExpandMacro response");
        }
        Response.ExpandMacro expandMacro = (Response.ExpandMacro) response;
        RsResult<FlatTree, PanicMessage> expansion = expandMacro.getExpansion();
        if (expansion instanceof RsResult.Ok) {
            FlatTree flatTree = ((RsResult.Ok<FlatTree, PanicMessage>) expansion).getOk();
            return new RsResult.Ok<>(flatTree.toTokenTree(version));
        } else {
            PanicMessage panic = ((RsResult.Err<FlatTree, PanicMessage>) expansion).getErr();
            return new RsResult.Err<>(new ProcMacroExpansionError.ServerSideError(panic.getMessage()));
        }
    }

    @NotNull
    private ProcMacroExpansionError toProcMacroExpansionError(@NotNull RequestSendError error) {
        if (error instanceof RequestSendError.Timeout) {
            return new ProcMacroExpansionError.Timeout(myTimeout);
        } else if (error instanceof RequestSendError.ProcessCreation) {
            MacroExpansionManagerUtil.MACRO_LOG.warn("Failed to run `" + RsPathManager.INTELLIJ_RUST_NATIVE_HELPER + "` process",
                ((RequestSendError.ProcessCreation) error).getException());
            return ProcMacroExpansionError.CantRunExpander;
        } else if (error instanceof RequestSendError.IO) {
            java.io.IOException e = ((RequestSendError.IO) error).getException();
            if (e instanceof ProcessAbortedException) {
                return new ProcMacroExpansionError.ProcessAborted(((ProcessAbortedException) e).getExitCode());
            } else {
                if (!OpenApiUtil.isUnitTestMode()) {
                    MacroExpansionManagerUtil.MACRO_LOG.error(
                        "Error communicating with `" + RsPathManager.INTELLIJ_RUST_NATIVE_HELPER + "` process", e);
                }
                return ProcMacroExpansionError.IOExceptionThrown;
            }
        } else if (error instanceof RequestSendError.UnknownVersion) {
            return new ProcMacroExpansionError.UnsupportedExpanderVersion(((RequestSendError.UnknownVersion) error).getVersion());
        }
        return ProcMacroExpansionError.ExecutableNotFound;
    }

    /**
     * Checks if the given PSI element contains errors that should be handled (for fixup).
     */
    public boolean hasErrorToHandle(@NotNull PsiElement psi) {
        if (psi instanceof RsDotExpr) return false;
        for (PsiElement child = psi.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof PsiErrorElement) return true;
            if (!(child instanceof RsExpr) && hasErrorToHandle(child)) return true;
        }
        return false;
    }

    @NotNull
    public static ProcMacroExpander forCrate(@NotNull Crate crate) {
        Project project = crate.getProject();
        RsToolchainBase toolchain = RustProjectSettingsServiceUtil.getRustSettings(project).getToolchain();
        SemVer rustcVersion = crate.getCargoProject() != null && crate.getCargoProject().getRustcInfo() != null
            ? crate.getCargoProject().getRustcInfo().getRealVersion().getSemver()
            : null;
        java.nio.file.Path procMacroExpanderPath = crate.getCargoProject() != null
            ? crate.getCargoProject().getProcMacroExpanderPath()
            : null;

        ProcMacroServerPool server = null;
        if (toolchain != null && rustcVersion != null && procMacroExpanderPath != null) {
            boolean needsVersionCheck = rustcVersion.compareTo(MIN_RUSTC_VERSION_WITH_EXPANDER_VERSION_CHECK) >= 0;
            server = ProcMacroApplicationService.getInstance().getServer(toolchain, needsVersionCheck, procMacroExpanderPath);
        }
        return new ProcMacroExpander(project, toolchain, server);
    }

    @NotNull
    public static ProcMacroExpander create(@NotNull Project project, @Nullable ProcMacroServerPool server) {
        RsToolchainBase toolchain = RustProjectSettingsServiceUtil.getRustSettings(project).getToolchain();
        return new ProcMacroExpander(project, toolchain, server);
    }
}
