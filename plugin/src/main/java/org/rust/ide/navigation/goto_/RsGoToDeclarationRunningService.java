/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.navigation.goto_;

import consulo.ide.impl.idea.codeInsight.navigation.actions.GotoDeclarationAction;
import consulo.application.ApplicationManager;
import jakarta.annotation.Nonnull;
import org.rust.openapiext.OpenApiUtil;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;

/**
 * A hack that let us know whether {@link GotoDeclarationAction} is now executes or not
 */
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public final class RsGoToDeclarationRunningService {

    private volatile boolean _isGoToDeclarationAction = false;

    public boolean isGoToDeclarationAction() {
        return _isGoToDeclarationAction && OpenApiUtil.isDispatchThread();
    }

    void setGoToDeclarationAction(boolean running) {
        _isGoToDeclarationAction = running;
    }

    @Nonnull
    public static RsGoToDeclarationRunningService getInstance() {
        return ApplicationManager.getApplication().getService(RsGoToDeclarationRunningService.class);
    }

}
