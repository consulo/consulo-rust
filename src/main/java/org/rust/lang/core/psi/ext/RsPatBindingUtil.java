/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.ty.Mutability;

public final class RsPatBindingUtil {
    private RsPatBindingUtil() {
    }

    @NotNull
    public static Mutability getMutability(@NotNull RsPatBinding binding) {
        RsBindingModeKind kind = getKind(binding);
        if (kind instanceof RsBindingModeKind.BindByValue) {
            return ((RsBindingModeKind.BindByValue) kind).getMutability();
        }
        return Mutability.IMMUTABLE;
    }

    public static boolean isArg(@NotNull RsPatBinding binding) {
        PsiElement parent = binding.getParent();
        return parent != null && parent.getParent() instanceof RsValueParameter;
    }

    @NotNull
    public static RsBindingModeKind getKind(@NotNull RsPatBinding binding) {
        RsBindingMode bindingMode = binding.getBindingMode();
        boolean ref = bindingMode != null && bindingMode.getRef() != null;
        Mutability mutability = Mutability.valueOf(bindingMode != null && bindingMode.getMut() != null);
        return ref
            ? new RsBindingModeKind.BindByReference(mutability)
            : new RsBindingModeKind.BindByValue(mutability);
    }

    @NotNull
    public static RsPat getTopLevelPattern(@NotNull RsPatBinding binding) {
        PsiElement current = binding;
        RsPat lastPat = null;
        while (current != null) {
            if (current instanceof RsPat) {
                lastPat = (RsPat) current;
            } else if (!(current instanceof RsPatField)) {
                break;
            }
            current = current.getParent();
        }
        // Now walk up filtering for RsPat
        PsiElement parent = binding;
        while (parent != null) {
            if (parent instanceof RsPat || parent instanceof RsPatField) {
                parent = parent.getParent();
                continue;
            }
            break;
        }
        // parent is the first non-Pat/PatField ancestor
        // The last RsPat before that is the top-level pattern
        // Re-implement: walk ancestors, drop while (isPat || isPatField), then filter isPat, take last
        PsiElement el = binding;
        RsPat topLevel = null;
        boolean droppedInitial = false;
        while (el != null) {
            if (!droppedInitial) {
                if (el instanceof RsPat || el instanceof RsPatField) {
                    el = el.getParent();
                    continue;
                }
                droppedInitial = true;
            }
            if (el instanceof RsPat) {
                topLevel = (RsPat) el;
            }
            el = el.getParent();
        }
        if (topLevel != null) return topLevel;
        throw new IllegalStateException("Binding outside the pattern: `" + binding.getText() + "`");
    }

    public static boolean isReferenceToConstant(@NotNull RsPatBinding binding) {
        PsiElement resolved = binding.getReference().resolve();
        return resolved != null && isConstantLike(resolved);
    }

    private static boolean isConstantLike(@NotNull PsiElement element) {
        return RsElementUtil.isConstantLike(element);
    }
}
