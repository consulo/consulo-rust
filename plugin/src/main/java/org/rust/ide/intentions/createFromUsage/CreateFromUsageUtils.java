/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.createFromUsage;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import consulo.language.psi.PsiElement;

public final class CreateFromUsageUtils {
    private CreateFromUsageUtils() {
    }

    @Nonnull
    public static String getVisibility(@Nonnull RsMod target, @Nonnull RsMod source) {
        if (!RsElementUtil.getContainingCrate((RsElement) source).equals(RsElementUtil.getContainingCrate((RsElement) target))) {
            return "pub ";
        }
        if (!source.equals(target)) {
            return "pub(crate) ";
        }
        return "";
    }

    @Nullable
    private static RsElement getWritablePathTarget(@Nonnull RsPath path) {
        RsPath qualifier = RsPathUtil.getQualifier(path);
        if (qualifier == null) return null;
        return (RsElement) qualifier.getReference().resolve();
    }

    @Nullable
    public static RsMod getWritablePathMod(@Nonnull RsPath path) {
        if (RsPathUtil.getQualifier(path) == null) return RsElementUtil.getContainingMod((RsElement) path);
        RsElement target = getWritablePathTarget(path);
        if (target instanceof RsMod) {
            return (RsMod) target;
        }
        return null;
    }

    @Nullable
    public static RsElement getTargetItemForFunctionCall(@Nonnull RsPath path) {
        RsPath qualifier = RsPathUtil.getQualifier(path);
        if (qualifier == null) return RsElementUtil.getContainingMod((RsElement) path);

        if (RsPathUtil.getHasCself(qualifier) && !RsPathUtil.getHasColonColon(qualifier)) {
            consulo.language.psi.PsiElement resolved = qualifier.getReference().resolve();
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
