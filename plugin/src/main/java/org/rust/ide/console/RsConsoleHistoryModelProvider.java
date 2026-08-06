/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import com.intellij.execution.console.ConsoleHistoryModel;
import com.intellij.execution.console.ConsoleHistoryModelProvider;
import consulo.codeEditor.Editor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class RsConsoleHistoryModelProvider implements ConsoleHistoryModelProvider {
    @Override
    @Nullable
    public ConsoleHistoryModel createModel(@Nonnull String persistenceId, @Nonnull Editor editor) {
        return null;
    }
}
