/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.impl;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.names.RsNamesValidatorUtil;
import org.rust.lang.core.psi.ext.impl.PsiElementUtil;

import java.util.List;
import org.rust.lang.core.psi.*;

public final class RsRawIdentifiers {

    public static final String RS_RAW_PREFIX = "r#";
    private static final List<String> CAN_NOT_BE_ESCAPED = List.of("self", "super", "crate", "Self");

    public static boolean canBeEscaped(@Nonnull String s) {
        if (CAN_NOT_BE_ESCAPED.contains(s)) return false;
        for (String prefix : CAN_NOT_BE_ESCAPED) {
            if (s.startsWith(prefix + "::")) return false;
        }
        return true;
    }

    @Nonnull
    public static String unescapeIdentifier(@Nonnull String s) {
        if (s.startsWith(RS_RAW_PREFIX)) {
            return s.substring(RS_RAW_PREFIX.length());
        }
        return s;
    }

    @Nonnull
    public static String escapeIdentifierIfNeeded(@Nonnull String s) {
        if (RsNamesValidatorUtil.isValidRustVariableIdentifier(s) || !canBeEscaped(s)) {
            return s;
        }
        return RS_RAW_PREFIX + s;
    }

    @Nonnull
    public static String getUnescapedText(@Nonnull PsiElement element) {
        String text = element.getText();
        if (text == null) return "";
        if (PsiElementUtil.getElementType(element) == RsElementTypes.IDENTIFIER) {
            return unescapeIdentifier(text);
        }
        return text;
    }

    private RsRawIdentifiers() {}
}
