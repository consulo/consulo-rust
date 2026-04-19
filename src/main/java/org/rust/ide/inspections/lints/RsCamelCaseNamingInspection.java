/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Checks if the name is CamelCase.
 */
public class RsCamelCaseNamingInspection extends RsNamingInspection {

    public RsCamelCaseNamingInspection(@NotNull String elementType, @NotNull String elementTitle) {
        super(elementType, "a camel", elementTitle);
    }

    public RsCamelCaseNamingInspection(@NotNull String elementType) {
        this(elementType, elementType);
    }

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.NonCamelCaseTypes;
    }

    @Nullable
    @Override
    public String checkName(@NotNull String name) {
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

    @NotNull
    private String suggestName(@NotNull String name) {
        String result = RsNamingInspection.toCamelCase(name);
        return result.isEmpty() ? "CamelCase" : result;
    }
}
