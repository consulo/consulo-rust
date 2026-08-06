/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Bridge class delegating to {@link RunConfigUtil}.
 */
public final class RsRunConfigurationUtil {
    private RsRunConfigurationUtil() {
    }

    public static boolean hasCargoProject(@Nonnull Project project) {
        return RunConfigUtil.hasCargoProject(project);
    }
}
