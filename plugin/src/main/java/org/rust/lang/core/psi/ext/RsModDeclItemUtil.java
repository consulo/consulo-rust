/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.stubs.RsModDeclItemStub;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class RsModDeclItemUtil {
    private RsModDeclItemUtil() {
    }

    @Nullable
    public static PsiFile getOrCreateModuleFile(@Nonnull RsModDeclItem decl) {
        PsiFile existing = decl.getReference().resolve() != null
            ? decl.getReference().resolve().getContainingFile()
            : null;
        if (existing != null) return existing;
        String fileName = getSuggestChildFileName(decl);
        if (fileName == null) return null;
        consulo.language.psi.PsiDirectory dir = decl.getContainingMod().getOwnedDirectory(true);
        if (dir == null) return null;
        return dir.createFile(fileName);
    }

    public static boolean isLocal(@Nonnull RsModDeclItem decl) {
        return RsPsiJavaUtil.ancestorStrict(decl, org.rust.lang.core.psi.RsBlock.class) != null;
    }

    @Nullable
    public static String getPathAttribute(@Nonnull RsModDeclItem decl) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(decl).lookupStringValueForKey("path");
    }

    public static boolean getHasMacroUse(@Nonnull RsModDeclItem decl) {
        return MOD_DECL_HAS_MACRO_USE_PROP.getByPsi(decl);
    }

    @Nonnull
    public static final StubbedAttributeProperty<RsModDeclItem, RsModDeclItemStub> MOD_DECL_HAS_MACRO_USE_PROP =
        new StubbedAttributeProperty<>(qa -> qa.hasAttribute("macro_use"), RsModDeclItemStub::getMayHaveMacroUse);

    @Nullable
    private static String getSuggestChildFileName(@Nonnull RsModDeclItem decl) {
        List<String> paths = getImplicitPaths(decl);
        return paths.isEmpty() ? null : paths.get(0);
    }

    @Nonnull
    private static List<String> getImplicitPaths(@Nonnull RsModDeclItem decl) {
        String name = decl.getName();
        if (name == null) return Collections.emptyList();
        if (isLocal(decl)) return Collections.emptyList();
        return Arrays.asList(name + ".rs", name + "/mod.rs");
    }
}
