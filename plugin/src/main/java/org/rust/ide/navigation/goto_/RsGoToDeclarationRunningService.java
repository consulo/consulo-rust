/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.navigation.goto_;

import consulo.ide.impl.idea.codeInsight.navigation.actions.GotoDeclarationAction;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import com.intellij.openapi.actionSystem.AnActionResult;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import consulo.application.progress.ProgressManager;
import jakarta.annotation.Nonnull;
import org.rust.openapiext.OpenApiUtil;

/**
 * A hack that let us know whether {@link GotoDeclarationAction} is now executes or not
 */
@Service
public final class RsGoToDeclarationRunningService {

    private volatile boolean _isGoToDeclarationAction = false;

    public boolean isGoToDeclarationAction() {
        // Upstream also accepted a ProgressWindow-backed indicator here; that class is
        // platform-internal, so only the dispatch-thread case can be recognised.
        return _isGoToDeclarationAction && OpenApiUtil.isDispatchThread();
    }

    @Nonnull
    public static RsGoToDeclarationRunningService getInstance() {
        return ApplicationManager.getApplication().getService(RsGoToDeclarationRunningService.class);
    }

    @SuppressWarnings("unused")
    public static class Listener implements AnActionListener {
        public void beforeActionPerformed(@Nonnull AnAction action, @Nonnull AnActionEvent event) {
            if (action instanceof GotoDeclarationAction) {
                getInstance()._isGoToDeclarationAction = true;
            }
        }

        public void afterActionPerformed(@Nonnull AnAction action, @Nonnull AnActionEvent event, @Nonnull AnActionResult result) {
            if (action instanceof GotoDeclarationAction) {
                getInstance()._isGoToDeclarationAction = false;
            }
        }
    }
}
