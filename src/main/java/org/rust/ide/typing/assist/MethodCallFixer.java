/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.assist;

import com.intellij.lang.SmartEnterProcessorWithFixers;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.rust.lang.core.psi.RsCallExpr;
import org.rust.lang.core.psi.RsValueArgumentList;

/**
 * Fixer that closes missing function call parenthesis.
 */
public class MethodCallFixer extends SmartEnterProcessorWithFixers.Fixer<RsSmartEnterProcessor> {

    @Override
    public void apply(Editor editor, RsSmartEnterProcessor processor, PsiElement element) {
        if (element instanceof RsCallExpr) {
            RsCallExpr callExpr = (RsCallExpr) element;
            RsValueArgumentList argList = callExpr.getValueArgumentList();
            PsiElement lastChild = argList.getLastChild();
            if (lastChild != null && !lastChild.getText().equals(")")) {
                editor.getDocument().insertString(element.getTextRange().getEndOffset(), ")");
            }
        }
    }
}
