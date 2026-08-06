/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.RsMacro;
import org.rust.lang.core.psi.RsMacroCall;

public final class RsModificationTrackerOwnerUtil {
    private RsModificationTrackerOwnerUtil() {
    }

    @Nullable
    public static RsModificationTrackerOwner findModificationTrackerOwner(@Nonnull PsiElement element, boolean strict) {
        PsiElement current = strict ? getContextWithoutIndexAccess(element) : element;
        while (current != null) {
            if (current instanceof RsItemElement || current instanceof RsMacroCall || current instanceof RsMacro) {
                if (current instanceof RsModificationTrackerOwner) {
                    return (RsModificationTrackerOwner) current;
                }
            }
            current = getContextWithoutIndexAccess(current);
        }
        return null;
    }

    @Nullable
    private static PsiElement getContextWithoutIndexAccess(@Nonnull PsiElement element) {
        if (element instanceof RsExpandedElement) {
            return RsExpandedElement.getContextImpl((RsExpandedElement) element, true);
        }
        return RsElementUtil.getStubParent(element);
    }
}
