/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.rust.ide.fixes.AddRemainingArmsFix;
import org.rust.ide.fixes.AddWildcardArmFix;
import org.rust.ide.utils.checkMatch.Pattern;
import org.rust.lang.core.psi.RsMatchExpr;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

import java.util.List;

public class AddWildcardArmIntention extends AddRemainingArmsIntention {

    @Override
    public String getText() {
        return AddWildcardArmFix.NAME;
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        Context ctx = super.findApplicableContext(project, editor, element);
        if (ctx == null) return null;
        if (RsPsiJavaUtil.getArms(ctx.matchExpr).isEmpty()) return null;
        return ctx;
    }

    @Override
    protected AddRemainingArmsFix createQuickFix(RsMatchExpr matchExpr, List<Pattern> patterns) {
        return new AddWildcardArmFix(matchExpr);
    }
}
