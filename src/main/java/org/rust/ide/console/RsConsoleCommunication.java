/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RsConsoleCommunication {

    /**
     * \u0091 and \u0092 are C1 control codes (https://en.wikipedia.org/wiki/C0_and_C1_control_codes#C1_control_codes_for_general_use)
     * with names "Private Use 1" and "Private Use 2"
     * and meaning
     *     "Reserved for a function without standardized meaning for private use as required,
     *      subject to the prior agreement of the sender and the recipient of the data."
     * so they are ideal for our purpose
     */
    @NotNull
    public static final String SUCCESS_EXECUTION_MARKER = "\u0091";
    @NotNull
    public static final String FAILED_EXECUTION_MARKER = "\u0092";

    @NotNull
    private final RsConsoleView consoleView;
    private volatile boolean isExecuting = false;
    private boolean receivedInitialPrompt = true;
    @Nullable
    private RsConsoleOneCommandContext lastCommandContext = null;

    public RsConsoleCommunication(@NotNull RsConsoleView consoleView) {
        this.consoleView = consoleView;
    }

    public boolean isExecuting() {
        return isExecuting;
    }

    public void onExecutionBegin() {
        if (isExecuting) {
            throw new IllegalStateException("new command must not be executed before previous command finishes");
        }
        isExecuting = true;

        var codeFragment = consoleView.getCodeFragment();
        if (codeFragment == null) return;
        String codeFragmentText = codeFragment.getText().trim();
        if (codeFragmentText.isEmpty()) return;
        if (codeFragmentText.startsWith(":")) {
            consoleView.handleEvcxrCommand(codeFragmentText);
        }
        lastCommandContext = new RsConsoleOneCommandContext(codeFragment);
    }

    private void onExecutionEnd(boolean success) {
        if (!receivedInitialPrompt) {
            receivedInitialPrompt = true;
            return;
        }

        RsConsoleOneCommandContext ctx = lastCommandContext;
        if (success && ctx != null) {
            consoleView.addToContext(ctx);
        }
        this.lastCommandContext = null;

        if (!isExecuting) {
            throw new IllegalStateException("isExecuting expected to be true");
        }
        isExecuting = false;
    }

    @NotNull
    public String processText(@NotNull String textOriginal) {
        String text = textOriginal.replace("\r", "");
        if (text.contains(SUCCESS_EXECUTION_MARKER)) {
            onExecutionEnd(true);
            return text.replace(SUCCESS_EXECUTION_MARKER, "");
        } else if (text.contains(FAILED_EXECUTION_MARKER)) {
            onExecutionEnd(false);
            return text.replace(FAILED_EXECUTION_MARKER, "");
        } else if (text.equals(RsConsoleView.PROMPT)) {
            return "";
        } else {
            return text;
        }
    }
}
