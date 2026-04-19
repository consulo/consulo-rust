/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.resolve.ref.RsReference;

/**
 * Marks an element that optionally can have a reference.
 */
public interface RsReferenceElement extends RsReferenceElementBase {
    @Override
    @Nullable
    RsReference getReference();
}
