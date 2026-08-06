/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import consulo.process.cmd.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.process.KillableColoredProcessHandler;
import consulo.process.util.AnsiEscapeDecoder;
import consulo.util.dataholder.Key;
import consulo.process.io.BaseOutputReader;
import com.pty4j.PtyProcess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.charset.Charset;

/**
 * Same as {@link com.intellij.execution.process.KillableColoredProcessHandler}, but uses {@link RsAnsiEscapeDecoder}.
 */
public class RsProcessHandler extends KillableColoredProcessHandler implements AnsiEscapeDecoder.ColoredTextAcceptor {
    @Nullable
    private final AnsiEscapeDecoder myDecoder;

    public RsProcessHandler(@Nonnull GeneralCommandLine commandLine) throws consulo.process.ExecutionException {
        this(commandLine, true);
    }

    public RsProcessHandler(@Nonnull GeneralCommandLine commandLine, boolean processColors) throws consulo.process.ExecutionException {
        super(commandLine);
        setHasPty(commandLine instanceof PtyCommandLine);
        setShouldDestroyProcessRecursively(!hasPty());
        myDecoder = (processColors && !hasPty()) ? new RsAnsiEscapeDecoder() : null;
    }

    public RsProcessHandler(
        @Nonnull Process process,
        @Nonnull String commandRepresentation,
        @Nonnull Charset charset,
        boolean processColors
    ) {
        super(process, commandRepresentation, charset);
        setHasPty(process instanceof PtyProcess);
        setShouldDestroyProcessRecursively(!hasPty());
        myDecoder = (processColors && !hasPty()) ? new RsAnsiEscapeDecoder() : null;
    }

    public RsProcessHandler(
        @Nonnull Process process,
        @Nonnull String commandRepresentation,
        @Nonnull Charset charset
    ) {
        this(process, commandRepresentation, charset, true);
    }

    @Override
    public void notifyTextAvailable(@Nonnull String text, @Nonnull Key outputType) {
        if (myDecoder != null) {
            myDecoder.escapeText(text, outputType, this);
        } else {
            super.notifyTextAvailable(text, outputType);
        }
    }

    @Override
    public void coloredTextAvailable(@Nonnull String text, @Nonnull Key attributes) {
        super.notifyTextAvailable(text, attributes);
    }

    @Nonnull
    protected BaseOutputReader.Options readerOptions() {
        if (hasPty()) {
            return BaseOutputReader.Options.NON_BLOCKING; // forTerminalPtyProcess not in Consulo
        } else {
            return BaseOutputReader.Options.forMostlySilentProcess();
        }
    }
}
