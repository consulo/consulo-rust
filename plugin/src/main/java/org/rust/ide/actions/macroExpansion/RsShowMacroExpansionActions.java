/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.macroExpansion;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.ui.ex.action.LegacyAnAction;
import org.rust.RsBundle;
import org.rust.lang.core.macros.errors.GetMacroExpansionError;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;
import org.rust.lang.core.psi.ext.impl.RsPossibleMacroCallUtil;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.RsResult;
import consulo.language.psi.PsiElement;

public abstract class RsShowMacroExpansionActions {

    public static abstract class RsShowMacroExpansionActionBase extends LegacyAnAction {
        private final boolean expandRecursively;

        protected RsShowMacroExpansionActionBase(boolean expandRecursively) {
            this.expandRecursively = expandRecursively;
        }


        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(getMacroUnderCaret(e.getDataContext()) != null);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            performForContext(e.getDataContext());
        }

        
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

        
        protected void showExpansion(Project project, Editor editor, MacroExpansionViewDetails expansionDetails) {
            MacroExpansionViewUtils.showMacroExpansionPopup(project, editor, expansionDetails);
        }

        
        protected void showError(Editor editor, GetMacroExpansionError error) {
            showMacroExpansionError(editor, error);
        }

        public static void showMacroExpansionError(Editor editor, GetMacroExpansionError error) {
            org.rust.openapiext.ui.Editor.showErrorHint(editor, RsBundle.message("macro.expansion.error.start", error.toUserViewableMessage()));
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
        consulo.language.psi.PsiElement elementUnderCaret = OpenApiUtil.getElementUnderCaretInEditor(event);
        if (elementUnderCaret == null) return null;
        return RsPossibleMacroCallUtil.getContextMacroCall(elementUnderCaret);
    }
}
