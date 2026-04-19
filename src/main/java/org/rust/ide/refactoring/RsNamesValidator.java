/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.lang.refactoring.NamesValidator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.lexer.LexerUtilUtil;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsTokenType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RsNamesValidator implements NamesValidator {

    public static final Set<String> RESERVED_LIFETIME_NAMES = new HashSet<>(Arrays.asList("'static", "'_"));

    public static final Set<String> RESERVED_KEYWORDS = new HashSet<>(Arrays.asList(
        "abstract",
        "become",
        "do",
        "final",
        "override",
        "priv",
        "typeof",
        "unsized",
        "virtual"
    ));

    @Override
    public boolean isKeyword(@NotNull String name, @Nullable Project project) {
        return isKeyword(name);
    }

    @Override
    public boolean isIdentifier(@NotNull String name, @Nullable Project project) {
        return isIdentifier(name);
    }

    public static boolean isIdentifier(@NotNull String name) {
        IElementType tokenType = LexerUtilUtil.getRustLexerTokenType(name);
        if (RsElementTypes.IDENTIFIER.equals(tokenType)) {
            return !RESERVED_KEYWORDS.contains(name);
        }
        if (RsElementTypes.QUOTE_IDENTIFIER.equals(tokenType)) {
            return true;
        }
        return false;
    }

    public static boolean isKeyword(@NotNull String name) {
        IElementType tokenType = LexerUtilUtil.getRustLexerTokenType(name);
        return RsTokenType.RS_KEYWORDS.contains(tokenType);
    }

    public static boolean isValidRustVariableIdentifier(@NotNull String name) {
        IElementType tokenType = LexerUtilUtil.getRustLexerTokenType(name);
        return RsElementTypes.IDENTIFIER.equals(tokenType) && !RESERVED_KEYWORDS.contains(name);
    }
}
