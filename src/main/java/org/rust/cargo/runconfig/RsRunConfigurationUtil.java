/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Bridge class delegating to {@link RunConfigUtil}.
 */
public final class RsRunConfigurationUtil {
    private RsRunConfigurationUtil() {
    }

    public static boolean hasCargoProject(@NotNull Project project) {
        return RunConfigUtil.hasCargoProject(project);
    }
}
