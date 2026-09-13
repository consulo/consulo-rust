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
import org.rust.lang.core.psi.RsPatRange;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.ext.impl.RsPatRangeUtil;
import consulo.localize.LocalizeValue;

public class ReplaceWithInclusiveRangeFix extends RsQuickFixBase<RsPatRange> {

    public ReplaceWithInclusiveRangeFix(@Nonnull RsPatRange range) {
        super(range);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.replace.with.inclusive.range"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        PsiElement element = myStartElement.getElement();
        if (!(element instanceof RsPatRange)) return getFamilyName();
        RsPatRange range = (RsPatRange) element;
        PsiElement start = RsPatRangeUtil.getStart(range);
        PsiElement end = RsPatRangeUtil.getEnd(range);
        if (start == null || end == null) return getFamilyName();
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.replace.with2", start.getText() + "..=" + end.getText()));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsPatRange element) {
        PsiElement dotdot = element.getDotdot();
        if (dotdot != null) {
            dotdot.replace(new RsPsiFactory(project).createDotDotEq());
        }
    }
}
