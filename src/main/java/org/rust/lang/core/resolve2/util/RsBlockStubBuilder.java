/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2.util;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsTokenType;
import org.rust.lang.core.stubs.RsBlockStubType;

import static org.rust.lang.core.psi.RsElementTypes.*;

/**
 * Provides stub building for RsBlock that is not used to build real PSI stubs,
 * but used for new name resolution in local scopes (hanging mod data).
 */
public final class RsBlockStubBuilder {

    private RsBlockStubBuilder() {}

    private static final TokenSet RS_ITEMS_AND_MACRO = TokenSet.orSet(
        TokenSet.andNot(RsTokenType.RS_ITEMS, RsTokenType.tokenSetOf(IMPL_ITEM)),
        RsTokenType.tokenSetOf(MACRO)
    );

    /**
     * Builds a stub tree for the given RsBlock.
     * Extension function on RsBlock: RsBlock.buildStub()
     */
    @Nullable
    public static StubElement<RsBlock> buildStub(@NotNull RsBlock block) {
        return new InternalRsBlockStubBuilder().buildStubTreeFor(block);
    }

    private static boolean isBlockOrLocalMod(@NotNull IElementType elementType) {
        return elementType == BLOCK || elementType == MOD_ITEM;
    }

    private static boolean shouldCreateStub(@NotNull IElementType parentType, @NotNull IElementType nodeType) {
        if (nodeType == BLOCK) {
            return parentType instanceof IStubFileElementType<?>;
        }
        if (nodeType == MACRO_CALL) {
            return isBlockOrLocalMod(parentType);
        }
        if (nodeType == PATH || nodeType == USE_GROUP || nodeType == USE_SPECK || nodeType == ALIAS
            || nodeType == ENUM_BODY || nodeType == ENUM_VARIANT || nodeType == VIS || nodeType == VIS_RESTRICTION
            || nodeType == OUTER_ATTR || nodeType == META_ITEM || nodeType == META_ITEM_ARGS) {
            return true;
        }
        return isBlockOrLocalMod(parentType) && RS_ITEMS_AND_MACRO.contains(nodeType);
    }

    /**
     * Internal stub builder implementation.
     */
    private static class InternalRsBlockStubBuilder extends DefaultStubBuilder {

        @Nullable
        StubElement<RsBlock> buildStubTreeFor(@NotNull RsBlock root) {
            PsiFileStubImpl<?> parentStub = new PsiFileStubImpl<>(null);
            RsBlockStubBuildingWalkingVisitor visitor = new RsBlockStubBuildingWalkingVisitor(root.getNode(), parentStub);
            visitor.buildStubTree();
            @SuppressWarnings("unchecked")
            IStubElementType<StubElement<RsBlock>, RsBlock> stubType =
                (IStubElementType<StubElement<RsBlock>, RsBlock>) (IStubElementType<?, ?>) RsBlockStubType.INSTANCE;
            return parentStub.findChildStubByType(stubType);
        }

        @Override
        public boolean skipChildProcessingWhenBuildingStubs(ASTNode parent, ASTNode node) {
            return !shouldCreateStub(parent.getElementType(), node.getElementType());
        }

        private class RsBlockStubBuildingWalkingVisitor extends StubBuildingWalkingVisitor {

            RsBlockStubBuildingWalkingVisitor(@NotNull ASTNode root, @NotNull StubElement<?> parentStub) {
                super(root, parentStub);
            }

            @Nullable
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            protected StubElement createStub(StubElement parentStub, ASTNode node) {
                IElementType elementType = node.getElementType();
                if (!(elementType instanceof IStubElementType<?, ?>)) return null;
                @SuppressWarnings("unchecked")
                IStubElementType<StubElement<?>, PsiElement> nodeType =
                    (IStubElementType<StubElement<?>, PsiElement>) elementType;
                IElementType parentType;
                if (parentStub instanceof PsiFileStub<?>) {
                    parentType = ((PsiFileStub<?>) parentStub).getType();
                } else {
                    parentType = parentStub.getStubType();
                }
                if (!shouldCreateStub(parentType, nodeType)) return null;
                return nodeType.createStub(node.getPsi(), parentStub);
            }
        }
    }
}
