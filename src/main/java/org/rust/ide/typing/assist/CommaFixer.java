/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.assist;

import com.intellij.lang.SmartEnterProcessorWithFixers;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.rust.lang.core.psi.RsBlockExpr;
import org.rust.lang.core.psi.RsMatchArm;

/**
 * Fixer that adds missing comma at the end of statements.
 */
public class CommaFixer extends SmartEnterProcessorWithFixers.Fixer<RsSmartEnterProcessor> {

    @Override
    public void apply(Editor editor, RsSmartEnterProcessor processor, PsiElement element) {
        if (element instanceof RsMatchArm) {
            RsMatchArm matchArm = (RsMatchArm) element;
            if (!(matchArm.getExpr() instanceof RsBlockExpr) && matchArm.getComma() == null) {
                editor.getDocument().insertString(element.getTextRange().getEndOffset(), ",");
            }
        }
    }
}
