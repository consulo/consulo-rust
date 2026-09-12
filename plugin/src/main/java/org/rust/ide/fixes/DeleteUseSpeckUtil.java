/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import org.rust.lang.core.psi.RsUseItem;
import org.rust.lang.core.psi.ext.RsUseSpeckUtil;

public final class DeleteUseSpeckUtil {
    private DeleteUseSpeckUtil() {
    }

    public static void deleteUseSpeck(@org.jetbrains.annotations.NotNull consulo.language.psi.PsiElement useSpeck) {
        consulo.language.psi.PsiElement parent = useSpeck.getParent();
        if (parent instanceof org.rust.lang.core.psi.RsUseItem) {
            consulo.language.psi.PsiElement next = parent.getNextSibling();
            if (next instanceof consulo.language.psi.PsiWhiteSpace) {
                next.delete();
            }
            parent.delete();
        } else {
            org.rust.lang.core.psi.ext.RsUseSpeckUtil.deleteWithSurroundingComma(useSpeck);
        }
    }
}
