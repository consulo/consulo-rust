/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.proc;


import consulo.project.Project;
import consulo.application.util.registry.Registry;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.util.ToolchainUtil;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.macros.*;
import org.rust.lang.core.macros.errors.ProcMacroExpansionError;
import org.rust.lang.core.macros.tt.*;
import org.rust.lang.core.psi.*;
// import removed - placeholder
import org.rust.openapiext.RsPathManager;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.RsResult;
import consulo.util.lang.SemVer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Procedural macro expander.
 */
import java.util.function.UnaryOperator;

public class ProcMacroExpander extends MacroExpander<RsProcMacroData, ProcMacroExpansionError> {

    /** Time allotted to one procedural macro expansion, unless the registry key overrides it. */
    private static final int DEFAULT_TIMEOUT_MS = 20000;

    public static final int EXPANDER_VERSION = 11;
    private static final SemVer MIN_RUSTC_VERSION_WITH_EXPANDER_VERSION_CHECK =
        ToolchainUtil.parseSemVer("1.70.0");

    @Nonnull
    private final Project myProject;
    @Nullable
    private final UnaryOperator<String> myRemotePath;
    @Nullable
    private final ProcMacroServer myServer;
    private final long myTimeout;
    private final boolean myIsEnabled;

    private ProcMacroExpander(
        @Nonnull Project project,
        @Nullable UnaryOperator<String> remotePath,
        @Nullable ProcMacroServer server,
        long timeout
    ) {
        myProject = project;
        myRemotePath = remotePath;
        myServer = server;
        myTimeout = timeout;
        myIsEnabled = server != null || ProcMacroFlags.isAnyEnabled();
    }

    private ProcMacroExpander(
        @Nonnull Project project,
        @Nullable UnaryOperator<String> remotePath,
        @Nullable ProcMacroServer server
    ) {
        this(project, remotePath, server, Registry.get("org.rust.macros.proc.timeout").asInteger(DEFAULT_TIMEOUT_MS));
    }

    @Nonnull
    private RsResult<ProcMacroServer, ProcMacroExpansionError> serverOrErr() {
        if (myServer != null) {
            return new RsResult.Ok<>(myServer);
        }
        if (myIsEnabled) {
            return new RsResult.Err<>(ProcMacroExpansionError.ExecutableNotFound);
        }
        return new RsResult.Err<>(ProcMacroExpansionError.ProcMacroExpansionIsDisabled);
    }

    @Nonnull
    @Override
    public RsResult<Pair<CharSequence, RangeMap>, ProcMacroExpansionError> expandMacroAsTextWithErr(
        @Nonnull RsProcMacroData def,
        @Nonnull RsMacroCallData call
    ) {
        RsResult<ProcMacroServer, ProcMacroExpansionError> serverResult = serverOrErr();
        if (serverResult instanceof RsResult.Err) {
            @SuppressWarnings("unchecked")
            RsResult.Err<Pair<CharSequence, RangeMap>, ProcMacroExpansionError> err =
                new RsResult.Err<>(((RsResult.Err<ProcMacroServer, ProcMacroExpansionError>) serverResult).getErr());
            return err;
        }
        ProcMacroServer server = ((RsResult.Ok<ProcMacroServer, ProcMacroExpansionError>) serverResult).getOk();

        MacroCallBody macroBody = call.getMacroBody();
        if (macroBody instanceof MacroCallBody.FunctionLike && !ProcMacroFlags.isFunctionLikeEnabled()) {
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

    @Nonnull
    public RsResult<TokenTree.Subtree, ProcMacroExpansionError> expandMacroAsTtWithErr(
        @Nonnull TokenTree.Subtree macroCallBody,
        @Nullable TokenTree.Subtree attributes,
        @Nonnull String macroName,
        @Nonnull String lib,
        @Nonnull Map<String, String> env
    ) {
        RsResult<ProcMacroServer, ProcMacroExpansionError> serverResult = serverOrErr();
        if (serverResult instanceof RsResult.Err) {
            @SuppressWarnings("unchecked")
            RsResult.Err<TokenTree.Subtree, ProcMacroExpansionError> err =
                new RsResult.Err<>(((RsResult.Err<ProcMacroServer, ProcMacroExpansionError>) serverResult).getErr());
            return err;
        }
        ProcMacroServer server = ((RsResult.Ok<ProcMacroServer, ProcMacroExpansionError>) serverResult).getOk();
        return expandMacroAsTtWithErrInternal(server, macroCallBody, attributes, macroName, lib, env);
    }

    @Nonnull
    private RsResult<TokenTree.Subtree, ProcMacroExpansionError> expandMacroAsTtWithErrInternal(
        @Nonnull ProcMacroServer server,
        @Nonnull TokenTree.Subtree macroCallBody,
        @Nullable TokenTree.Subtree attributes,
        @Nonnull String macroName,
        @Nonnull String lib,
        @Nonnull Map<String, String> env
    ) {
        String remoteLib = myRemotePath != null ? myRemotePath.apply(lib) : lib;
        Map<String, String> envMapped = new HashMap<>();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            envMapped.put(entry.getKey(), myRemotePath != null ? myRemotePath.apply(entry.getValue()) : entry.getValue());
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

    @Nonnull
    private ProcMacroExpansionError toProcMacroExpansionError(@Nonnull RequestSendError error) {
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
    public boolean hasErrorToHandle(@Nonnull PsiElement psi) {
        if (psi instanceof RsDotExpr) return false;
        for (PsiElement child = psi.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof PsiErrorElement) return true;
            if (!(child instanceof RsExpr) && hasErrorToHandle(child)) return true;
        }
        return false;
    }

    @Nonnull
    /**
     * An expander that maps host paths with {@code remotePath} and expands through {@code server}.
     * Both may be null, in which case expansion is unavailable and paths are passed through.
     */
    public static ProcMacroExpander create(@Nonnull Project project,
                                           @Nullable UnaryOperator<String> remotePath,
                                           @Nullable ProcMacroServer server) {
        return new ProcMacroExpander(project, remotePath, server);
    }

    /** The expander configured for {@code crate}. */
    public static ProcMacroExpander forCrate(@Nonnull Crate crate) {
        return ProcMacroExpanderProvider.getInstance().forCrate(crate);
    }

}
