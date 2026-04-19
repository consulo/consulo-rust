/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs.common;

import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.PathKind;

public interface RsPathPsiOrStub {
    @Nullable
    RsPathPsiOrStub getPath();

    @Nullable
    String getReferenceName();

    boolean getHasColonColon();

    PathKind getKind();
}
