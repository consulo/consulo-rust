/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.PsiElementUtil;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.application.util.CachedValueProvider;
import consulo.language.psi.PsiModificationTracker;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RsInferenceContextOwnerUtil {
    private RsInferenceContextOwnerUtil() {
    }

    @Nullable
    public static RsElement getBody(@Nonnull RsInferenceContextOwner owner) {
        if (owner instanceof RsArrayType) return ((RsArrayType) owner).getExpr();
        if (owner instanceof RsConstant) return ((RsConstant) owner).getExpr();
        if (owner instanceof RsConstParameter) return ((RsConstParameter) owner).getExpr();
        if (owner instanceof RsFunction) return RsFunctionUtil.getBlock((RsFunction) owner);
        if (owner instanceof RsVariantDiscriminant) return ((RsVariantDiscriminant) owner).getExpr();
        if (owner instanceof RsExpressionCodeFragment) return ((RsExpressionCodeFragment) owner).getExpr();
        if (owner instanceof RsReplCodeFragment) return (RsReplCodeFragment) owner;
        if (owner instanceof RsPathCodeFragment) return (RsPathCodeFragment) owner;
        if (owner instanceof RsPath) return ((RsPath) owner).getTypeArgumentList();
        if (owner instanceof RsDefaultParameterValue) return ((RsDefaultParameterValue) owner).getExpr();
        return null;
    }

    @Nonnull
    public static <T> CachedValueProvider.Result<T> createCachedResult(@Nonnull RsInferenceContextOwner owner,
                                                                       @Nonnull T value) {
        consulo.component.util.ModificationTracker structureModTracker =
            org.rust.lang.core.psi.RsPsiUtilUtil.getRustStructureModificationTracker(owner.getProject());

        // Injected language case
        if (owner.getContainingFile().getVirtualFile() instanceof VirtualFileWindow) {
            return CachedValueProvider.Result.create(value, PsiModificationTracker.MODIFICATION_COUNT);
        }

        // Code fragment
        if (owner instanceof RsCodeFragment) {
            return CachedValueProvider.Result.create(value, PsiModificationTracker.MODIFICATION_COUNT);
        }

        // Normal case
        RsModificationTrackerOwner trackerOwner = PsiElementUtil.contextOrSelf(owner, RsModificationTrackerOwner.class);
        List<Object> deps = new ArrayList<>();
        deps.add(structureModTracker);
        if (trackerOwner != null) deps.add(trackerOwner.getModificationTracker());
        return CachedValueProvider.Result.create(value, deps);
    }
}
