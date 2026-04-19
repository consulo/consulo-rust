/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.PsiElementUtil;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsFile;

/**
 * Base interface for all Rust PSI elements.
 */
public interface RsElement extends PsiElement, UserDataHolderEx {
    /**
     * Find parent module *in this file*.
     */
    @NotNull
    RsMod getContainingMod();

    /**
     * Get the crate root module.
     */
    @Nullable
    default RsMod getCrateRoot() {
        RsFile file = PsiElementUtil.getContainingRsFileSkippingCodeFragments(this);
        return file != null ? file.getCrateRoot() : null;
    }

    @Nullable
    default org.rust.lang.core.crate.Crate getContainingCrate() {
        return RsElementUtil.getContainingCrate(this);
    }
}
