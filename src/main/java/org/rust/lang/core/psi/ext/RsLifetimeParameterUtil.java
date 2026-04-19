/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsLifetime;
import org.rust.lang.core.psi.RsLifetimeParameter;
import org.rust.lang.core.psi.RsWherePred;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RsLifetimeParameterUtil {
    private RsLifetimeParameterUtil() {
    }

    @NotNull
    public static List<RsLifetime> getBounds(@NotNull RsLifetimeParameter param) {
        List<RsLifetime> result = new ArrayList<>();

        // Direct bounds from the parameter declaration
        if (param.getLifetimeParamBounds() != null) {
            result.addAll(param.getLifetimeParamBounds().getLifetimeList());
        }

        // Where clause bounds
        Object parent = param.getParent();
        if (parent != null) {
            Object grandParent = ((com.intellij.psi.PsiElement) parent).getParent();
            if (grandParent instanceof RsGenericDeclaration) {
                List<RsWherePred> preds = RsGenericDeclarationUtil.getWherePreds((RsGenericDeclaration) grandParent);
                for (RsWherePred pred : preds) {
                    if (pred.getLifetime() != null
                        && pred.getLifetime().getReference() != null
                        && param.equals(pred.getLifetime().getReference().resolve())) {
                        if (pred.getLifetimeParamBounds() != null) {
                            result.addAll(pred.getLifetimeParamBounds().getLifetimeList());
                        }
                    }
                }
            }
        }

        return result;
    }
}
