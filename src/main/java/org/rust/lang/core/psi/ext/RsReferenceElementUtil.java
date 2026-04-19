/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsPsiUtilUtil;

/**
 * <p>
 * <ul>
 *   <li>{@link RsReferenceElementBase} - provides basic methods for reference implementation</li>
 *   <li>{@link RsReferenceElement} - marks an element that optionally can have a reference</li>
 *   <li>{@link RsMandatoryReferenceElement} - marks an element that has a reference</li>
 * </ul>
 * <p>
 * The Java interface declarations are in their own files. This utility class provides
 * the default method implementations as static helpers.
 *
 * @see RsReferenceElementBase
 * @see RsReferenceElement
 * @see RsMandatoryReferenceElement
 */
public final class RsReferenceElementUtil {
    private RsReferenceElementUtil() {
    }

    /**
     * Returns the unescaped text of the reference name element, or {@code null} if the
     * reference name element is null. This corresponds to the default {@code referenceName}
     */
    @Nullable
    public static String getReferenceName(@NotNull RsReferenceElementBase element) {
        PsiElement nameElement = element.getReferenceNameElement();
        return nameElement != null ? RsPsiUtilUtil.getUnescapedText(nameElement) : null;
    }

    /**
     * Returns the unescaped text of the reference name element. This corresponds to the
     * overridden {@code referenceName} property in the {@link RsMandatoryReferenceElement}
     */
    @NotNull
    public static String getMandatoryReferenceName(@NotNull RsMandatoryReferenceElement element) {
        return RsPsiUtilUtil.getUnescapedText(element.getReferenceNameElement());
    }
}
