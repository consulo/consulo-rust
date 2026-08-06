/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.project.Project;
import consulo.component.util.ModificationTracker;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsElement;

/**
 * Delegates to {@link RsRawIdentifiers} and {@link RsPsiManagerKt}.
 */
public final class RsPsiUtilUtil {
    private RsPsiUtilUtil() {
    }

    @Nonnull
    public static String escapeIdentifierIfNeeded(@Nonnull String s) {
        return RsRawIdentifiers.escapeIdentifierIfNeeded(s);
    }

    @Nonnull
    public static String getUnescapedText(@Nonnull PsiElement element) {
        return RsRawIdentifiers.getUnescapedText(element);
    }

    @Nonnull
    public static ModificationTracker getRustStructureModificationTracker(@Nonnull Project project) {
        return RsPsiManagerUtil.getRustStructureModificationTracker(project);
    }

    @Nonnull
    public static ModificationTracker getRustStructureOrAnyPsiModificationTracker(@Nonnull RsElement element) {
        return RsPsiManagerUtil.getRustStructureOrAnyPsiModificationTracker(element);
    }
}
