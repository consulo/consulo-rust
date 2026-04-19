/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.navigation.goto_;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.AnActionResult;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressWindow;
import org.jetbrains.annotations.NotNull;
import org.rust.openapiext.OpenApiUtil;

/**
 * A hack that let us know whether {@link GotoDeclarationAction} is now executes or not
 */
@Service
public final class RsGoToDeclarationRunningService {

    private volatile boolean _isGoToDeclarationAction = false;

    public boolean isGoToDeclarationAction() {
        return _isGoToDeclarationAction
            && (OpenApiUtil.isDispatchThread()
                || ProgressManager.getGlobalProgressIndicator() instanceof ProgressWindow);
    }

    @NotNull
    public static RsGoToDeclarationRunningService getInstance() {
        return ApplicationManager.getApplication().getService(RsGoToDeclarationRunningService.class);
    }

    @SuppressWarnings("unused")
    public static class Listener implements AnActionListener {
        @Override
        public void beforeActionPerformed(@NotNull AnAction action, @NotNull AnActionEvent event) {
            if (action instanceof GotoDeclarationAction) {
                getInstance()._isGoToDeclarationAction = true;
            }
        }

        @Override
        public void afterActionPerformed(@NotNull AnAction action, @NotNull AnActionEvent event, @NotNull AnActionResult result) {
            if (action instanceof GotoDeclarationAction) {
                getInstance()._isGoToDeclarationAction = false;
            }
        }
    }
}
