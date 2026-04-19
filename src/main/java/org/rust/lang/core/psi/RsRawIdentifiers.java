/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.refactoring.RsNamesValidatorUtil;
import org.rust.lang.core.psi.ext.PsiElementUtil;

import java.util.List;

public final class RsRawIdentifiers {

    public static final String RS_RAW_PREFIX = "r#";
    private static final List<String> CAN_NOT_BE_ESCAPED = List.of("self", "super", "crate", "Self");

    public static boolean canBeEscaped(@NotNull String s) {
        if (CAN_NOT_BE_ESCAPED.contains(s)) return false;
        for (String prefix : CAN_NOT_BE_ESCAPED) {
            if (s.startsWith(prefix + "::")) return false;
        }
        return true;
    }

    @NotNull
    public static String unescapeIdentifier(@NotNull String s) {
        if (s.startsWith(RS_RAW_PREFIX)) {
            return s.substring(RS_RAW_PREFIX.length());
        }
        return s;
    }

    @NotNull
    public static String escapeIdentifierIfNeeded(@NotNull String s) {
        if (RsNamesValidatorUtil.isValidRustVariableIdentifier(s) || !canBeEscaped(s)) {
            return s;
        }
        return RS_RAW_PREFIX + s;
    }

    @NotNull
    public static String getUnescapedText(@NotNull PsiElement element) {
        String text = element.getText();
        if (text == null) return "";
        if (PsiElementUtil.getElementType(element) == RsElementTypes.IDENTIFIER) {
            return unescapeIdentifier(text);
        }
        return text;
    }

    private RsRawIdentifiers() {}
}
