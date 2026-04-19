/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import com.intellij.execution.actions.EOFAction;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.console.ConsoleHistoryController;
import com.intellij.execution.console.ProcessBackedConsoleExecuteActionHandler;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.runners.AbstractConsoleRunnerWithHistory;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.actions.CloseAction;
import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.actions.ScrollToTheEndToolbarAction;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBEmptyBorder;
import com.intellij.util.ui.MessageCategory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.cargo.toolchain.tools.Evcxr;
import org.rust.ide.icons.RsIcons;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.border.Border;

public class RsConsoleRunner extends AbstractConsoleRunnerWithHistory<RsConsoleView> {

    @NotNull
    private static final Logger LOG = Logger.getInstance(RsConsoleRunner.class);

    @Nls
    @NotNull
    public static final String TOOL_WINDOW_TITLE = RsBundle.message("rust.repl");

    private GeneralCommandLine commandLine;
    private RsConsoleCommunication consoleCommunication;

    public RsConsoleRunner(@NotNull Project project) {
        super(project, TOOL_WINDOW_TITLE, null);
    }

    @Override
    @NotNull
    public RsConsoleExecuteActionHandler getConsoleExecuteActionHandler() {
        return (RsConsoleExecuteActionHandler) super.getConsoleExecuteActionHandler();
    }

    @Override
    @Nullable
    public RsConsoleProcessHandler getProcessHandler() {
        return (RsConsoleProcessHandler) super.getProcessHandler();
    }

    @Override
    @NotNull
    protected RsConsoleView createConsoleView() {
        RsConsoleView consoleView = new RsConsoleView(getProject());
        consoleCommunication = new RsConsoleCommunication(consoleView);
        return consoleView;
    }

    @Override
    protected void createContentDescriptorAndActions() {
        ActionManager actionManager = ActionManager.getInstance();

        DefaultActionGroup runActionGroup = new DefaultActionGroup();
        ActionToolbar runToolbar = actionManager.createActionToolbar("RustConsoleRunner", runActionGroup, false);

        DefaultActionGroup outputActionGroup = new DefaultActionGroup();
        ActionToolbar outputToolbar = actionManager.createActionToolbar("RustConsoleRunner", outputActionGroup, false);
        JComponent outputToolbarComponent = outputToolbar.getComponent();
        int emptyBorderSize = outputToolbarComponent.getBorder().getBorderInsets(outputToolbarComponent).left;
        Border outsideBorder = BorderFactory.createMatteBorder(0, 1, 0, 0, JBColor.border());
        Border insideBorder = new JBEmptyBorder(emptyBorderSize);
        outputToolbarComponent.setBorder(BorderFactory.createCompoundBorder(outsideBorder, insideBorder));

        JPanel actionsPanel = new JPanel(new BorderLayout());
        actionsPanel.add(runToolbar.getComponent(), BorderLayout.WEST);
        actionsPanel.add(outputToolbarComponent, BorderLayout.CENTER);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(actionsPanel, BorderLayout.WEST);
        mainPanel.add(getConsoleView().getComponent(), BorderLayout.CENTER);
        runToolbar.setTargetComponent(mainPanel);
        outputToolbar.setTargetComponent(mainPanel);

        String title = constructConsoleTitle(getConsoleTitle());
        RunContentDescriptor contentDescriptor = new RunContentDescriptor(
            getConsoleView(), getProcessHandler(), mainPanel, title, getConsoleIcon());
        contentDescriptor.setFocusComputable(() -> getConsoleView().getConsoleEditor().getContentComponent());
        contentDescriptor.setAutoFocusContent(isAutoFocusContent());
        Disposer.register(getProject(), contentDescriptor);

        RsConsoleProcessHandler ph = getProcessHandler();
        List<AnAction> runActions = Arrays.asList(
            new RsConsoleActions.RestartAction(this),
            createConsoleExecAction(getConsoleExecuteActionHandler()),
            new RsConsoleActions.StopAction(ph),
            new CloseAction(getExecutor(), contentDescriptor, getProject())
        );
        for (AnAction action : runActions) {
            runActionGroup.add(action);
        }

        ConsoleHistoryController historyController = ConsoleHistoryController.getController(getConsoleView());
        List<AnAction> outputActions = Arrays.asList(
            new RsConsoleActions.SoftWrapAction(getConsoleView()),
            new ScrollToTheEndToolbarAction(getConsoleView().getEditor()),
            new RsConsoleActions.PrintAction(getConsoleView()),
            historyController != null ? historyController.getBrowseHistory() : null,
            new RsConsoleActions.ShowVariablesAction(getConsoleView())
        );
        for (AnAction action : outputActions) {
            if (action != null) {
                outputActionGroup.add(action);
            }
        }

        List<AnAction> allActions = new ArrayList<>();
        allActions.addAll(outputActions);
        allActions.addAll(runActions);
        allActions.add(new EOFAction());
        allActions.removeIf(a -> a == null);
        AnAction[] actionsArray = allActions.toArray(AnAction.EMPTY_ARRAY);

        registerActionShortcuts(Arrays.asList(actionsArray), getConsoleView().getConsoleEditor().getComponent());
        registerActionShortcuts(Arrays.asList(actionsArray), mainPanel);

        showConsole(getExecutor(), contentDescriptor);
    }

    @Override
    @NotNull
    protected AnAction createConsoleExecAction(@NotNull ProcessBackedConsoleExecuteActionHandler consoleExecuteActionHandler) {
        var consoleEditor = getConsoleView().getConsoleEditor();

        AnAction executeAction = super.createConsoleExecAction(consoleExecuteActionHandler);
        executeAction.registerCustomShortcutSet(getExecuteActionShortcut(), consoleEditor.getComponent());

        String actionShortcutText = KeymapUtil.getFirstKeyboardShortcutText(executeAction);
        consoleEditor.setPlaceholder(RsBundle.message("0.to.execute", actionShortcutText));
        consoleEditor.setShowPlaceholderWhenFocused(true);

        return executeAction;
    }

    @NotNull
    private ShortcutSet getExecuteActionShortcut() {
        var keymap = KeymapManager.getInstance().getActiveKeymap();
        Shortcut[] shortcuts = keymap.getShortcuts("Console.Execute.Multiline");
        if (shortcuts.length > 0) {
            return new CustomShortcutSet(shortcuts);
        } else {
            return CommonShortcuts.CTRL_ENTER;
        }
    }

    public void runSync(boolean requestEditorFocus) {
        if (Cargo.checkNeedInstallEvcxr(getProject())) return;

        try {
            initAndRun();
            ProgressManager.getInstance().run(new Task.Backgroundable(getProject(), RsBundle.message("progress.title.connecting.to.console"), false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setText(RsBundle.message("progress.text.connecting.to.console"));
                    connect();
                    if (requestEditorFocus) {
                        RsConsoleView cv = getConsoleView();
                        if (cv != null) {
                            cv.requestFocus();
                        }
                    }
                }
            });
        } catch (Exception e) {
            LOG.warn("Error running console", e);
            showErrorsInConsole(e);
        }
    }

    public void run(boolean requestEditorFocus) {
        if (Cargo.checkNeedInstallEvcxr(getProject())) return;
        @SuppressWarnings("deprecation")
        Runnable saveAction = () -> OpenApiUtil.saveAllDocuments();
        //noinspection deprecation
        TransactionGuard.submitTransaction(getProject(), saveAction);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ProgressManager.getInstance().run(new Task.Backgroundable(getProject(), RsBundle.message("progress.title.connecting.to.console"), false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setText(RsBundle.message("progress.text.connecting.to.console"));
                    try {
                        initAndRun();
                        connect();
                        if (requestEditorFocus) {
                            RsConsoleView cv = getConsoleView();
                            if (cv != null) {
                                cv.requestFocus();
                            }
                        }
                    } catch (Exception e) {
                        LOG.warn("Error running console", e);
                        ApplicationManager.getApplication().invokeAndWait(() -> showErrorsInConsole(e));
                    }
                }
            });
        });
    }

    @Override
    public void initAndRun() {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                super.initAndRun();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    @NotNull
    protected OSProcessHandler createProcessHandler(@NotNull Process process) {
        return new RsConsoleProcessHandler(
            process,
            getConsoleView(),
            consoleCommunication,
            commandLine.getCommandLineString(),
            StandardCharsets.UTF_8
        );
    }

    @NotNull
    private GeneralCommandLine createCommandLine() {
        var cargoProjects = CargoProjectServiceUtil.getCargoProjects(getProject()).getAllProjects();
        var iterator = cargoProjects.iterator();
        if (!iterator.hasNext()) {
            throw new RuntimeException("No cargo project");
        }
        var cargoProject = iterator.next();
        var toolchain = RsProjectSettingsServiceUtil.getToolchain(getProject());
        if (toolchain == null) {
            throw new RuntimeException("Rust toolchain is not defined");
        }
        var evcxr = Evcxr.create(toolchain);
        if (evcxr == null) {
            throw new RuntimeException("Evcxr executable not found");
        }

        var workingDir = CargoCommandConfiguration.getWorkingDirectory(cargoProject);
        return evcxr.createCommandLine(workingDir.toFile());
    }

    @Override
    @NotNull
    protected Process createProcess() {
        try {
            commandLine = createCommandLine();
            return commandLine.createProcess();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void connect() {
        ApplicationManager.getApplication().invokeLater(() -> {
            getConsoleView().removeBorders();
            getConsoleView().initVariablesWindow();
            getConsoleView().setExecuteActionHandler(getConsoleExecuteActionHandler());

            getConsoleExecuteActionHandler().setEnabled(true);

            getConsoleView().initialized();
        });
    }

    @Override
    @NotNull
    protected RsConsoleExecuteActionHandler createExecuteActionHandler() {
        RsConsoleExecuteActionHandler handler =
            new RsConsoleExecuteActionHandler(getProcessHandler(), consoleCommunication);
        handler.setEnabled(false);
        new ConsoleHistoryController(RsConsoleRootType.getInstance(), "", getConsoleView()).install();
        return handler;
    }

    public void rerun() {
        new Task.Backgroundable(getProject(), RsBundle.message("progress.title.restarting.console"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                RsConsoleProcessHandler ph = getProcessHandler();
                if (ph != null) {
                    ph.destroyProcess();
                    ph.waitFor();
                }

                ApplicationManager.getApplication().invokeLater(() -> {
                    new RsConsoleRunner(getProject()).run(true);
                });
            }
        }.queue();
    }

    private void showErrorsInConsole(@NotNull Exception e) {
        DefaultActionGroup actionGroup = new DefaultActionGroup(new RsConsoleActions.RestartAction(this));

        ActionToolbar actionToolbar = ActionManager.getInstance()
            .createActionToolbar("RsConsoleRunnerErrors", actionGroup, false);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(actionToolbar.getComponent(), BorderLayout.WEST);

        NewErrorTreeViewPanel errorViewPanel = new NewErrorTreeViewPanel(
            getProject(),
            null,
            /* createExitAction = */ false,
            /* createToolbar = */ false,
            /* rerunAction = */ null
        );

        List<String> messages = new ArrayList<>();
        messages.add("Can't start evcxr.");
        String message = e.getMessage();
        if (message != null && !message.isBlank()) {
            messages.addAll(Arrays.asList(message.split("\n")));
        }

        errorViewPanel.addMessage(MessageCategory.ERROR, messages.toArray(new String[0]), null, -1, -1, null);
        panel.add(errorViewPanel, BorderLayout.CENTER);

        RunContentDescriptor contentDescriptor = new RunContentDescriptor(
            null, getProcessHandler(), panel, RsBundle.message("tab.title.error.running.console"));

        showConsole(getExecutor(), contentDescriptor);
    }

    @Override
    @NotNull
    protected Icon getConsoleIcon() {
        return RsIcons.REPL;
    }
}
