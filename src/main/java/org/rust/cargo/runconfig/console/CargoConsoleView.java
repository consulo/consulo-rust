/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.console;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class CargoConsoleView extends ConsoleViewImpl {

    private static final Pattern ERROR_RE = Pattern.compile("^\\s*error\\S*:.*");

    private boolean myHasErrors = false;

    public CargoConsoleView(@NotNull Project project,
                            @NotNull GlobalSearchScope searchScope,
                            boolean viewer,
                            boolean usePredefinedMessageFilter) {
        super(project, searchScope, viewer, usePredefinedMessageFilter);
    }

    @NotNull
    @Override
    protected EditorEx doCreateConsoleEditor() {
        EditorEx editor = super.doCreateConsoleEditor();
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent e) {
                if (!e.getNewFragment().toString().contains("error")) return;

                Document document = e.getDocument();
                int startLine = document.getLineNumber(e.getOffset());
                int endLine = document.getLineNumber(e.getOffset() + e.getNewLength());
                for (int lineNumber = startLine; lineNumber <= endLine; lineNumber++) {
                    int lineStart = document.getLineStartOffset(lineNumber);
                    int lineEnd = document.getLineEndOffset(lineNumber);
                    CharSequence line = document.getImmutableCharSequence().subSequence(lineStart, lineEnd);
                    processLine(lineNumber, line);
                }
            }

            private void processLine(int lineNumber, @NotNull CharSequence line) {
                if (ERROR_RE.matcher(line).matches()) {
                    if (!myHasErrors) {
                        getEditor().getCaretModel().moveToLogicalPosition(new LogicalPosition(lineNumber - 1, 0));
                        getEditor().getScrollingModel().scrollToCaret(ScrollType.CENTER);
                    }
                    myHasErrors = true;
                }
            }
        });
        return editor;
    }

    @Override
    public void scrollToEnd() {
        if (myHasErrors) return;
        super.scrollToEnd();
    }
}
