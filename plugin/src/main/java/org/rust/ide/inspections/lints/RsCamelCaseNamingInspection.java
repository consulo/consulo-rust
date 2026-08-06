/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Checks if the name is CamelCase.
 */
public class RsCamelCaseNamingInspection extends RsNamingInspection {

    public RsCamelCaseNamingInspection(@Nonnull String elementType, @Nonnull String elementTitle) {
        super(elementType, "a camel", elementTitle);
    }

    public RsCamelCaseNamingInspection(@Nonnull String elementType) {
        this(elementType, elementType);
    }

    @Nonnull
    @Override
    protected RsLint getLint(@Nonnull PsiElement element) {
        return RsLint.NonCamelCaseTypes;
    }

    @Nullable
    @Override
    public String checkName(@Nonnull String name) {
        String str = name;
        // trim leading and trailing underscores
        int start = 0;
        while (start < str.length() && str.charAt(start) == '_') start++;
        int end = str.length();
        while (end > start && str.charAt(end - 1) == '_') end--;
        str = str.substring(start, end);

        if (RsNamingInspection.isCamelCase(str)) {
            return null;
        } else {
            if (str.isEmpty()) return "CamelCase";
            return suggestName(name);
        }
    }

    @Nonnull
    private String suggestName(@Nonnull String name) {
        String result = RsNamingInspection.toCamelCase(name);
        return result.isEmpty() ? "CamelCase" : result;
    }
}
