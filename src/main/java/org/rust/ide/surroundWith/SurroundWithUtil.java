/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
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
    public static void addStatements(@NotNull RsBlock block, @NotNull PsiElement[] statements) {
        RsPsiFactory factory = new RsPsiFactory(block.getProject());
        block.addBefore(factory.createWhitespace("\n    "), block.getRbrace());
        block.addRangeBefore(statements[0], statements[statements.length - 1], block.getRbrace());
        block.addBefore(factory.createNewline(), block.getRbrace());
    }

    public static void addStatement(@NotNull RsBlock block, @NotNull PsiElement statement) {
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
