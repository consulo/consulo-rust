/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.PlatformProjectOpenProcessor;
import com.intellij.projectImport.ProjectOpenProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.icons.CargoIcons;
import org.rust.cargo.project.model.CargoProjectServiceUtil;

import javax.swing.*;

public class CargoProjectOpenProcessor extends ProjectOpenProcessor {

    @NotNull
    @Override
    public Icon getIcon() {
        return CargoIcons.ICON;
    }

    @NotNull
    @Override
    public String getName() {
        return "Cargo";
    }

    @Override
    public boolean canOpenProject(@NotNull VirtualFile file) {
        return FileUtil.namesEqual(file.getName(), CargoConstants.MANIFEST_FILE) ||
            (file.isDirectory() && file.findChild(CargoConstants.MANIFEST_FILE) != null);
    }

    @Nullable
    @Override
    public Project doOpenProject(@NotNull VirtualFile virtualFile, @Nullable Project projectToClose, boolean forceNewFrame) {
        VirtualFile basedir = virtualFile.isDirectory() ? virtualFile : virtualFile.getParent();

        Project project = PlatformProjectOpenProcessor.getInstance().doOpenProject(basedir, projectToClose, forceNewFrame);
        if (project != null) {
            StartupManager.getInstance(project).runWhenProjectIsInitialized(() -> CargoProjectServiceUtil.guessAndSetupRustProject(project, false));
        }
        return project;
    }
}
