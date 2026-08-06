/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsPsiFactory;

/**
 * Utility methods for surround-with operations.
 */
public final class SurroundWithUtil {

    private SurroundWithUtil() {
    }

    /**
     * Shortcut for {@link PsiElement#addRangeAfter}
     */
    public static void addStatements(@Nonnull RsBlock block, @Nonnull PsiElement[] statements) {
        RsPsiFactory factory = new RsPsiFactory(block.getProject());
        block.addBefore(factory.createWhitespace("\n    "), block.getRbrace());
        block.addRangeBefore(statements[0], statements[statements.length - 1], block.getRbrace());
        block.addBefore(factory.createNewline(), block.getRbrace());
    }

    public static void addStatement(@Nonnull RsBlock block, @Nonnull PsiElement statement) {
        RsPsiFactory factory = new RsPsiFactory(block.getProject());
        PsiElement newline = factory.createNewline();
        PsiElement rbrace = block.getRbrace();
        PsiElement prevSibling = rbrace != null ? rbrace.getPrevSibling() : null;
        if (!(prevSibling instanceof PsiWhiteSpace) || !prevSibling.getText().contains("\n")) {
            block.addBefore(newline, rbrace);
        }
        block.addBefore(statement, rbrace);
        block.addBefore(factory.createNewline(), rbrace);
    }
}
