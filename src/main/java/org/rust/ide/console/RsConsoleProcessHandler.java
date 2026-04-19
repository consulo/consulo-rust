/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.util.io.BaseOutputReader;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

public class RsConsoleProcessHandler extends KillableColoredProcessHandler {

    @NotNull
    private final RsConsoleView consoleView;
    @NotNull
    private final RsConsoleCommunication consoleCommunication;

    public RsConsoleProcessHandler(@NotNull Process process,
                                   @NotNull RsConsoleView consoleView,
                                   @NotNull RsConsoleCommunication consoleCommunication,
                                   @NotNull String commandLine,
                                   @NotNull Charset charset) {
        super(process, commandLine, charset);
        this.consoleView = consoleView;
        this.consoleCommunication = consoleCommunication;
        Disposer.register(consoleView, () -> {
            if (!isProcessTerminated()) {
                destroyProcess();
            }
        });
    }

    @Override
    public void coloredTextAvailable(@NotNull String textOriginal, @NotNull Key attributes) {
        String text = consoleCommunication.processText(textOriginal);
        consoleView.print(text, attributes);
    }

    @Override
    public boolean isSilentlyDestroyOnClose() {
        return !consoleCommunication.isExecuting();
    }

    @Override
    public boolean shouldKillProcessSoftly() {
        return true;
    }

    @Override
    @NotNull
    protected BaseOutputReader.Options readerOptions() {
        return BaseOutputReader.Options.forMostlySilentProcess();
    }
}
