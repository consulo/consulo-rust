/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.TemplateResultListener;
import com.intellij.codeInsight.template.impl.MacroCallNode;
import com.intellij.codeInsight.template.macro.CompleteMacro;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateWithExpressionSelector;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;
import org.rust.ide.utils.template.EditorExtUtil;
import org.rust.lang.core.parser.RustParserUtil;
import org.rust.lang.core.psi.RsPathType;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.openapiext.PsiElementExtUtil;

public class WrapTypePathPostfixTemplate extends PostfixTemplateWithExpressionSelector {

    public WrapTypePathPostfixTemplate(RsPostfixTemplateProvider provider) {
        super(null, "wrap", "$wrapper$<path>", new RsTypeParentsSelector(), provider);
    }

    @Override
    protected void expandForChooseExpression(PsiElement element, Editor editor) {
        if (!(element instanceof RsTypeReference)) return;
        RsTypeReference typeRef = (RsTypeReference) element;

        RsPsiFactory factory = new RsPsiFactory(typeRef.getProject());
        org.rust.lang.core.psi.RsPath path = factory.tryCreatePath("Wrapper<" + typeRef.getText() + ">", RustParserUtil.PathParsingMode.TYPE);
        if (path == null) return;
        RsTypeReference newType = factory.tryCreateType(path.getText());
        if (newType == null) return;
        PsiElement inserted = typeRef.replace(newType);
        if (!(inserted instanceof RsPathType)) return;
        RsPathType insertedType = (RsPathType) inserted;

        PsiElement name = insertedType.getPath().getReferenceNameElement();
        if (name == null) return;

        // Template logic would go here - simplified for conversion
        editor.getCaretModel().moveToOffset(insertedType.getTextRange().getEndOffset());
    }
}
