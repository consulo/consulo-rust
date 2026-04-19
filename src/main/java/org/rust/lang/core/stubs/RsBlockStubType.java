/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import org.rust.stdext.Lazy;
import com.intellij.lang.*;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LazyParseableElement;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.*;
import com.intellij.util.CharTable;
import com.intellij.util.diff.FlyweightCapableTreeStructure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.lexer.RsLexer;
import org.rust.lang.core.parser.RustParser;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.impl.RsBlockImpl;

import static org.rust.lang.core.psi.RsElementTypes.*;
import static org.rust.lang.core.psi.RsTokenType.*;

/**
 * {@link IReparseableElementTypeBase} and {@link ICustomParsingType} are implemented to provide lazy and incremental
 * parsing of function bodies.
 * {@link ICompositeElementType} - to create AST of type {@link LazyParseableElement} in the case of non-lazy parsing
 * ({@code if} bodies, {@code match} arms, etc), just to have the same AST class for all code blocks.
 * {@link ILightLazyParseableElementType} is needed to diff trees correctly (see {@code PsiBuilderImpl.MyComparator}).
 */
public class RsBlockStubType extends RsPlaceholderStub.Type<RsBlock>
    implements ICustomParsingType, ICompositeElementType, IReparseableElementTypeBase, ILightLazyParseableElementType {

    public static final RsBlockStubType INSTANCE = new RsBlockStubType();

    private static final TokenSet RS_ITEMS_AND_INNER_ATTR = TokenSet.orSet(RS_ITEMS, tokenSetOf(MACRO, INNER_ATTR));

    private RsBlockStubType() {
        super("BLOCK", RsBlockImpl::new);
    }

    /** Note: must return {@code false} if {@link com.intellij.psi.StubBuilder#skipChildProcessingWhenBuildingStubs} returns {@code true} for the node */
    @Override
    public boolean shouldCreateStub(@NotNull ASTNode node) {
        if (node.getTreeParent().getElementType() == FUNCTION) {
            return node.findChildByType(RS_ITEMS_AND_INNER_ATTR) != null || ItemSeekingVisitor.containsItems(node);
        } else {
            return createStubIfParentIsStub(node) || node.findChildByType(RS_ITEMS) != null;
        }
    }

    // Lazy parsed (function body)
    @NotNull
    @Override
    public ASTNode parse(@NotNull CharSequence text, @NotNull CharTable table) {
        return new LazyParseableElement(this, text);
    }

    // Non-lazy case (`if` body, etc).
    @NotNull
    @Override
    public ASTNode createCompositeNode() {
        return new LazyParseableElement(this, null);
    }

    @Nullable
    @Override
    public ASTNode parseContents(@NotNull ASTNode chameleon) {
        Project project = chameleon.getTreeParent().getPsi().getProject();
        PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, RsLanguage.INSTANCE, chameleon.getChars());
        parseBlock(builder);
        return builder.getTreeBuilt().getFirstChildNode();
    }

    @NotNull
    @Override
    public FlyweightCapableTreeStructure<LighterASTNode> parseContents(@NotNull LighterLazyParseableNode chameleon) {
        var containingFile = chameleon.getContainingFile();
        if (containingFile == null) {
            throw new IllegalStateException("`containingFile` must not be null: " + chameleon);
        }
        Project project = containingFile.getProject();
        PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, RsLanguage.INSTANCE, chameleon.getText());
        parseBlock(builder);
        return builder.getLightTree();
    }

    private void parseBlock(@NotNull PsiBuilder builder) {
        PsiBuilder adaptBuilder = GeneratedParserUtilBase.adapt_builder_(BLOCK, builder, new RustParser(), RustParser.EXTENDS_SETS_);
        PsiBuilder.Marker marker = GeneratedParserUtilBase.enter_section_(adaptBuilder, 0, GeneratedParserUtilBase._COLLAPSE_, null);
        boolean result = RustParser.InnerAttrsAndBlock(adaptBuilder, 0);
        GeneratedParserUtilBase.exit_section_(adaptBuilder, 0, marker, BLOCK, result, true, GeneratedParserUtilBase.TRUE_CONDITION);
    }

    // Restricted to a function body only because it is well tested case.
    @Override
    public boolean isReparseable(@NotNull ASTNode currentNode, @NotNull CharSequence newText, @NotNull Language fileLanguage, @NotNull Project project) {
        return currentNode.getTreeParent() != null && currentNode.getTreeParent().getElementType() == FUNCTION
            && PsiBuilderUtil.hasProperBraceBalance(newText, new RsLexer(), LBRACE, RBRACE);
    }

    // Avoid double lexing
    @Override
    public boolean reuseCollapsedTokens() {
        return true;
    }
}
