/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import org.rust.lang.core.resolve.KnownItems;

/**
 * Bridge class delegating to {@link TyUtil}.
 */
public final class TyUtilUtil {
    private TyUtilUtil() {
    }

    public static Ty builtinIndex(@NotNull Ty ty) {
        return TyUtil.builtinIndex(ty);
    }

    public static Pair<Ty, Mutability> builtinDeref(@NotNull Ty ty, @Nullable KnownItems items, boolean explicit) {
        return TyUtil.builtinDeref(ty, items, explicit);
    }

    public static Pair<Ty, Mutability> builtinDeref(@NotNull Ty ty, @Nullable KnownItems items) {
        return TyUtil.builtinDeref(ty, items);
    }
}
