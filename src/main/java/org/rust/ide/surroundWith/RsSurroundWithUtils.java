/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsPsiFactory;

public final class RsSurroundWithUtils {

    private RsSurroundWithUtils() {
    }

    /**
     * Shortcut for {@link PsiElement#addRangeAfter}
     */
    public static void addStatements(RsBlock block, PsiElement[] statements) {
        RsPsiFactory factory = new RsPsiFactory(block.getProject());
        block.addBefore(factory.createWhitespace("\n    "), block.getRbrace());
        block.addRangeBefore(statements[0], statements[statements.length - 1], block.getRbrace());
        block.addBefore(factory.createNewline(), block.getRbrace());
    }

    public static void addStatement(RsBlock block, PsiElement statement) {
        RsPsiFactory factory = new RsPsiFactory(block.getProject());
        PsiElement newline = factory.createNewline();
        PsiElement rbrace = block.getRbrace();
        if (rbrace != null) {
            PsiElement prevSibling = rbrace.getPrevSibling();
            boolean hasNewline = prevSibling instanceof PsiWhiteSpace && prevSibling.getText().contains("\n");
            if (!hasNewline) {
                block.addBefore(newline, rbrace);
            }
            block.addBefore(statement, rbrace);
            block.addBefore(factory.createNewline(), rbrace);
        }
    }
}
