/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.notifications.NotificationUtils;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.crate.impl.FakeCrate;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.OpenApiUtil;

public class RsRebuildCurrentDefMapAction extends AnAction {
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(consulo.project.Project.KEY);
        if (project == null) return;
        consulo.language.psi.PsiFile psiFile = OpenApiUtil.getPsiFile(e.getDataContext());
        if (!(psiFile instanceof RsFile)) return;
        RsFile file = (RsFile) psiFile;
        Crate crate = file.getCrate();
        if (crate instanceof FakeCrate) return;
        Integer crateId = crate.getId();
        if (crateId == null) return;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            long start = System.currentTimeMillis();
            // project.forceRebuildDefMapForCrate(crateId);
            long time = System.currentTimeMillis() - start;
            NotificationUtils.setStatusBarText(
                project,
                RsBundle.message("status.bar.text.rebuilt.defmap.for.in.ms", crate, time)
            );
        });
    }
}
