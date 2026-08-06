/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;
import consulo.ui.ex.action.ToggleAction;

import consulo.execution.impl.internal.action.EOFAction;
import consulo.application.AllIcons;
import consulo.ui.ex.action.ActionsBundle;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.util.dataholder.Key;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.ui.ex.action.util.ActionUtil;
import consulo.application.dumb.DumbAware;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;

public class RsConsoleActions {

    public static class RestartAction extends AnAction {
        @Nonnull
        private final RsConsoleRunner consoleRunner;

        public RestartAction(@Nonnull RsConsoleRunner consoleRunner) {
            this.consoleRunner = consoleRunner;
            ActionUtil.copyFrom(this, IdeActions.ACTION_RERUN);
            getTemplatePresentation().setIcon(AllIcons.Actions.Restart);
        }

        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            consoleRunner.rerun();
        }
    }

    public static class StopAction extends DumbAwareAction {
        @Nonnull
        private final RsConsoleProcessHandler processHandler;

        public StopAction(@Nonnull RsConsoleProcessHandler processHandler) {
            super(RsBundle.message("action.stop.console.text"),
                  RsBundle.message("action.stop.rust.console.description"),
                  AllIcons.Actions.Suspend);
            this.processHandler = processHandler;
            AnAction eofAction = ActionManager.getInstance().getAction(EOFAction.ACTION_ID);
            copyShortcutFrom(eofAction);
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabled(!processHandler.isProcessTerminated());
        }

        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            processHandler.destroyProcess();
        }
    }

    public static class SoftWrapAction extends ToggleAction implements DumbAware {
        @Nonnull
        private final RsConsoleView consoleView;
        private boolean isSelected = false;

        public SoftWrapAction(@Nonnull RsConsoleView consoleView) {
            super(ActionsBundle.actionText("EditorToggleUseSoftWraps"),
                  ActionsBundle.actionDescription("EditorToggleUseSoftWraps"),
                  AllIcons.Actions.ToggleSoftWrap);
            this.consoleView = consoleView;
            updateEditors();
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return isSelected;
        }

        private void updateEditors() {
            consoleView.getEditor().getSettings().setUseSoftWraps(isSelected);
            consoleView.getConsoleEditor().getSettings().setUseSoftWraps(isSelected);
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            isSelected = state;
            updateEditors();
        }
    }

    public static class PrintAction extends DumbAwareAction {
        @Nonnull
        private final RsConsoleView consoleView;
        @Nonnull
        private final AnAction printAction;

        public PrintAction(@Nonnull RsConsoleView consoleView) {
            this.consoleView = consoleView;
            this.printAction = ActionManager.getInstance().getAction("Print");
            ActionUtil.copyFrom(this, "Print");
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            printAction.update(createActionEvent(e));
        }

        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            printAction.actionPerformed(createActionEvent(e));
        }

        @Nonnull
        private AnActionEvent createActionEvent(@Nonnull AnActionEvent e) {
            DataContext dataContext = new ConsoleDataContext(e.getDataContext(), consoleView);
            return new AnActionEvent(e.getInputEvent(), dataContext, e.getPlace(), e.getPresentation(), e.getActionManager(), e.getModifiers());
        }

        private static class ConsoleDataContext implements DataContext {
            @Nonnull
            private final DataContext myOriginalDataContext;
            @Nonnull
            private final RsConsoleView consoleView;

            ConsoleDataContext(@Nonnull DataContext originalDataContext, @Nonnull RsConsoleView consoleView) {
                this.myOriginalDataContext = originalDataContext;
                this.consoleView = consoleView;
            }

            @Override
            @Nullable
            public <T> T getData(@Nonnull consulo.util.dataholder.Key<T> dataKey) {
                if (CommonDataKeys.EDITOR == dataKey) {
                    @SuppressWarnings("unchecked")
                    T t = (T) consoleView.getEditor();
                    return t;
                } else {
                    return myOriginalDataContext.getData(dataKey);
                }
            }
        }
    }

    public static class ShowVariablesAction extends ToggleAction implements DumbAware {
        @Nonnull
        private final RsConsoleView consoleView;

        public ShowVariablesAction(@Nonnull RsConsoleView consoleView) {
            super(consulo.localize.LocalizeValue.of(RsBundle.message("action.show.variables.text")),
                  consulo.localize.LocalizeValue.of(RsBundle.message("action.shows.active.console.variables.description")),
                  null);
            this.consoleView = consoleView;
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return consoleView.isShowVariables();
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            consoleView.updateVariables(state);
        }
    }
}
