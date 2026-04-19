/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.stubs.RsModDeclItemStub;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class RsModDeclItemUtil {
    private RsModDeclItemUtil() {
    }

    @Nullable
    public static PsiFile getOrCreateModuleFile(@NotNull RsModDeclItem decl) {
        PsiFile existing = decl.getReference().resolve() != null
            ? decl.getReference().resolve().getContainingFile()
            : null;
        if (existing != null) return existing;
        String fileName = getSuggestChildFileName(decl);
        if (fileName == null) return null;
        com.intellij.psi.PsiDirectory dir = decl.getContainingMod().getOwnedDirectory(true);
        if (dir == null) return null;
        return dir.createFile(fileName);
    }

    public static boolean isLocal(@NotNull RsModDeclItem decl) {
        return RsPsiJavaUtil.ancestorStrict(decl, org.rust.lang.core.psi.RsBlock.class) != null;
    }

    @Nullable
    public static String getPathAttribute(@NotNull RsModDeclItem decl) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(decl).lookupStringValueForKey("path");
    }

    public static boolean getHasMacroUse(@NotNull RsModDeclItem decl) {
        return MOD_DECL_HAS_MACRO_USE_PROP.getByPsi(decl);
    }

    @NotNull
    public static final StubbedAttributeProperty<RsModDeclItem, RsModDeclItemStub> MOD_DECL_HAS_MACRO_USE_PROP =
        new StubbedAttributeProperty<>(qa -> qa.hasAttribute("macro_use"), RsModDeclItemStub::getMayHaveMacroUse);

    @Nullable
    private static String getSuggestChildFileName(@NotNull RsModDeclItem decl) {
        List<String> paths = getImplicitPaths(decl);
        return paths.isEmpty() ? null : paths.get(0);
    }

    @NotNull
    private static List<String> getImplicitPaths(@NotNull RsModDeclItem decl) {
        String name = decl.getName();
        if (name == null) return Collections.emptyList();
        if (isLocal(decl)) return Collections.emptyList();
        return Arrays.asList(name + ".rs", name + "/mod.rs");
    }
}
