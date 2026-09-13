/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.actions.macroExpansion.MacroExpansionViewDetails;
import org.rust.ide.actions.macroExpansion.RsShowMacroExpansionActions;
import org.rust.lang.core.macros.MacroExpansionUtil;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.macros.errors.GetMacroExpansionError;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;
import org.rust.lang.core.psi.ext.impl.RsPossibleMacroCallKind;
import org.rust.lang.core.psi.ext.impl.RsPossibleMacroCallUtil;
import org.rust.lang.core.psi.ext.impl.PsiElementExt;
import org.rust.stdext.RsResult;
import org.rust.ide.actions.macroExpansion.MacroExpansionViewUtil;
import consulo.localize.LocalizeValue;
import org.rust.lang.core.macros.MacroExpansionExtUtil;

public abstract class RsShowMacroExpansionIntentionBase extends RsElementBaseIntentionAction<RsPossibleMacroCall> {

    protected abstract boolean getExpandRecursively();

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    @Nonnull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Nonnull
    @Override
    public InvokeInside getFunctionLikeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Nullable
    @Override
    public RsPossibleMacroCall findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        PsiElement possiblyExpandedElement = org.rust.lang.core.macros.MacroExpansionExtUtil.findExpansionElementOrSelf(element);
        RsPossibleMacroCall macroCall = RsPossibleMacroCallUtil.getContextMacroCall(possiblyExpandedElement);
        if (macroCall == null) return null;

        RsPossibleMacroCallKind kind = RsPossibleMacroCallUtil.getKind(macroCall);
        boolean isValidContext;
        if (kind instanceof RsPossibleMacroCallKind.MacroCall) {
            RsPossibleMacroCallKind.MacroCall macroCallKind = (RsPossibleMacroCallKind.MacroCall) kind;
            isValidContext = PsiElementExt.isContextOf(macroCallKind.call.getPath(), possiblyExpandedElement)
                || possiblyExpandedElement == macroCallKind.call.getExcl();
        } else {
            isValidContext = true;
        }
        if (!isValidContext) return null;
        return macroCall;
    }

    /** Progress window cannot be shown in the write action, so it have to be disabled. */
    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull RsPossibleMacroCall ctx) {
        RsResult<MacroExpansionViewDetails, GetMacroExpansionError> expansionDetails =
            MacroExpansionViewUtil.expandMacroForViewWithProgress(project, ctx, getExpandRecursively());
        if (expansionDetails instanceof RsResult.Ok) {
            showExpansion(project, editor, ((RsResult.Ok<MacroExpansionViewDetails, GetMacroExpansionError>) expansionDetails).getOk());
        } else if (expansionDetails instanceof RsResult.Err) {
            showError(editor, ((RsResult.Err<MacroExpansionViewDetails, GetMacroExpansionError>) expansionDetails).getErr());
        }
    }

    
    protected void showExpansion(@Nonnull Project project, @Nonnull Editor editor, @Nonnull MacroExpansionViewDetails expansionDetails) {
        MacroExpansionViewUtil.showMacroExpansionPopup(project, editor, expansionDetails);
    }

    private void showError(@Nonnull Editor editor, @Nonnull GetMacroExpansionError error) {
        RsShowMacroExpansionActions.RsShowMacroExpansionActionBase.showMacroExpansionError(editor, error);
    }
}
