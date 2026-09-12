/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsWhileExpr;
import org.rust.lang.core.psi.ext.RsLabelReferenceOwner;
import consulo.localize.LocalizeValue;

public class RsAddLabelFix extends RsQuickFixBase<RsLabelReferenceOwner> {

    public RsAddLabelFix(@Nonnull RsLabelReferenceOwner element) {
        super(element);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.add.label"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return getFamilyName();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsLabelReferenceOwner element) {
        if (editor == null) return;
        RsWhileExpr whileExpr = PsiTreeUtil.getParentOfType(element, RsWhileExpr.class, true);
        if (whileExpr == null) return;
        RsPsiFactory factory = new RsPsiFactory(project);
        whileExpr.addBefore(factory.createLabelDeclaration("a"), whileExpr.getFirstChild());
        element.add(factory.createLabel("a"));
    }
}
