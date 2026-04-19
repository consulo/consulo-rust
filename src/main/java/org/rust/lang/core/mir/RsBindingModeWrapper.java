/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsBindingMode;
import org.rust.lang.core.types.ty.Mutability;

/**
 * This class exists because in case of `let x = 3` there is no binding mode created in PSI.
 */
public final class RsBindingModeWrapper {
    @Nullable
    private final RsBindingMode bindingMode;

    public RsBindingModeWrapper(@Nullable RsBindingMode bindingMode) {
        this.bindingMode = bindingMode;
    }

    @Nullable
    public PsiElement getMut() {
        return bindingMode != null ? bindingMode.getMut() : null;
    }

    @Nullable
    public PsiElement getRef() {
        return bindingMode != null ? bindingMode.getRef() : null;
    }

    @NotNull
    public Mutability getMutability() {
        return getMut() == null ? Mutability.IMMUTABLE : Mutability.MUTABLE;
    }

    /**
     * Extension-like static method to wrap a nullable RsBindingMode.
     */
    @NotNull
    public static RsBindingModeWrapper wrapper(@Nullable RsBindingMode bindingMode) {
        return new RsBindingModeWrapper(bindingMode);
    }
}
