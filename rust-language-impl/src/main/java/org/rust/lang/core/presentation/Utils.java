/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.presentation;

import consulo.navigation.ItemPresentation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwner;
import org.rust.lang.core.psi.ext.RsElement;

/**
 * Contains utility methods for PSI element presentation.
 */
public final class Utils {

    private Utils() {
    }

    /**
     * Returns the presentation for a Rust PSI element, including name, location, and icon.
     */
    @Nonnull
    public static ItemPresentation getPresentation(@Nonnull RsElement psi) {
        return PresentationUtils.getPresentation(psi);
    }

    /**
     * Returns the presentation for a Rust PSI element in the structure view,
     * including parameter types, return types, and field types.
     */
    @Nonnull
    public static ItemPresentation getPresentationForStructure(@Nonnull RsElement psi) {
        return PresentationUtils.getPresentationForStructure(psi);
    }

    /**
     * Extension property equivalent: returns the presentable qualified name for a
     * {@link RsDocAndAttributeOwner}.
     */
    @Nullable
    public static String getPresentableQualifiedName(@Nonnull RsDocAndAttributeOwner element) {
        return PresentationUtils.getPresentableQualifiedName(element);
    }
}
