/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

public interface RsVisible extends RsElement {
    RsVisibility getVisibility();

    boolean isPublic();

    /**
     * Check if this visible element is accessible from the given module.
     * Default implementation: public elements are always visible, others only from the same module.
     */
    default boolean isVisibleFrom(@org.jetbrains.annotations.NotNull RsMod mod) {
        if (isPublic()) return true;
        RsVisibility visibility = getVisibility();
        if (visibility instanceof RsVisibility.Private) {
            // Private: visible only from the containing module
            return getContainingMod() == mod || mod.getSuperMods().contains(getContainingMod());
        }
        if (visibility instanceof RsVisibility.Restricted) {
            // Restricted: visible within the restriction scope
            return true; // Simplified: assume restricted visibility is accessible
        }
        return false;
    }
}
