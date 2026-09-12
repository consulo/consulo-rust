/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import consulo.language.editor.refactoring.NamesValidator;
import consulo.project.Project;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.lexer.LexerUtilUtil;
import org.rust.lang.core.psi.RsElementTypes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import consulo.annotation.component.ExtensionImpl;
import org.rust.lang.core.psi.RsTokenSets;
import consulo.language.Language;
import org.rust.lang.RsLanguage;

@ExtensionImpl
public class RsNamesValidator implements NamesValidator {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


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
    public boolean isKeyword(@Nonnull String name, @Nullable Project project) {
        return isKeyword(name);
    }

    @Override
    public boolean isIdentifier(@Nonnull String name, @Nullable Project project) {
        return isIdentifier(name);
    }

    public static boolean isIdentifier(@Nonnull String name) {
        IElementType tokenType = LexerUtilUtil.getRustLexerTokenType(name);
        if (RsElementTypes.IDENTIFIER.equals(tokenType)) {
            return !RESERVED_KEYWORDS.contains(name);
        }
        if (RsElementTypes.QUOTE_IDENTIFIER.equals(tokenType)) {
            return true;
        }
        return false;
    }

    public static boolean isKeyword(@Nonnull String name) {
        IElementType tokenType = LexerUtilUtil.getRustLexerTokenType(name);
        return RsTokenSets.RS_KEYWORDS.contains(tokenType);
    }

    public static boolean isValidRustVariableIdentifier(@Nonnull String name) {
        IElementType tokenType = LexerUtilUtil.getRustLexerTokenType(name);
        return RsElementTypes.IDENTIFIER.equals(tokenType) && !RESERVED_KEYWORDS.contains(name);
    }
}
