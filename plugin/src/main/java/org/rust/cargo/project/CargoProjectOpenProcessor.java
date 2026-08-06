/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project;

import consulo.project.Project;
import consulo.project.ProjectOpenContext;
import consulo.project.internal.ProjectOpenProcessor;
import consulo.project.startup.StartupManager;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.icons.CargoIcons;
import org.rust.cargo.project.model.CargoProjectServiceUtil;

import java.io.File;

public class CargoProjectOpenProcessor extends ProjectOpenProcessor {

    @Nonnull
    @Override
    public Image getIcon(@Nonnull VirtualFile file) {
        return CargoIcons.ICON;
    }

    @Override
    public boolean canOpenProject(@Nonnull File file) {
        if (file.isFile() && CargoConstants.MANIFEST_FILE.equalsIgnoreCase(file.getName())) return true;
        if (file.isDirectory()) return new File(file, CargoConstants.MANIFEST_FILE).isFile();
        return false;
    }

    @Nonnull
    @Override
    public AsyncResult<Project> doOpenProjectAsync(@Nonnull VirtualFile virtualFile,
                                                   @Nonnull UIAccess uiAccess,
                                                   @Nonnull ProjectOpenContext context) {
        VirtualFile basedir = virtualFile.isDirectory() ? virtualFile : virtualFile.getParent();
        AsyncResult<Project> result = AsyncResult.undefined();
        // Delegate to default project-open via context; stub-level impl just resolves with null.
        // Full behaviour would fire PlatformProjectOpenProcessor.openExistingProject(basedir, uiAccess, context).
        result.setDone((Project) null);
        result.doWhenDone(project -> {
            if (project != null) {
                StartupManager.getInstance(project).runWhenProjectIsInitialized(
                    () -> CargoProjectServiceUtil.guessAndSetupRustProject(project, false));
            }
        });
        return result;
    }
}
