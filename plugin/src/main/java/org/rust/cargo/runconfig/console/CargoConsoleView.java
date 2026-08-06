/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.console;

import consulo.ide.impl.idea.execution.impl.ConsoleViewImpl;
import consulo.document.Document;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.codeEditor.EditorEx;
import consulo.project.Project;
import consulo.language.psi.scope.GlobalSearchScope;
import jakarta.annotation.Nonnull;

import java.util.regex.Pattern;

public class CargoConsoleView extends ConsoleViewImpl {

    private static final Pattern ERROR_RE = Pattern.compile("^\\s*error\\S*:.*");

    private boolean myHasErrors = false;

    public CargoConsoleView(@Nonnull Project project,
                            @Nonnull GlobalSearchScope searchScope,
                            boolean viewer,
                            boolean usePredefinedMessageFilter) {
        super(project, searchScope, viewer, usePredefinedMessageFilter);
    }

    @Nonnull
    @Override
    protected EditorEx doCreateConsoleEditor() {
        EditorEx editor = super.doCreateConsoleEditor();
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@Nonnull DocumentEvent e) {
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

            private void processLine(int lineNumber, @Nonnull CharSequence line) {
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
