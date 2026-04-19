/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.createFromUsage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;

public final class CreateFromUsageUtils {
    private CreateFromUsageUtils() {
    }

    @NotNull
    public static String getVisibility(@NotNull RsMod target, @NotNull RsMod source) {
        if (!RsElementUtil.getContainingCrate((RsElement) source).equals(RsElementUtil.getContainingCrate((RsElement) target))) {
            return "pub ";
        }
        if (!source.equals(target)) {
            return "pub(crate) ";
        }
        return "";
    }

    @Nullable
    private static RsElement getWritablePathTarget(@NotNull RsPath path) {
        RsPath qualifier = RsPathUtil.getQualifier(path);
        if (qualifier == null) return null;
        return (RsElement) qualifier.getReference().resolve();
    }

    @Nullable
    public static RsMod getWritablePathMod(@NotNull RsPath path) {
        if (RsPathUtil.getQualifier(path) == null) return RsElementUtil.getContainingMod((RsElement) path);
        RsElement target = getWritablePathTarget(path);
        if (target instanceof RsMod) {
            return (RsMod) target;
        }
        return null;
    }

    @Nullable
    public static RsElement getTargetItemForFunctionCall(@NotNull RsPath path) {
        RsPath qualifier = RsPathUtil.getQualifier(path);
        if (qualifier == null) return RsElementUtil.getContainingMod((RsElement) path);

        if (RsPathUtil.getHasCself(qualifier) && !RsPathUtil.getHasColonColon(qualifier)) {
            com.intellij.psi.PsiElement resolved = qualifier.getReference().resolve();
            if (resolved instanceof RsImplItem) {
                RsImplItem impl = (RsImplItem) resolved;
                if (RsElementUtil.isContextOf(impl, path)) {
                    return impl;
                }
            }
        }
        return getWritablePathTarget(path);
    }
}
