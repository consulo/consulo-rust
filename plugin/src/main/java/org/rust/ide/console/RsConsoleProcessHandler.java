/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import com.intellij.execution.process.KillableColoredProcessHandler;
import consulo.disposer.Disposer;
import consulo.util.dataholder.Key;
import consulo.process.io.BaseOutputReader;
import jakarta.annotation.Nonnull;

import java.nio.charset.Charset;

public class RsConsoleProcessHandler extends KillableColoredProcessHandler {

    @Nonnull
    private final RsConsoleView consoleView;
    @Nonnull
    private final RsConsoleCommunication consoleCommunication;

    public RsConsoleProcessHandler(@Nonnull Process process,
                                   @Nonnull RsConsoleView consoleView,
                                   @Nonnull RsConsoleCommunication consoleCommunication,
                                   @Nonnull String commandLine,
                                   @Nonnull Charset charset) {
        super(process, commandLine, charset);
        this.consoleView = consoleView;
        this.consoleCommunication = consoleCommunication;
        Disposer.register(consoleView, () -> {
            if (!isProcessTerminated()) {
                destroyProcess();
            }
        });
    }

    public void coloredTextAvailable(@Nonnull String textOriginal, @Nonnull Key attributes) {
        String text = consoleCommunication.processText(textOriginal);
        consoleView.print(text, attributes);
    }

    public boolean isSilentlyDestroyOnClose() {
        return !consoleCommunication.isExecuting();
    }

    public boolean shouldKillProcessSoftly() {
        return true;
    }

    @Nonnull
    protected BaseOutputReader.Options readerOptions() {
        return BaseOutputReader.Options.forMostlySilentProcess();
    }
}
