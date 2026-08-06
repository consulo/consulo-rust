package com.intellij.ide;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
/** IntelliJ-compat stub. */
public interface FileIconProvider {
    Image getIcon(VirtualFile file, int flags, Project project);
}
