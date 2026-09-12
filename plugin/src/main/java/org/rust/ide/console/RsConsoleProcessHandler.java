/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import com.intellij.execution.process.KillableColoredProcessHandler;
import consulo.disposer.Disposer;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.util.dataholder.Key;
import consulo.process.io.BaseOutputReader;
import jakarta.annotation.Nonnull;

public class RsConsoleProcessHandler extends KillableColoredProcessHandler {

    @Nonnull
    private final RsConsoleView consoleView;
    @Nonnull
    private final RsConsoleCommunication consoleCommunication;

    public RsConsoleProcessHandler(@Nonnull GeneralCommandLine commandLine,
                                   @Nonnull RsConsoleView consoleView,
                                   @Nonnull RsConsoleCommunication consoleCommunication) throws ExecutionException {
        super(commandLine);
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
