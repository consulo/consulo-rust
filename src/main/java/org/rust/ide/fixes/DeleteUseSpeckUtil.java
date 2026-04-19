/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

public final class DeleteUseSpeckUtil {
    private DeleteUseSpeckUtil() {
    }

    public static void deleteUseSpeck(@org.jetbrains.annotations.NotNull com.intellij.psi.PsiElement useSpeck) {
        com.intellij.psi.PsiElement parent = useSpeck.getParent();
        if (parent instanceof org.rust.lang.core.psi.RsUseItem) {
            com.intellij.psi.PsiElement next = parent.getNextSibling();
            if (next instanceof com.intellij.psi.PsiWhiteSpace) {
                next.delete();
            }
            parent.delete();
        } else {
            org.rust.lang.core.psi.ext.RsUseSpeckUtil.deleteWithSurroundingComma(useSpeck);
        }
    }
}
