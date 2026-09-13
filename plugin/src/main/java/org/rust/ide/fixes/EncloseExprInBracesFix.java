/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsElement;
import consulo.localize.LocalizeValue;

public class EncloseExprInBracesFix extends RsQuickFixBase<PsiElement> {

    public EncloseExprInBracesFix(@Nonnull RsElement element) {
        super(element);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.enclose.expression.in.braces"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement element) {
        var enclosed = new RsPsiFactory(project).createExpression("{ " + element.getText() + " }");
        element.replace(enclosed);
    }
}
