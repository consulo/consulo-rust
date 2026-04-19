/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.assist;

import com.intellij.lang.SmartEnterProcessorWithFixers;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;

/**
 * Smart enter implementation for the Rust language.
 */
public class RsSmartEnterProcessor extends SmartEnterProcessorWithFixers {

    public RsSmartEnterProcessor() {
        addFixers(
            new MethodCallFixer(),
            new SemicolonFixer(),
            new CommaFixer(),
            new FunctionOrStructFixer()
        );

        addEnterProcessors(
            new AfterSemicolonEnterProcessor(),
            new AfterFunctionOrStructEnterProcessor(),
            new PlainEnterProcessor()
        );
    }

    @Override
    protected PsiElement getStatementAtCaret(Editor editor, PsiFile psiFile) {
        PsiElement atCaret = super.getStatementAtCaret(editor, psiFile);
        if (atCaret == null) return null;
        if (atCaret instanceof PsiWhiteSpace) return null;
        PsiElement element = atCaret;
        while (element != null) {
            com.intellij.psi.tree.IElementType elementType = element.getNode().getElementType();
            if (elementType == RsElementTypes.LBRACE || elementType == RsElementTypes.RBRACE) {
                element = element.getParent();
                continue;
            }

            boolean suitable = isSuitableElement(element);
            PsiElement parent = element.getParent();
            boolean stopAtParent = parent instanceof RsBlock
                || parent instanceof RsFunction
                || parent instanceof RsStructItem;
            if (suitable || stopAtParent) return element;
            element = element.getParent();
        }
        return null;
    }

    @Override
    public boolean doNotStepInto(PsiElement element) {
        return true;
    }

    @Override
    protected void processDefaultEnter(Project project, Editor editor, PsiFile file) {
        plainEnter(editor);
    }

    public static boolean isSuitableElement(PsiElement element) {
        return element instanceof RsMatchArm
            || element instanceof RsTypeAlias
            || element instanceof RsTraitAlias
            || element instanceof RsConstant
            || element instanceof RsExternCrateItem;
    }

    private class PlainEnterProcessor extends FixEnterProcessor {
        @Override
        public boolean doEnter(PsiElement atCaret, PsiFile file, Editor editor, boolean modified) {
            plainEnter(editor);
            return true;
        }
    }
}
