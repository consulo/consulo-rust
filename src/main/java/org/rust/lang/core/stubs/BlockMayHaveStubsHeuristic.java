/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.openapi.util.Key;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.RsLanguage;

import static org.rust.lang.core.psi.RsElementTypes.*;
import static org.rust.lang.core.psi.RsTokenType.tokenSetOf;

/**
 * A utility used in the stub builder to detect that a code block should not be
 * traversed in order to find child stubs.
 */
final class BlockMayHaveStubsHeuristic {

    private BlockMayHaveStubsHeuristic() {
    }

    private static final Key<Boolean> RS_HAS_ITEMS_OR_ATTRS = Key.create("RS_HAS_ITEMS_OR_ATTRS");
    private static final TokenSet ITEM_DEF_KWS = tokenSetOf(STATIC, ENUM, IMPL, MACRO_KW, MOD, STRUCT, UNION, TRAIT, TYPE_KW, USE);
    private static final TokenSet UNEXPECTED_NEXT_CONST_TOKENS = tokenSetOf(LBRACE, MOVE, OR);

    public static boolean computeAndCache(@NotNull ASTNode node) {
        assertIsBlock(node);
        boolean hasItemsOrAttrs = computeBlockMayHaveStubs(node);
        node.putUserData(RS_HAS_ITEMS_OR_ATTRS, hasItemsOrAttrs);
        return hasItemsOrAttrs;
    }

    public static boolean getAndClearCached(@NotNull ASTNode node) {
        assertIsBlock(node);
        Boolean cachedHasItemsOrAttrs = node.getUserData(RS_HAS_ITEMS_OR_ATTRS);
        if (cachedHasItemsOrAttrs != null) {
            node.putUserData(RS_HAS_ITEMS_OR_ATTRS, null);
            return cachedHasItemsOrAttrs;
        }
        return computeBlockMayHaveStubs(node);
    }

    private static void assertIsBlock(@NotNull ASTNode node) {
        if (node.getElementType() != BLOCK) {
            throw new IllegalStateException("Expected block, found: " + node.getElementType() + ", text: `" + node.getText() + "`");
        }
    }

    private static boolean computeBlockMayHaveStubs(@NotNull ASTNode node) {
        com.intellij.lang.PsiBuilder b = PsiBuilderFactory.getInstance()
            .createBuilder(node.getPsi().getProject(), node, null, RsLanguage.INSTANCE, node.getChars());
        IElementType prevToken = null;
        String prevTokenText = null;
        while (true) {
            IElementType token = b.getTokenType();
            if (token == null) break;
            boolean looksLikeStubElement = ITEM_DEF_KWS.contains(token)
                || (token == CONST && !(prevToken == MUL || (prevToken == IDENTIFIER && "raw".equals(prevTokenText)))
                    && !UNEXPECTED_NEXT_CONST_TOKENS.contains(b.lookAhead(1)))
                || (token == EXCL && prevToken == SHA)
                || (token == EXCL && prevToken == IDENTIFIER && "macro_rules".equals(prevTokenText))
                || (token == IDENTIFIER && prevToken == FN)
                || (token == IDENTIFIER && "union".equals(b.getTokenText()) && b.lookAhead(1) == IDENTIFIER);
            if (looksLikeStubElement) {
                return true;
            }
            prevToken = token;
            prevTokenText = (token == IDENTIFIER) ? b.getTokenText() : null;
            b.advanceLexer();
        }
        return false;
    }
}
