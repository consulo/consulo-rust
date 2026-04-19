/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.Nullable;

public interface RsQualifiedNamedElement extends RsNamedElement {
    @Nullable
    String getCrateRelativePath();

    @Nullable
    default String getQualifiedName() {
        return RsQualifiedNamedElementUtil.getQualifiedName(this);
    }

    @Nullable
    default String qualifiedName() {
        return getQualifiedName();
    }

    @Nullable
    default String getQualifiedNameRelativeTo(RsMod context) {
        return RsQualifiedNamedElementUtil.qualifiedNameRelativeTo(this, context);
    }

    /**
     * Returns the qualified name of this element within the crate that {@code context} belongs to.
     * Falls back to crateRelativePath prefixed with "crate" if specific resolution is unavailable.
     */
    @Nullable
    default String getQualifiedNameInCrate(RsMod context) {
        // Try relative first
        String relative = getQualifiedNameRelativeTo(context);
        if (relative != null) return relative;
        // Fall back to crate-relative path
        String crateRelative = getCrateRelativePath();
        if (crateRelative != null) return "crate" + crateRelative;
        return null;
    }
}
