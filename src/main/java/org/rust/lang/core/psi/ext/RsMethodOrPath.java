/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsTypeArgumentList;

public interface RsMethodOrPath extends RsElement {
    @Nullable
    RsTypeArgumentList getTypeArgumentList();
}
