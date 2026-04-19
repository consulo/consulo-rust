/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.assist;

import com.intellij.lang.SmartEnterProcessorWithFixers;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsMembers;
import org.rust.lang.core.psi.ext.RsMod;

/**
 * Fixer that adds missing semicolons at the end of statements.
 */
public class SemicolonFixer extends SmartEnterProcessorWithFixers.Fixer<RsSmartEnterProcessor> {

    @Override
    public void apply(Editor editor, RsSmartEnterProcessor processor, PsiElement element) {
        PsiElement parent = element.getParent();
        if (!(parent instanceof RsBlock) && !(parent instanceof RsMod) && !(parent instanceof RsMembers)) return;
        if (element.getNode().findChildByType(RsElementTypes.SEMICOLON) != null) return;
        editor.getDocument().insertString(element.getTextRange().getEndOffset(), ";");
    }
}
