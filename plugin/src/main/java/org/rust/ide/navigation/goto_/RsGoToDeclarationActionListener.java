/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.navigation.goto_;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.codeInsight.navigation.actions.GotoDeclarationAction;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.event.AnActionListener;
import jakarta.annotation.Nonnull;

/**
 * Records whether {@link GotoDeclarationAction} is currently running, so that resolve can adapt to it.
 */
@TopicImpl(ComponentScope.APPLICATION)
public class RsGoToDeclarationActionListener implements AnActionListener {

    @Override
    public void beforeActionPerformed(@Nonnull AnAction action, @Nonnull DataContext dataContext, @Nonnull AnActionEvent event) {
        if (action instanceof GotoDeclarationAction) {
            RsGoToDeclarationRunningService.getInstance().setGoToDeclarationAction(true);
        }
    }

    @Override
    public void afterActionPerformed(@Nonnull AnAction action, @Nonnull DataContext dataContext, @Nonnull AnActionEvent event) {
        if (action instanceof GotoDeclarationAction) {
            RsGoToDeclarationRunningService.getInstance().setGoToDeclarationAction(false);
        }
    }
}
