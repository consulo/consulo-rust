/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import com.intellij.execution.console.ConsoleHistoryModel;
import com.intellij.execution.console.ConsoleHistoryModelProvider;
import com.intellij.execution.console.LanguageConsoleView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RsConsoleHistoryModelProvider implements ConsoleHistoryModelProvider {
    @Override
    @Nullable
    public ConsoleHistoryModel createModel(@NotNull String persistenceId, @NotNull LanguageConsoleView consoleView) {
        // ConsoleHistoryModel is abstract; return null to use default behavior
        return null;
    }
}
