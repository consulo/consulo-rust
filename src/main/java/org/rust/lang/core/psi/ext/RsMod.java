/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RsMod extends RsQualifiedNamedElement, RsItemsOwner, RsVisible, RsDocAndAttributeOwner {
    /**
     * Returns a parent module ({@code super::} in paths).
     * The parent module may be in the same or other file.
     */
    @Nullable
    RsMod getSuper();

    /**
     * This might be different than {@link com.intellij.psi.PsiNamedElement#getName()}.
     */
    @Nullable
    String getModName();

    /**
     * Returns value of {@code path} attribute related to this module.
     * If module doesn't have a {@code path} attribute, returns null.
     */
    @Nullable
    String getPathAttribute();

    boolean getOwnsDirectory();

    /**
     * Returns directory where direct submodules should be located.
     */
    @Nullable
    default PsiDirectory getOwnedDirectory(boolean createIfNotExists) {
        return RsModUtil.getOwnedDirectory(this, createIfNotExists);
    }

    @Nullable
    default PsiDirectory getOwnedDirectory() {
        return getOwnedDirectory(false);
    }

    boolean isCrateRoot();

    @NotNull
    default java.util.List<RsMod> getSuperMods() {
        return RsModExtUtil.getSuperMods(this);
    }
}
