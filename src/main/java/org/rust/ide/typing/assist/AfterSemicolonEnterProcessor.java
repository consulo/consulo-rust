/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.assist;

import com.intellij.lang.SmartEnterProcessorWithFixers;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.rust.lang.core.psi.RsExprStmt;
import org.rust.lang.core.psi.RsLetDecl;

public class AfterSemicolonEnterProcessor extends SmartEnterProcessorWithFixers.FixEnterProcessor {

    @Override
    public boolean doEnter(PsiElement atCaret, PsiFile file, Editor editor, boolean modified) {
        if (!modified) return false;

        boolean isSuitableElement = RsSmartEnterProcessor.isSuitableElement(atCaret)
            || atCaret instanceof RsExprStmt || atCaret instanceof RsLetDecl;
        if (!isSuitableElement) return false;

        int elementEndOffset = atCaret.getTextRange().getEndOffset();
        editor.getCaretModel().moveToOffset(elementEndOffset);
        return true;
    }
}
