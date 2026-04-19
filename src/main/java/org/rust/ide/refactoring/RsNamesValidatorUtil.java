/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.lexer.LexerUtilUtil;
import org.rust.lang.core.psi.RsElementTypes;

import java.util.Arrays;
import java.util.List;

public final class RsNamesValidatorUtil {
    private RsNamesValidatorUtil() {}

    public static final String RS_RAW_PREFIX = "r#";

    private static final List<String> CAN_NOT_BE_ESCAPED =
        Arrays.asList("self", "super", "crate", "Self");

    public static boolean getCanBeEscaped(@NotNull String name) {
        if (CAN_NOT_BE_ESCAPED.contains(name)) return false;
        for (String keyword : CAN_NOT_BE_ESCAPED) {
            if (name.startsWith(keyword + "::")) return false;
        }
        return true;
    }

    public static boolean isValidRustVariableIdentifier(@NotNull String name) {
        IElementType tokenType = LexerUtilUtil.getRustLexerTokenType(name);
        return RsElementTypes.IDENTIFIER.equals(tokenType)
            && !RsNamesValidator.RESERVED_KEYWORDS.contains(name);
    }
}
