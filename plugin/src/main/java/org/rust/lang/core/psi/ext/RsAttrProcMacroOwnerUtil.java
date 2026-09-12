/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.stubs.RsAttributeOwnerStub;
import org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub;
import org.rust.lang.core.psi.ProcMacroAttribute;

/**
 * Delegates to {@link RsAttrProcMacroOwnerUtil} for the actual implementations.
 */
public final class RsAttrProcMacroOwnerUtil {

    private RsAttrProcMacroOwnerUtil() {
    }

    /**
     * Type alias for {@link org.rust.lang.core.psi.ProcMacroAttribute}.
     */
    public static abstract class ProcMacroAttribute<T extends RsMetaItemPsiOrStub>
        extends org.rust.lang.core.psi.ProcMacroAttribute<T> {
    }

    /**
     * Returns the proc macro attribute for the given element.
     */
    @Nullable
    public static org.rust.lang.core.psi.ProcMacroAttribute<RsMetaItem> getProcMacroAttribute(
        @Nonnull RsAttrProcMacroOwner owner) {
        return org.rust.lang.core.psi.ProcMacroAttribute.getProcMacroAttribute(owner);
    }

    /**
     * Returns the proc macro attribute for the given element (with derives).
     */
    @Nullable
    public static org.rust.lang.core.psi.ProcMacroAttribute<RsMetaItem> getProcMacroAttributeWithDerives(
        @Nonnull RsAttrProcMacroOwner owner) {
        return org.rust.lang.core.psi.ProcMacroAttribute.getProcMacroAttribute(
            owner,
            RsDocAndAttributeOwnerUtil.getAttributeStub(owner),
            null,
            true,
            false);
    }

    /**
     * Returns the nearest RsAttrProcMacroOwner ancestor (including self).
     */
    @Nullable
    public static PsiElement getOwner(@Nonnull PsiElement elem) {
        PsiElement current = elem;
        while (current != null) {
            if (current instanceof RsAttrProcMacroOwner) return current;
            current = current.getParent();
        }
        return null;
    }

    /**
     * Returns query attributes for the given doc/attribute owner.
     */
    @Nonnull
    public static QueryAttributes<RsMetaItem> getQueryAttributes(
        @Nonnull RsDocAndAttributeOwner owner,
        @Nullable Crate crate,
        @Nullable RsAttributeOwnerStub stub) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(owner, crate, stub, false);
    }
}
