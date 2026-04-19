/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiComment;
import com.intellij.psi.SyntaxTraverser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsUseGroup;
import org.rust.lang.core.psi.RsUseSpeck;

import java.util.List;

public final class RsUseGroupUtil {
    private RsUseGroupUtil() {
    }

    @NotNull
    public static RsUseSpeck getParentUseSpeck(@NotNull RsUseGroup useGroup) {
        return (RsUseSpeck) useGroup.getParent();
    }

    @Nullable
    public static RsUseSpeck getAsTrivial(@NotNull RsUseGroup useGroup) {
        List<RsUseSpeck> specks = useGroup.getUseSpeckList();
        if (specks.size() != 1) return null;
        RsUseSpeck speck = specks.get(0);
        // Do not collapse {self}
        if (speck.getAlias() == null && !RsUseSpeckUtil.isIdentifier(speck) && (speck.getPath() == null || speck.getPath().getPath() == null)) {
            return null;
        }
        // Do not change use-groups with comments
        for (Object element : SyntaxTraverser.psiTraverser(useGroup)) {
            if (element instanceof PsiComment) return null;
        }
        return speck;
    }
}
