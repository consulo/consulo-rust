/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import com.intellij.execution.actions.EOFAction;
import com.intellij.icons.AllIcons;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;

public class RsConsoleActions {

    public static class RestartAction extends AnAction {
        @NotNull
        private final RsConsoleRunner consoleRunner;

        public RestartAction(@NotNull RsConsoleRunner consoleRunner) {
            this.consoleRunner = consoleRunner;
            ActionUtil.copyFrom(this, IdeActions.ACTION_RERUN);
            getTemplatePresentation().setIcon(AllIcons.Actions.Restart);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            consoleRunner.rerun();
        }
    }

    public static class StopAction extends DumbAwareAction {
        @NotNull
        private final RsConsoleProcessHandler processHandler;

        public StopAction(@NotNull RsConsoleProcessHandler processHandler) {
            super(RsBundle.message("action.stop.console.text"),
                  RsBundle.message("action.stop.rust.console.description"),
                  AllIcons.Actions.Suspend);
            this.processHandler = processHandler;
            AnAction eofAction = ActionManager.getInstance().getAction(EOFAction.ACTION_ID);
            copyShortcutFrom(eofAction);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(!processHandler.isProcessTerminated());
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            processHandler.destroyProcess();
        }
    }

    public static class SoftWrapAction extends ToggleAction implements DumbAware {
        @NotNull
        private final RsConsoleView consoleView;
        private boolean isSelected = false;

        public SoftWrapAction(@NotNull RsConsoleView consoleView) {
            super(ActionsBundle.actionText("EditorToggleUseSoftWraps"),
                  ActionsBundle.actionDescription("EditorToggleUseSoftWraps"),
                  AllIcons.Actions.ToggleSoftWrap);
            this.consoleView = consoleView;
            updateEditors();
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return isSelected;
        }

        private void updateEditors() {
            consoleView.getEditor().getSettings().setUseSoftWraps(isSelected);
            consoleView.getConsoleEditor().getSettings().setUseSoftWraps(isSelected);
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            isSelected = state;
            updateEditors();
        }
    }

    public static class PrintAction extends DumbAwareAction {
        @NotNull
        private final RsConsoleView consoleView;
        @NotNull
        private final AnAction printAction;

        public PrintAction(@NotNull RsConsoleView consoleView) {
            this.consoleView = consoleView;
            this.printAction = ActionManager.getInstance().getAction("Print");
            ActionUtil.copyFrom(this, "Print");
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            printAction.update(createActionEvent(e));
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            printAction.actionPerformed(createActionEvent(e));
        }

        @NotNull
        private AnActionEvent createActionEvent(@NotNull AnActionEvent e) {
            DataContext dataContext = new ConsoleDataContext(e.getDataContext(), consoleView);
            return new AnActionEvent(e.getInputEvent(), dataContext, e.getPlace(), e.getPresentation(), e.getActionManager(), e.getModifiers());
        }

        private static class ConsoleDataContext implements DataContext {
            @NotNull
            private final DataContext myOriginalDataContext;
            @NotNull
            private final RsConsoleView consoleView;

            ConsoleDataContext(@NotNull DataContext originalDataContext, @NotNull RsConsoleView consoleView) {
                this.myOriginalDataContext = originalDataContext;
                this.consoleView = consoleView;
            }

            @Override
            @Nullable
            public Object getData(@NotNull String dataId) {
                if (CommonDataKeys.EDITOR.is(dataId)) {
                    return consoleView.getEditor();
                } else {
                    return myOriginalDataContext.getData(dataId);
                }
            }
        }
    }

    public static class ShowVariablesAction extends ToggleAction implements DumbAware {
        @NotNull
        private final RsConsoleView consoleView;

        public ShowVariablesAction(@NotNull RsConsoleView consoleView) {
            super(RsBundle.message("action.show.variables.text"),
                  RsBundle.message("action.shows.active.console.variables.description"),
                  AllIcons.Debugger.Watch);
            this.consoleView = consoleView;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return consoleView.isShowVariables();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            consoleView.updateVariables(state);
        }
    }
}
