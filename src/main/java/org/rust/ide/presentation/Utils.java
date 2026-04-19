/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.presentation;

import com.intellij.navigation.ItemPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwner;
import org.rust.lang.core.psi.ext.RsElement;

/**
 * <p>
 * Contains utility methods for PSI element presentation.
 */
public final class Utils {

    private Utils() {
    }

    /**
     * Returns the presentation for a Rust PSI element, including name, location, and icon.
     */
    @NotNull
    public static ItemPresentation getPresentation(@NotNull RsElement psi) {
        return PresentationUtils.getPresentation(psi);
    }

    /**
     * Returns the presentation for a Rust PSI element in the structure view,
     * including parameter types, return types, and field types.
     */
    @NotNull
    public static ItemPresentation getPresentationForStructure(@NotNull RsElement psi) {
        return PresentationUtils.getPresentationForStructure(psi);
    }

    /**
     * Extension property equivalent: returns the presentable qualified name for a
     * {@link RsDocAndAttributeOwner}.
     */
    @Nullable
    public static String getPresentableQualifiedName(@NotNull RsDocAndAttributeOwner element) {
        return PresentationUtils.getPresentableQualifiedName(element);
    }
}
