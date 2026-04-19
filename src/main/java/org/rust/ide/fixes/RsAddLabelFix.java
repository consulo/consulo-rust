/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsWhileExpr;
import org.rust.lang.core.psi.ext.RsLabelReferenceOwner;

public class RsAddLabelFix extends RsQuickFixBase<RsLabelReferenceOwner> {

    public RsAddLabelFix(@NotNull RsLabelReferenceOwner element) {
        super(element);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.add.label");
    }

    @NotNull
    @Override
    public String getText() {
        return getFamilyName();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsLabelReferenceOwner element) {
        if (editor == null) return;
        RsWhileExpr whileExpr = PsiTreeUtil.getParentOfType(element, RsWhileExpr.class, true);
        if (whileExpr == null) return;
        RsPsiFactory factory = new RsPsiFactory(project);
        whileExpr.addBefore(factory.createLabelDeclaration("a"), whileExpr.getFirstChild());
        element.add(factory.createLabel("a"));
    }
}
