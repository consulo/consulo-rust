/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsMacroBinding;
import org.rust.lang.core.psi.ext.*;

public final class RsMacroBindingUtil {
    private RsMacroBindingUtil() {
    }

    @Nullable
    public static String getFragmentSpecifier(@Nonnull RsMacroBinding binding) {
        PsiElement identifier = binding.getIdentifier();
        return identifier != null ? identifier.getText() : null;
    }
}
