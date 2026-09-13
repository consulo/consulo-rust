package org.rust.lang.core.macros.proc;

import jakarta.annotation.Nonnull;
import org.rust.stdext.RsResult;

/**
 * The expander process, as macro expansion sees it: something requests can be sent to.
 * Implemented where the toolchain that launches the process is known.
 */
public interface ProcMacroServer {
    @Nonnull
    RsResult<Response, RequestSendError> send(@Nonnull Request request, long timeout);

    @Nonnull
    RsResult<ProMacroExpanderVersion, RequestSendError> requestExpanderVersion();
}
