/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsPatRange;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsPatRangeUtil;

public class ReplaceWithInclusiveRangeFix extends RsQuickFixBase<RsPatRange> {

    public ReplaceWithInclusiveRangeFix(@NotNull RsPatRange range) {
        super(range);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.replace.with.inclusive.range");
    }

    @NotNull
    @Override
    public String getText() {
        PsiElement element = myStartElement.getElement();
        if (!(element instanceof RsPatRange)) return getFamilyName();
        RsPatRange range = (RsPatRange) element;
        PsiElement start = RsPatRangeUtil.getStart(range);
        PsiElement end = RsPatRangeUtil.getEnd(range);
        if (start == null || end == null) return getFamilyName();
        return RsBundle.message("intention.name.replace.with2", start.getText() + "..=" + end.getText());
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsPatRange element) {
        PsiElement dotdot = element.getDotdot();
        if (dotdot != null) {
            dotdot.replace(new RsPsiFactory(project).createDotDotEq());
        }
    }
}
