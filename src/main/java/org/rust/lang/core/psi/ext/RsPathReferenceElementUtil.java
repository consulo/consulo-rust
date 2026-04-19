/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsPsiUtilUtil;
import org.rust.lang.core.resolve.ref.RsPathReference;
import org.rust.lang.core.stubs.common.RsPathPsiOrStub;

/**
 * Utility methods for the {@code RsPathReferenceElement} interface.
 * <p>
 * {@link RsReferenceElement} and {@link RsPathPsiOrStub}, overriding
 * {@code getReference()} to return {@link RsPathReference} and providing a
 * default implementation for {@code referenceName}.
 *
 * @see RsReferenceElement
 * @see RsPathPsiOrStub
 */
public final class RsPathReferenceElementUtil {
    private RsPathReferenceElementUtil() {
    }

    /**
     * Returns the reference name by getting the unescaped text of the reference name element.
     */
    @Nullable
    public static String getReferenceName(@NotNull RsReferenceElementBase element) {
        PsiElement nameElement = element.getReferenceNameElement();
        return nameElement != null ? RsPsiUtilUtil.getUnescapedText(nameElement) : null;
    }
}
