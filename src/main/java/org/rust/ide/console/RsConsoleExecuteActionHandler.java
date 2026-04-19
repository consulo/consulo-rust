/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import com.intellij.execution.console.LanguageConsoleView;
import com.intellij.execution.console.ProcessBackedConsoleExecuteActionHandler;
import com.intellij.execution.process.ProcessHandler;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.openapiext.OpenApiUtil;

public class RsConsoleExecuteActionHandler extends ProcessBackedConsoleExecuteActionHandler {

    @NotNull
    private final RsConsoleCommunication consoleCommunication;
    private boolean isEnabled = false;

    @Nls
    @NotNull
    public static final String prevCommandRunningMessage =
        RsBundle.message("previous.command.is.still.running.please.wait.or.press.ctrl.c.in.console.to.interrupt");
    @Nls
    @NotNull
    public static final String consoleIsNotEnabledMessage = RsBundle.message("console.is.not.enabled");

    public RsConsoleExecuteActionHandler(@NotNull ProcessHandler processHandler,
                                         @NotNull RsConsoleCommunication consoleCommunication) {
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
    public void processLine(@NotNull String line) {
        String lineEscaped = line.replace("\n", "\u2028");
        super.processLine(lineEscaped);
    }

    @Override
    public void runExecuteAction(@NotNull LanguageConsoleView console) {
        if (!isEnabled) {
            OpenApiUtil.showErrorHint(console.getConsoleEditor(), consoleIsNotEnabledMessage);
            return;
        }

        if (!canExecuteNow()) {
            OpenApiUtil.showErrorHint(console.getConsoleEditor(), prevCommandRunningMessage);
            return;
        }

        consoleCommunication.onExecutionBegin();
        copyToHistoryAndExecute(console);
    }

    private boolean canExecuteNow() {
        return !consoleCommunication.isExecuting();
    }

    private void copyToHistoryAndExecute(@NotNull LanguageConsoleView console) {
        super.runExecuteAction(console);
    }
}
