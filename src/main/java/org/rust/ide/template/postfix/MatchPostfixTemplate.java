/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateWithExpressionSelector;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import org.rust.ide.fixes.AddRemainingArmsFix;
import org.rust.ide.fixes.AddWildcardArmFix;
import org.rust.ide.utils.checkMatch.CheckMatchUtil;
import org.rust.ide.utils.template.EditorExtUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.ty.TyReference;
import org.rust.lang.core.types.ty.TyStr;
import org.rust.openapiext.PsiElementExtUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class MatchPostfixTemplate extends PostfixTemplateWithExpressionSelector {

    public MatchPostfixTemplate(RsPostfixTemplateProvider provider) {
        super(null, "match", "match expr {...}", new RsExprParentsSelector(), provider);
    }

    @Override
    protected void expandForChooseExpression(PsiElement expression, Editor editor) {
        if (!(expression instanceof RsExpr)) return;
        RsExpr rsExpr = (RsExpr) expression;

        com.intellij.openapi.project.Project project = expression.getProject();
        RsPsiFactory factory = new RsPsiFactory(project);
        Ty type = RsTypesUtil.getType(rsExpr);

        String exprText;
        if (expression instanceof RsStructLiteral) {
            exprText = "(" + expression.getText() + ")";
        } else {
            exprText = expression.getText();
        }

        RsMatchExpr match;
        if (type instanceof TyAdt && ((TyAdt) type).getItem() == KnownItems.getKnownItems(rsExpr).getString()) {
            match = (RsMatchExpr) factory.createExpression("match " + exprText + ".as_str() {\n\"\" => {}\n_ => {} }");
        } else if (type instanceof TyReference && ((TyReference) type).getReferenced() instanceof TyStr) {
            match = (RsMatchExpr) factory.createExpression("match " + exprText + " {\n\"\" => {}\n_ => {} }");
        } else {
            match = (RsMatchExpr) factory.createExpression("match " + exprText + " {}");
        }

        RsMatchExpr matchExpr = (RsMatchExpr) expression.replace(match);
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

        // For generic match, add arms
        if (!(type instanceof TyAdt && ((TyAdt) type).getItem() == KnownItems.getKnownItems(rsExpr).getString())
            && !(type instanceof TyReference && ((TyReference) type).getReferenced() instanceof TyStr)) {
            List<?> patterns = CheckMatchUtil.checkExhaustive(matchExpr);
            if (patterns == null) patterns = Collections.emptyList();
            if (patterns.isEmpty()) {
                new AddWildcardArmFix(matchExpr).invoke(matchExpr.getProject(), null, matchExpr);
            } else {
                new AddRemainingArmsFix(matchExpr, (List) patterns).invoke(matchExpr.getProject(), null, matchExpr);
            }
        }

        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

        RsMatchBody matchBody = matchExpr.getMatchBody();
        if (matchBody == null) return;
        List<RsMatchArm> arms = matchBody.getMatchArmList();
        if (!arms.isEmpty()) {
            RsExpr firstExpr = arms.get(0).getExpr();
            if (firstExpr instanceof RsBlockExpr) {
                editor.getCaretModel().moveToOffset(((RsBlockExpr) firstExpr).getBlock().getLbrace().getTextOffset() + 1);
            }
        }
    }

    public static void fillMatchArms(RsMatchExpr match, Editor editor) {
        List<?> patterns = CheckMatchUtil.checkExhaustive(match);
        if (patterns == null) patterns = Collections.emptyList();
        if (patterns.isEmpty()) {
            new AddWildcardArmFix(match).invoke(match.getProject(), null, match);
        } else {
            new AddRemainingArmsFix(match, (List) patterns).invoke(match.getProject(), null, match);
        }

        Collection<RsPatWild> wildcards = RsElementUtil.descendantsOfType(match, RsPatWild.class);
        RsMatchBody matchBody = match.getMatchBody();
        if (matchBody == null) return;
        List<RsMatchArm> arms = matchBody.getMatchArmList();
        if (!arms.isEmpty()) {
            RsExpr firstExpr = arms.get(0).getExpr();
            if (firstExpr instanceof RsBlockExpr) {
                editor.getCaretModel().moveToOffset(((RsBlockExpr) firstExpr).getBlock().getLbrace().getTextOffset() + 1);
            }
        }
    }
}
