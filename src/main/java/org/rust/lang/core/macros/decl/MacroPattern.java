/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.decl;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.macros.errors.MacroMatchingError;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsMacroPatternContents;
import org.rust.lang.core.psi.RsTokenType;
import org.rust.stdext.RsResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a parsed macro pattern used for matching against macro call bodies.
 */
public class MacroPattern {

    @NotNull
    private final List<ASTNode> myPattern;

    private MacroPattern(@NotNull List<ASTNode> pattern) {
        myPattern = pattern;
    }

    @NotNull
    public List<ASTNode> getPattern() {
        return myPattern;
    }

    @NotNull
    public RsResult<MacroSubstitution, MacroMatchingError> match(@NotNull PsiBuilder macroCallBody) {
        // Full matching implementation delegates to matchPartial and checks for EOF
        // This is a structural placeholder
        return new RsResult.Err<>(new MacroMatchingError.EndOfInput(macroCallBody.getCurrentOffset()));
    }

    public boolean isEmpty() {
        return myPattern.isEmpty();
    }

    @NotNull
    public static MacroPattern valueOf(@Nullable RsMacroPatternContents psi) {
        if (psi == null) return new MacroPattern(new ArrayList<>());
        ASTNode node = psi.getNode();
        List<ASTNode> children = flattenChildren(node);
        return new MacroPattern(children);
    }

    @NotNull
    private static List<ASTNode> flattenChildren(@NotNull ASTNode node) {
        List<ASTNode> result = new ArrayList<>();
        ASTNode child = node.getFirstChildNode();
        while (child != null) {
            IElementType type = child.getElementType();
            if (type != TokenType.WHITE_SPACE && !RsTokenType.RS_COMMENTS.contains(type)) {
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
