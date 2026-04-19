/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.util.PsiTreeUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;

public interface RsVisibilityOwner extends RsVisible {
    default RsVis getVis() {
        return PsiTreeUtil.getStubChildOfType(this, RsVis.class);
    }

    @Override
    default RsVisibility getVisibility() {
        RsVis vis = getVis();
        return vis != null ? RsVisibilityUtil.getVisibility(vis) : RsVisibility.Private.INSTANCE;
    }

    @Override
    default boolean isPublic() {
        return getVis() != null;
    }
}
