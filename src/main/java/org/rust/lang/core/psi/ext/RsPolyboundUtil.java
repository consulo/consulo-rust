/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsPolybound;
import org.rust.lang.core.stubs.RsPolyboundStub;

public final class RsPolyboundUtil {
    private RsPolyboundUtil() {
    }

    /**
     * Return true if there is a question mark before a bound:
     * <pre>fn foo&lt;T: ?Sized&gt;() {}</pre>
     */
    public static boolean getHasQ(@NotNull RsPolybound polybound) {
        Object stub = RsPsiJavaUtil.getGreenStub(polybound);
        if (stub instanceof RsPolyboundStub) {
            return ((RsPolyboundStub) stub).getHasQ();
        }
        return polybound.getQ() != null;
    }

    /**
     * Return true if there is the tilde const before a bound:
     * <pre>fn foo&lt;T: ~const A&gt;() {}</pre>
     */
    public static boolean getHasConst(@NotNull RsPolybound polybound) {
        Object stub = RsPsiJavaUtil.getGreenStub(polybound);
        if (stub instanceof RsPolyboundStub) {
            return ((RsPolyboundStub) stub).getHasConst();
        }
        return polybound.getTildeConst() != null;
    }

    /**
     * Delete the polybound along with surrounding plus signs.
     */
    public static void deleteWithSurroundingPlus(@NotNull RsPolybound polybound) {
        PsiElement followingPlus = RsElementUtil.getNextNonCommentSibling(polybound);
        if (followingPlus != null && RsElementUtil.getElementType(followingPlus) == RsElementTypes.PLUS) {
            followingPlus.delete();
        } else {
            PsiElement precedingPlus = RsElementUtil.getPrevNonCommentSibling(polybound);
            if (precedingPlus != null && RsElementUtil.getElementType(precedingPlus) == RsElementTypes.PLUS) {
                precedingPlus.delete();
            }
        }
        polybound.delete();
    }
}
