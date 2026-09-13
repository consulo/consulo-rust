/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import org.rust.ide.fixes.AddRemainingArmsFix;
import org.rust.lang.core.match.CheckMatchUtil;
import org.rust.lang.core.match.Pattern;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsMatchBody;
import org.rust.lang.core.psi.RsMatchExpr;

import java.util.List;
import consulo.localize.LocalizeValue;

public class AddRemainingArmsIntention extends RsElementBaseIntentionAction<AddRemainingArmsIntention.Context> {

    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(AddRemainingArmsFix.NAME);
        }

    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    public static class Context {
        public final RsMatchExpr matchExpr;
        public final RsExpr expr;
        public final List<Pattern> patterns;
        public final AddRemainingArmsFix.ArmsInsertionPlace place;

        public Context(RsMatchExpr matchExpr, RsExpr expr, List<Pattern> patterns, AddRemainingArmsFix.ArmsInsertionPlace place) {
            this.matchExpr = matchExpr;
            this.expr = expr;
            this.patterns = patterns;
            this.place = place;
        }
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        PsiElement parent = element.getContext();
        RsMatchExpr matchExpr;
        if (parent instanceof RsMatchExpr) {
            matchExpr = (RsMatchExpr) parent;
        } else if (parent instanceof RsMatchBody) {
            PsiElement grandParent = parent.getContext();
            matchExpr = grandParent instanceof RsMatchExpr ? (RsMatchExpr) grandParent : null;
        } else {
            matchExpr = null;
        }
        if (matchExpr == null) return null;

        if (matchExpr.getMatch().getTextRange().containsOffset(editor.getCaretModel().getOffset())) return null;

        RsExpr expr = matchExpr.getExpr();
        if (expr == null) return null;
        List<Pattern> patterns = CheckMatchUtil.checkExhaustive(matchExpr);
        if (patterns == null) return null;
        AddRemainingArmsFix.ArmsInsertionPlace place = AddRemainingArmsFix.findArmsInsertionPlaceIn(matchExpr);
        if (place == null) return null;
        return new Context(matchExpr, expr, patterns, place);
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        createQuickFix(ctx.matchExpr, ctx.patterns).invoke(
            project,
            ctx.matchExpr,
            ctx.expr,
            ctx.place
        );
    }

    protected AddRemainingArmsFix createQuickFix(RsMatchExpr matchExpr, List<Pattern> patterns) {
        return new AddRemainingArmsFix(matchExpr, patterns);
    }
}
