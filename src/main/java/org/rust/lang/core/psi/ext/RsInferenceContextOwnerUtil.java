/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.PsiElementUtil;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RsInferenceContextOwnerUtil {
    private RsInferenceContextOwnerUtil() {
    }

    @Nullable
    public static RsElement getBody(@NotNull RsInferenceContextOwner owner) {
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

    @NotNull
    public static <T> CachedValueProvider.Result<T> createCachedResult(@NotNull RsInferenceContextOwner owner,
                                                                       @NotNull T value) {
        com.intellij.openapi.util.ModificationTracker structureModTracker =
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
