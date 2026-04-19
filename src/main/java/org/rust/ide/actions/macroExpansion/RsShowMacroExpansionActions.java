/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.macroExpansion;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.rust.RsBundle;
import org.rust.lang.core.macros.errors.GetMacroExpansionError;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;
import org.rust.lang.core.psi.ext.RsPossibleMacroCallUtil;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.RsResult;

public abstract class RsShowMacroExpansionActions {

    public static abstract class RsShowMacroExpansionActionBase extends AnAction {
        private final boolean expandRecursively;

        protected RsShowMacroExpansionActionBase(boolean expandRecursively) {
            this.expandRecursively = expandRecursively;
        }

        @Override
        public ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(getMacroUnderCaret(e.getDataContext()) != null);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            performForContext(e.getDataContext());
        }

        @VisibleForTesting
        public void performForContext(DataContext e) {
            Project project = OpenApiUtil.getProject(e);
            if (project == null) return;
            Editor editor = OpenApiUtil.getEditor(e);
            if (editor == null) return;
            RsPossibleMacroCall macroToExpand = getMacroUnderCaret(e);
            if (macroToExpand == null) return;

            RsResult<MacroExpansionViewDetails, GetMacroExpansionError> expansionDetails = MacroExpansionViewUtils.expandMacroForViewWithProgress(project, macroToExpand, expandRecursively);
            if (expansionDetails instanceof RsResult.Ok ok) {
                showExpansion(project, editor, (MacroExpansionViewDetails) ok.getOk());
            } else if (expansionDetails instanceof RsResult.Err err) {
                showError(editor, (GetMacroExpansionError) err.getErr());
            }
        }

        @VisibleForTesting
        protected void showExpansion(Project project, Editor editor, MacroExpansionViewDetails expansionDetails) {
            MacroExpansionViewUtils.showMacroExpansionPopup(project, editor, expansionDetails);
        }

        @VisibleForTesting
        protected void showError(Editor editor, GetMacroExpansionError error) {
            showMacroExpansionError(editor, error);
        }

        public static void showMacroExpansionError(Editor editor, GetMacroExpansionError error) {
            org.rust.openapiext.Editor.showErrorHint(editor, RsBundle.message("macro.expansion.error.start", error.toUserViewableMessage()));
        }
    }

    /** @deprecated Use {@link org.rust.ide.actions.macroExpansion.RsShowRecursiveMacroExpansionAction} directly. */
    @Deprecated
    public static class RsShowRecursiveMacroExpansionAction extends org.rust.ide.actions.macroExpansion.RsShowRecursiveMacroExpansionAction {
    }

    /** @deprecated Use {@link org.rust.ide.actions.macroExpansion.RsShowSingleStepMacroExpansionAction} directly. */
    @Deprecated
    public static class RsShowSingleStepMacroExpansionAction extends org.rust.ide.actions.macroExpansion.RsShowSingleStepMacroExpansionAction {
    }

    /** Returns closest macro call under cursor in the editor if present. */
    public static RsPossibleMacroCall getMacroUnderCaret(DataContext event) {
        com.intellij.psi.PsiElement elementUnderCaret = OpenApiUtil.getElementUnderCaretInEditor(event);
        if (elementUnderCaret == null) return null;
        return RsPossibleMacroCallUtil.getContextMacroCall(elementUnderCaret);
    }
}
