/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import consulo.language.ast.ASTNode;
import consulo.language.ast.ILazyParseableElementType;
import consulo.language.impl.ast.SharedImplUtil;
import consulo.language.psi.PsiElement;
import consulo.language.util.CharTable;
import jakarta.annotation.Nonnull;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import org.intellij.markdown.parser.LinkMap;
import org.intellij.plugins.markdown.lang.MarkdownFileType;
import org.rust.lang.RsLanguage;
import org.rust.lang.doc.psi.impl.RsDocCommentImpl;

/**
 * The element type of a doc comment token. The comment content is parsed lazily: the comment
 * decoration is stripped, the remainder is parsed as Markdown and the Markdown tree is turned into
 * the doc comment AST.
 */
public class RsDocCommentElementType extends ILazyParseableElementType {
    public RsDocCommentElementType(String debugName) {
        super(debugName, RsLanguage.INSTANCE);
    }

    @Override
    protected ASTNode doParseContents(@Nonnull ASTNode chameleon, @Nonnull PsiElement psi) {
        CharTable charTable = SharedImplUtil.findCharTableByTree(chameleon);
        RsDocTextMap textMap = RsDocTextMap.create(chameleon.getChars(), RsDocKind.of(this));

        RsDocCommentImpl root = new RsDocCommentImpl(this, null);
        String markdownText = textMap.getMappedText();
        // Parsed by the platform, so what comes back is a real AST rather than a parallel one.
        PsiFile markdownFile = PsiFileFactory.getInstance(psi.getProject())
            .createFileFromText("doc.md", MarkdownFileType.INSTANCE, markdownText);
        ASTNode markdownRoot = markdownFile.getNode();
        LinkMap linkMap = LinkMap.buildLinkMap(markdownRoot, markdownText);
        new RsDocMarkdownAstBuilder(textMap, charTable, linkMap).buildTree(root, markdownRoot);

        return root.getFirstChildNode();
    }

    @Override
    public ASTNode createNode(CharSequence text) {
        return new RsDocCommentImpl(this, text);
    }
}
