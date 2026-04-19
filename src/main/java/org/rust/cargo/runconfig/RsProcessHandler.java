/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.process.AnsiEscapeDecoder;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.openapi.util.Key;
import com.intellij.util.io.BaseOutputReader;
import com.pty4j.PtyProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;

/**
 * Same as {@link com.intellij.execution.process.KillableColoredProcessHandler}, but uses {@link RsAnsiEscapeDecoder}.
 */
public class RsProcessHandler extends KillableProcessHandler implements AnsiEscapeDecoder.ColoredTextAcceptor {
    @Nullable
    private final AnsiEscapeDecoder myDecoder;

    public RsProcessHandler(@NotNull GeneralCommandLine commandLine) throws com.intellij.execution.ExecutionException {
        this(commandLine, true);
    }

    public RsProcessHandler(@NotNull GeneralCommandLine commandLine, boolean processColors) throws com.intellij.execution.ExecutionException {
        super(commandLine);
        setHasPty(commandLine instanceof PtyCommandLine);
        setShouldDestroyProcessRecursively(!hasPty());
        myDecoder = (processColors && !hasPty()) ? new RsAnsiEscapeDecoder() : null;
    }

    public RsProcessHandler(
        @NotNull Process process,
        @NotNull String commandRepresentation,
        @NotNull Charset charset,
        boolean processColors
    ) {
        super(process, commandRepresentation, charset);
        setHasPty(process instanceof PtyProcess);
        setShouldDestroyProcessRecursively(!hasPty());
        myDecoder = (processColors && !hasPty()) ? new RsAnsiEscapeDecoder() : null;
    }

    public RsProcessHandler(
        @NotNull Process process,
        @NotNull String commandRepresentation,
        @NotNull Charset charset
    ) {
        this(process, commandRepresentation, charset, true);
    }

    @Override
    public void notifyTextAvailable(@NotNull String text, @NotNull Key outputType) {
        if (myDecoder != null) {
            myDecoder.escapeText(text, outputType, this);
        } else {
            super.notifyTextAvailable(text, outputType);
        }
    }

    @Override
    public void coloredTextAvailable(@NotNull String text, @NotNull Key attributes) {
        super.notifyTextAvailable(text, attributes);
    }

    @NotNull
    @Override
    protected BaseOutputReader.Options readerOptions() {
        if (hasPty()) {
            return BaseOutputReader.Options.forTerminalPtyProcess();
        } else {
            return BaseOutputReader.Options.forMostlySilentProcess();
        }
    }
}
