/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.RsVisibility;

public interface RsVisible extends RsElement {
    RsVisibility getVisibility();

    boolean isPublic();

    /**
     * Checks whether this element is accessible from {@code mod}, taking the element's
     * visibility, the enclosing module chain and trait/impl membership into account.
     */
    default boolean isVisibleFrom(@jakarta.annotation.Nonnull RsMod mod) {
        return RsPsiSupport.getInstance().isVisibleFrom(this, mod);
    }
}
