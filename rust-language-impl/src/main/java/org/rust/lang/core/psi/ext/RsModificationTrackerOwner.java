/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.component.util.ModificationTracker;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

/**
 *
 * A PSI element that holds modification tracker for some reason.
 * This is mostly used to invalidate cached type inference results.
 */
public interface RsModificationTrackerOwner extends RsElement {
    @Nonnull
    ModificationTracker getModificationTracker();

    /**
     * Increments local modification counter if needed.
     *
     * If and only if false returned,
     * {@link org.rust.lang.core.psi.RsPsiManager#getRustStructureModificationTracker()}
     * will be incremented.
     *
     * @param element the changed psi element
     * @see org.rust.lang.core.psi.impl.RsPsiManagerImpl#updateModificationCount
     */
    boolean incModificationCount(@Nonnull PsiElement element);
}
