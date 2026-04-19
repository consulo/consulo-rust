/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.ILazyParseableElementType;
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor;
import org.intellij.markdown.parser.LinkMap;
import org.intellij.markdown.parser.MarkdownParser;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.RsLanguage;
import org.rust.lang.doc.psi.impl.RsDocCommentImpl;

public class RsDocCommentElementType extends ILazyParseableElementType {
    public RsDocCommentElementType(String debugName) {
        super(debugName, RsLanguage.INSTANCE);
    }

    @Override
    protected ASTNode doParseContents(@NotNull ASTNode chameleon, @NotNull PsiElement psi) {
        // Simplified: actual implementation would require RsDocTextMap and RsDocMarkdownAstBuilder
        RsDocCommentImpl root = new RsDocCommentImpl(this, null);
        return root.getFirstChildNode();
    }

    @Override
    public ASTNode createNode(CharSequence text) {
        return new RsDocCommentImpl(this, text);
    }
}
