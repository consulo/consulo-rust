/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.spelling;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.spellcheker.tokenizer.TokenConsumer;
import consulo.language.spellcheker.tokenizer.Tokenizer;
import consulo.language.spellcheker.tokenizer.splitter.CommentTokenSplitter;
import jakarta.annotation.Nonnull;
import org.rust.lang.doc.psi.RsDocCodeFence;
import org.rust.lang.doc.psi.RsDocComment;
import org.rust.lang.doc.psi.RsDocElementTypes;

/**
 * Spellchecks the prose of a doc comment.
 * <p>
 * Only the text leaves are checked: the comment decoration ({@code ///}, {@code //!}, the leading
 * {@code *} of a block comment) is held in separate gap leaves and skipped, and code fences are
 * skipped whole because they hold Rust code rather than prose.
 */
public class RsDocCommentTokenizer extends Tokenizer<RsDocComment> {

    public static final RsDocCommentTokenizer INSTANCE = new RsDocCommentTokenizer();

    private RsDocCommentTokenizer() {
    }

    @Override
    public void tokenize(@Nonnull RsDocComment element, @Nonnull TokenConsumer consumer) {
        tokenizeChildren(element, consumer);
    }

    private static void tokenizeChildren(@Nonnull PsiElement element, @Nonnull TokenConsumer consumer) {
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof RsDocCodeFence) continue;

            if (child.getFirstChild() == null) {
                ASTNode node = child.getNode();
                if (node != null && node.getElementType() == RsDocElementTypes.DOC_DATA) {
                    consumer.consumeToken(child, CommentTokenSplitter.getInstance());
                }
            }
            else {
                tokenizeChildren(child, consumer);
            }
        }
    }
}
