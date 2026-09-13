/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.util.dataholder.UserDataHolderEx;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Base interface for all Rust PSI elements.
 */
public interface RsElement extends PsiElement, UserDataHolderEx {
    /**
     * Find parent module *in this file*.
     */
    @Nonnull
    RsMod getContainingMod();

    /** The crate root module, or {@code null} when this element is not attached to a crate. */
    @Nullable
    RsMod getCrateRoot();

    /** The crate this element belongs to. */
    @Nonnull
    org.rust.lang.core.crate.Crate getContainingCrate();
}
