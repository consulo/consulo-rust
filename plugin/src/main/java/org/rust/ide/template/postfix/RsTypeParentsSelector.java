/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import consulo.language.editor.refactoring.postfixTemplate.PostfixTemplateExpressionSelectorBase;
import consulo.document.Document;
import consulo.util.lang.function.Condition;
import consulo.language.psi.PsiElement;
import org.rust.lang.core.psi.RsTypeReference;

import java.util.ArrayList;
import java.util.List;

public class RsTypeParentsSelector extends PostfixTemplateExpressionSelectorBase {

    public RsTypeParentsSelector() {
        super((Condition<PsiElement>) element -> element instanceof RsTypeReference);
    }

    @Override
    protected List<PsiElement> getNonFilteredExpressions(PsiElement context, Document document, int offset) {
        List<PsiElement> result = new ArrayList<>();
        PsiElement current = context;
        while (current != null) {
            if (current instanceof RsTypeReference) {
                result.add(current);
            }
            current = current.getParent();
        }
        return result;
    }
}
