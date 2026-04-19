/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.RsPath;

/**
 *
 * A PSI element that can be a declarative or (function-like, derive or attribute) procedural macro call.
 * It is implemented by {@code RsMacroCall} and {@code RsMetaItem}. A <i>possible</i> macro call is a
 * <i>real</i> macro call if {@code isMacroCall} returns {@code true} for it.
 */
public interface RsPossibleMacroCall extends RsExpandedElement {
    @Nullable
    RsPath getPath();
}
