/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import consulo.execution.ui.console.language.LanguageConsoleView;
import consulo.execution.ui.console.language.ProcessBackedConsoleExecuteActionHandler;
import consulo.process.ProcessHandler;

import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.openapiext.OpenApiUtil;

public class RsConsoleExecuteActionHandler extends ProcessBackedConsoleExecuteActionHandler {

    @Nonnull
    private final RsConsoleCommunication consoleCommunication;
    private boolean isEnabled = false;

    
    @Nonnull
    public static final String prevCommandRunningMessage =
        RsBundle.message("previous.command.is.still.running.please.wait.or.press.ctrl.c.in.console.to.interrupt");
    
    @Nonnull
    public static final String consoleIsNotEnabledMessage = RsBundle.message("console.is.not.enabled");

    public RsConsoleExecuteActionHandler(@Nonnull ProcessHandler processHandler,
                                         @Nonnull RsConsoleCommunication consoleCommunication) {
        super(processHandler, false);
        this.consoleCommunication = consoleCommunication;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    @Override
    public void processLine(@Nonnull String line) {
        String lineEscaped = line.replace("\n", "\u2028");
        super.processLine(lineEscaped);
    }

    @Override
    public void runExecuteAction(@Nonnull LanguageConsoleView console) {
        if (!isEnabled) {
            org.rust.openapiext.ui.EditorExt.showErrorHint(console.getConsoleEditor(), consoleIsNotEnabledMessage);
            return;
        }

        if (!canExecuteNow()) {
            org.rust.openapiext.ui.EditorExt.showErrorHint(console.getConsoleEditor(), prevCommandRunningMessage);
            return;
        }

        consoleCommunication.onExecutionBegin();
        copyToHistoryAndExecute(console);
    }

    private boolean canExecuteNow() {
        return !consoleCommunication.isExecuting();
    }

    private void copyToHistoryAndExecute(@Nonnull LanguageConsoleView console) {
        super.runExecuteAction(console);
    }
}
