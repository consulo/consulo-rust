/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.assist;

import consulo.language.editor.action.SmartEnterProcessorWithFixers;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import org.rust.lang.RsLanguage;

/**
 * Smart enter implementation for the Rust language.
 */
@ExtensionImpl
public class RsSmartEnterProcessor extends SmartEnterProcessorWithFixers {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


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
            consulo.language.ast.IElementType elementType = element.getNode().getElementType();
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
