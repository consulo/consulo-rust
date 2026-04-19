/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsElement;

/**
 * Delegates to {@link RsRawIdentifiers} and {@link RsPsiManagerKt}.
 */
public final class RsPsiUtilUtil {
    private RsPsiUtilUtil() {
    }

    @NotNull
    public static String escapeIdentifierIfNeeded(@NotNull String s) {
        return RsRawIdentifiers.escapeIdentifierIfNeeded(s);
    }

    @NotNull
    public static String getUnescapedText(@NotNull PsiElement element) {
        return RsRawIdentifiers.getUnescapedText(element);
    }

    @NotNull
    public static ModificationTracker getRustStructureModificationTracker(@NotNull Project project) {
        return RsPsiManagerUtil.getRustStructureModificationTracker(project);
    }

    @NotNull
    public static ModificationTracker getRustStructureOrAnyPsiModificationTracker(@NotNull RsElement element) {
        return RsPsiManagerUtil.getRustStructureOrAnyPsiModificationTracker(element);
    }
}
