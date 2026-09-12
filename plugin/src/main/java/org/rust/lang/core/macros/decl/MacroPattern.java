/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.decl;

import consulo.language.ast.ASTNode;
import consulo.language.parser.PsiBuilder;
import consulo.language.ast.TokenType;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.macros.errors.MacroMatchingError;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsMacroPatternContents;
import org.rust.stdext.RsResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.rust.lang.core.psi.RsTokenSets;

/**
 * Represents a parsed macro pattern used for matching against macro call bodies.
 */
public class MacroPattern {

    @Nonnull
    private final List<ASTNode> myPattern;

    private MacroPattern(@Nonnull List<ASTNode> pattern) {
        myPattern = pattern;
    }

    @Nonnull
    public List<ASTNode> getPattern() {
        return myPattern;
    }

    @Nonnull
    public RsResult<MacroSubstitution, MacroMatchingError> match(@Nonnull PsiBuilder macroCallBody) {
        // Full matching implementation delegates to matchPartial and checks for EOF
        // This is a structural placeholder
        return new RsResult.Err<>(new MacroMatchingError.EndOfInput(macroCallBody.getCurrentOffset()));
    }

    public boolean isEmpty() {
        return myPattern.isEmpty();
    }

    @Nonnull
    public static MacroPattern valueOf(@Nullable RsMacroPatternContents psi) {
        if (psi == null) return new MacroPattern(new ArrayList<>());
        ASTNode node = psi.getNode();
        List<ASTNode> children = flattenChildren(node);
        return new MacroPattern(children);
    }

    @Nonnull
    private static List<ASTNode> flattenChildren(@Nonnull ASTNode node) {
        List<ASTNode> result = new ArrayList<>();
        ASTNode child = node.getFirstChildNode();
        while (child != null) {
            IElementType type = child.getElementType();
            if (type != TokenType.WHITE_SPACE && !RsTokenSets.RS_COMMENTS.contains(type)) {
                if (type == RsElementTypes.MACRO_PATTERN || type == RsElementTypes.MACRO_PATTERN_CONTENTS) {
                    result.addAll(flattenChildren(child));
                } else {
                    result.add(child);
                }
            }
            child = child.getTreeNext();
        }
        return result;
    }
}
