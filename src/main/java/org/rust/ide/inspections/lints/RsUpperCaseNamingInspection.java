/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Checks if the name is UPPER_CASE.
 */
public class RsUpperCaseNamingInspection extends RsNamingInspection {

    public RsUpperCaseNamingInspection(@NotNull String elementType) {
        super(elementType, "an upper");
    }

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.NonUpperCaseGlobals;
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

        if (!str.isEmpty()) {
            boolean noneLower = true;
            for (int i = 0; i < str.length(); i++) {
                if (Character.isLowerCase(str.charAt(i))) {
                    noneLower = false;
                    break;
                }
            }
            if (noneLower) return null;
        }
        if (str.isEmpty()) return "UPPER_CASE";
        return RsNamingInspection.toSnakeCase(name, true);
    }
}
