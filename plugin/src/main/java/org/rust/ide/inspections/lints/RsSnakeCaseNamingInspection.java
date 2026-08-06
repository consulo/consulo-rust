/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Checks if the name is snake_case.
 */
public class RsSnakeCaseNamingInspection extends RsNamingInspection {

    public RsSnakeCaseNamingInspection(@Nonnull String elementType) {
        super(elementType, "a snake");
    }

    @Nonnull
    @Override
    protected RsLint getLint(@Nonnull PsiElement element) {
        return RsLint.NonSnakeCase;
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

        // Some characters don't have case so we can't use `isLowerCase` here
        if (!str.isEmpty()) {
            boolean allNonUpper = true;
            for (int i = 0; i < str.length(); i++) {
                if (Character.isUpperCase(str.charAt(i))) {
                    allNonUpper = false;
                    break;
                }
            }
            if (allNonUpper) return null;
        }
        if (str.isEmpty()) return "snake_case";
        return RsNamingInspection.toSnakeCase(name, false);
    }
}
