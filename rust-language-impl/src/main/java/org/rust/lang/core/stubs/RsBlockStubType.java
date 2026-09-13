/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;


import consulo.language.ast.LighterLazyParseableNode;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiBuilderFactory;
import consulo.language.ast.LighterASTNode;
import consulo.language.impl.parser.GeneratedParserUtilBase;
import consulo.project.Project;
import consulo.language.impl.ast.LazyParseableElement;
import consulo.language.psi.stub.*;
import consulo.language.ast.TokenSet;
import consulo.language.ast.ICustomParsingType;
import consulo.language.ast.ILightLazyParseableElementType;
import consulo.language.ast.IReparseableElementTypeBase;
import consulo.language.parser.PsiBuilderUtil;
import consulo.language.ast.ICompositeElementType;
import consulo.language.util.CharTable;
import consulo.language.util.FlyweightCapableTreeStructure;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.lexer.RsLexer;
import org.rust.lang.core.parser.RustParser;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.impl.RsBlockImpl;

import static org.rust.lang.core.psi.RsElementTypes.*;
import static org.rust.lang.core.psi.impl.RsTokenSets.*;
import static org.rust.lang.core.psi.RsTokenType.*;
import consulo.language.version.LanguageVersion;
import org.rust.lang.core.psi.RsTokenType;

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

    /**
     * Token sets derived from {@link org.rust.lang.core.psi.RsTokenType} live in a holder so that they are
     * computed on first use instead of while this element type is being constructed. Initialization is
     * published safely by the class initialization lock of the holder.
     */
    private static final class ItemTokens {
        private static final TokenSet RS_ITEMS_AND_INNER_ATTR = TokenSet.orSet(RS_ITEMS, tokenSetOf(MACRO, INNER_ATTR));
    }

    private RsBlockStubType() {
        super("BLOCK", RsBlockImpl::new);
    }

    /** Note: must return {@code false} if {@link com.intellij.psi.StubBuilder#skipChildProcessingWhenBuildingStubs} returns {@code true} for the node */
    @Override
    public boolean shouldCreateStub(@Nonnull ASTNode node) {
        if (node.getTreeParent().getElementType() == FUNCTION) {
            return node.findChildByType(ItemTokens.RS_ITEMS_AND_INNER_ATTR) != null || ItemSeekingVisitor.containsItems(node);
        } else {
            return createStubIfParentIsStub(node) || node.findChildByType(RS_ITEMS) != null;
        }
    }

    // Lazy parsed (function body)
    @Nonnull
    @Override
    public ASTNode parse(@Nonnull CharSequence text, @Nonnull CharTable table) {
        return new LazyParseableElement(this, text);
    }

    // Non-lazy case (`if` body, etc).
    @Nonnull
    @Override
    public ASTNode createCompositeNode() {
        return new LazyParseableElement(this, null);
    }

    @Nullable
    @Override
    public ASTNode parseContents(@Nonnull ASTNode chameleon) {
        Project project = chameleon.getTreeParent().getPsi().getProject();
        PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, RsLanguage.INSTANCE, chameleon.getChars());
        parseBlock(builder);
        return builder.getTreeBuilt().getFirstChildNode();
    }

    @Nonnull
    @Override
    public FlyweightCapableTreeStructure<LighterASTNode> parseContents(@Nonnull LighterLazyParseableNode chameleon) {
        var containingFile = chameleon.getContainingFile();
        if (containingFile == null) {
            throw new IllegalStateException("`containingFile` must not be null: " + chameleon);
        }
        Project project = containingFile.getProject();
        consulo.language.version.LanguageVersion version = RsLanguage.INSTANCE.getVersions()[0];
        PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, RsLanguage.INSTANCE, version, chameleon.getText());
        parseBlock(builder);
        return builder.getLightTree();
    }

    private void parseBlock(@Nonnull PsiBuilder builder) {
        PsiBuilder adaptBuilder = GeneratedParserUtilBase.adapt_builder_(BLOCK, builder, new RustParser(), RustParser.EXTENDS_SETS_);
        PsiBuilder.Marker marker = GeneratedParserUtilBase.enter_section_(adaptBuilder, 0, GeneratedParserUtilBase._COLLAPSE_, null);
        boolean result = RustParser.InnerAttrsAndBlock(adaptBuilder, 0);
        GeneratedParserUtilBase.exit_section_(adaptBuilder, 0, marker, BLOCK, result, true, GeneratedParserUtilBase.TRUE_CONDITION);
    }

    // Restricted to a function body only because it is well tested case.
    public boolean isReparseable(@Nonnull ASTNode currentNode, @Nonnull CharSequence newText, @Nonnull Language fileLanguage, @Nonnull Project project) {
        return currentNode.getTreeParent() != null && currentNode.getTreeParent().getElementType() == FUNCTION
            && PsiBuilderUtil.hasProperBraceBalance(newText, new RsLexer(), LBRACE, RBRACE);
    }

    // Avoid double lexing
    @Override
    public boolean reuseCollapsedTokens() {
        return true;
    }
}
