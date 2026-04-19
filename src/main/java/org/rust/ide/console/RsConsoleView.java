/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import com.intellij.execution.console.LanguageConsoleImpl;
import com.intellij.execution.impl.ConsoleViewUtil;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.ObservableConsoleView;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.JBSplitter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsReplCodeFragment;
import org.rust.openapiext.VirtualFileExtUtil;

import javax.swing.*;
import java.awt.*;

public class RsConsoleView extends LanguageConsoleImpl implements ObservableConsoleView {

    @NotNull
    public static final String PROMPT = ">> ";
    @NotNull
    public static final String INDENT_PROMPT = ".. ";
    @NotNull
    public static final String VIRTUAL_FILE_NAME = "IntellijRustRepl.rs";
    @NotNull
    private static final Key<Boolean> RUST_CONSOLE_KEY = Key.create("RS_CONSOLE_KEY");

    @Nullable
    private RsConsoleExecuteActionHandler executeActionHandler;
    @NotNull
    private final ActionCallback initialized = new ActionCallback();
    @Nullable
    private final RsReplCodeFragment codeFragment;
    @NotNull
    private final RsConsoleCodeFragmentContext codeFragmentContext;
    @Nullable
    private RsConsoleVariablesView variablesView;
    @NotNull
    private final RsConsoleOptions options;

    public RsConsoleView(@NotNull Project project) {
        super(project, VIRTUAL_FILE_NAME, RsLanguage.INSTANCE);
        getVirtualFile().putUserData(RUST_CONSOLE_KEY, true);
        // Mark editor as console one, to prevent autopopup completion
        getHistoryViewer().putUserData(ConsoleViewUtil.EDITOR_IS_CONSOLE_HISTORY_VIEW, true);
        super.setPrompt(PROMPT);
        getConsolePromptDecorator().setIndentPrompt(INDENT_PROMPT);
        setUpdateFoldingsEnabled(false);

        var psiFile = VirtualFileExtUtil.toPsiFile(getVirtualFile(), project);
        codeFragment = psiFile instanceof RsReplCodeFragment ? (RsReplCodeFragment) psiFile : null;
        codeFragmentContext = new RsConsoleCodeFragmentContext(codeFragment);
        options = RsConsoleOptions.getInstance(project);
    }

    @Nullable
    public RsReplCodeFragment getCodeFragment() {
        return codeFragment;
    }

    @Nullable
    public RsConsoleExecuteActionHandler getExecuteActionHandler() {
        return executeActionHandler;
    }

    public void setExecuteActionHandler(@NotNull RsConsoleExecuteActionHandler handler) {
        this.executeActionHandler = handler;
    }

    @Override
    public void requestFocus() {
        initialized.doWhenDone(() -> {
            IdeFocusManager.getGlobalInstance().requestFocus(getConsoleEditor().getContentComponent(), true);
        });
    }

    @Override
    @NotNull
    protected JComponent createCenterComponent() {
        // workaround for extra lines appearing in the console
        JComponent centerComponent = super.createCenterComponent();
        getHistoryViewer().getSettings().setAdditionalLinesCount(0);
        getHistoryViewer().getSettings().setUseSoftWraps(false);
        getConsoleEditor().getGutterComponentEx().setBackground(getConsoleEditor().getBackgroundColor());
        getConsoleEditor().getGutterComponentEx().revalidate();
        getConsoleEditor().getColorsScheme().setColor(EditorColors.GUTTER_BACKGROUND, getConsoleEditor().getBackgroundColor());

        return centerComponent;
    }

    public void print(@NotNull String text, @NotNull Key<?> attributes) {
        print(text, outputTypeForAttributes(attributes));
    }

    @NotNull
    private ConsoleViewContentType outputTypeForAttributes(@NotNull Key<?> attributes) {
        if (attributes == ProcessOutputTypes.STDERR) {
            return ConsoleViewContentType.ERROR_OUTPUT;
        } else if (attributes == ProcessOutputTypes.SYSTEM) {
            return ConsoleViewContentType.SYSTEM_OUTPUT;
        } else {
            return ConsoleViewContentType.getConsoleViewType(attributes);
        }
    }

    public void initialized() {
        initialized.setDone();
    }

    public void addToContext(@NotNull RsConsoleOneCommandContext lastCommandContext) {
        if (codeFragment != null) {
            codeFragmentContext.addToContext(lastCommandContext);
            codeFragmentContext.updateContextAsync(getProject(), codeFragment);
            if (variablesView != null) {
                variablesView.rebuild();
            }
        }
    }

    public void removeBorders() {
        getHistoryViewer().setBorder(null);
        getConsoleEditor().setBorder(null);
    }

    public boolean isShowVariables() {
        return options.showVariables;
    }

    public void updateVariables(boolean state) {
        if (options.showVariables == state) return;
        options.showVariables = state;
        if (state) {
            showVariables();
        } else {
            hideVariables();
        }
    }

    private void showVariables() {
        variablesView = new RsConsoleVariablesView(getProject(), codeFragmentContext);

        Component console = getComponent(0);
        removeAll();
        JBSplitter splitter = new JBSplitter(false, 2f / 3);
        splitter.setFirstComponent((JComponent) console);
        splitter.setSecondComponent(variablesView);
        splitter.setShowDividerControls(true);
        splitter.setHonorComponentsMinimumSize(true);
        add(splitter, BorderLayout.CENTER);
        validate();
        repaint();
    }

    private void hideVariables() {
        Component splitterComponent = getComponent(0);
        RsConsoleVariablesView vv = variablesView;
        if (vv != null && splitterComponent instanceof JBSplitter) {
            JBSplitter splitter = (JBSplitter) splitterComponent;
            removeAll();
            Disposer.dispose(vv);
            variablesView = null;
            add(splitter.getFirstComponent(), BorderLayout.CENTER);
            validate();
            repaint();
        }
    }

    public void initVariablesWindow() {
        if (options.showVariables) {
            showVariables();
        }
    }

    public void handleEvcxrCommand(@NotNull String command) {
        if (":clear".equals(command)) {
            codeFragmentContext.clearAllCommands();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (variablesView != null) {
            Disposer.dispose(variablesView);
        }
        variablesView = null;
    }
}
