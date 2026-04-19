/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.RsEnumVariant;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.RsEnumItemUtil;
import org.rust.lang.core.psi.ext.RsFieldsOwner;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;

import java.util.ArrayList;
import java.util.List;

public final class ThirUtilUtil {
    private ThirUtilUtil() {
    }

    @NotNull
    public static RsFieldsOwner variant(@NotNull RsStructOrEnumItemElement element, int index) {
        if (element instanceof RsStructItem) {
            return (RsFieldsOwner) element;
        } else if (element instanceof RsEnumItem) {
            return RsEnumItemUtil.getVariants((RsEnumItem) element).get(index);
        } else {
            throw new IllegalStateException("unreachable");
        }
    }

    @NotNull
    public static List<Pair<Integer, Long>> discriminants(@NotNull RsEnumItem enumItem) {
        List<RsEnumVariant> variants = RsEnumItemUtil.getVariants(enumItem);
        List<Pair<Integer, Long>> result = new ArrayList<>(variants.size());
        for (int i = 0; i < variants.size(); i++) {
            result.add(new Pair<>(i, (long) i));
        }
        return result;
    }
}
