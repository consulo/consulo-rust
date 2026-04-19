/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.rust.RsBundle;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyAdt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsEnumItemUtil;
import org.rust.lang.core.psi.ext.RsPatUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class IfLetToMatchIntention extends RsElementBaseIntentionAction<IfLetToMatchIntention.Context> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.convert.if.let.statement.to.match");
    }

    @Override
    public String getFamilyName() {
        return getText();
    }

    public static class Context {
        public final RsIfExpr ifStmt;
        public final RsExpr target;
        public final List<MatchArm> matchArms;
        public RsBlock elseBody;

        public Context(RsIfExpr ifStmt, RsExpr target, List<MatchArm> matchArms, RsBlock elseBody) {
            this.ifStmt = ifStmt;
            this.target = target;
            this.matchArms = matchArms;
            this.elseBody = elseBody;
        }
    }

    public static class MatchArm {
        public final RsPat pat;
        public final RsBlock body;

        public MatchArm(RsPat pat, RsBlock body) {
            this.pat = pat;
            this.body = body;
        }
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        RsIfExpr ifStatement = RsPsiJavaUtil.ancestorStrict(element, RsIfExpr.class);
        if (ifStatement == null) return null;
        RsCondition condition = ifStatement.getCondition();
        if (condition == null) return null;
        RsLetExpr letExpr = condition.getExpr() instanceof RsLetExpr ? (RsLetExpr) condition.getExpr() : null;

        if (element != ifStatement.getIf() && (letExpr == null || (element != letExpr.getLet() && element != letExpr.getEq()))) {
            return null;
        }

        while (ifStatement.getParent() instanceof RsElseBranch) {
            PsiElement grandParent = ifStatement.getParent().getParent();
            if (!(grandParent instanceof RsIfExpr)) return null;
            ifStatement = (RsIfExpr) grandParent;
        }

        return extractIfLetStatementIfAny(ifStatement, null);
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        RsExpr target = ctx.target;
        RsEnumItem item = null;
        if (RsTypesUtil.getType(target) instanceof TyAdt) {
            TyAdt tyAdt = (TyAdt) RsTypesUtil.getType(target);
            if (tyAdt.getItem() instanceof RsEnumItem) {
                item = (RsEnumItem) tyAdt.getItem();
            }
        }

        List<RsPat> optionOrResultPats = ctx.matchArms.stream()
            .map(arm -> arm.pat)
            .filter(IfLetToMatchIntention::isPossibleOptionOrResultVariant)
            .collect(Collectors.toList());

        boolean isIrrefutable = ctx.matchArms.stream().allMatch(arm -> RsPatUtil.isIrrefutable(arm.pat));

        StringBuilder sb = new StringBuilder();
        sb.append("match ").append(target.getText()).append(" {");
        for (MatchArm arm : ctx.matchArms) {
            sb.append('\n').append(arm.pat.getText()).append(" => ").append(arm.body.getText());
        }
        if (ctx.elseBody != null || (!isIrrefutable && (item == null || !allOptionOrResultVariantsCovered(item, optionOrResultPats)))) {
            sb.append('\n').append(missingBranch(item, optionOrResultPats)).append(" => ");
            sb.append(ctx.elseBody != null ? ctx.elseBody.getText() : "{}");
        }
        sb.append("}");

        RsMatchExpr matchExpression = (RsMatchExpr) new RsPsiFactory(project).createExpression(sb.toString());
        ctx.ifStmt.replace(matchExpression);
    }

    private String missingBranch(RsEnumItem item, List<RsPat> pats) {
        if (item == null || !RsEnumItemUtil.isStdOptionOrResult(item)) return "_";
        String patName = pats.size() == 1 ? getPatName(pats.get(0)) : null;
        if (patName == null) return "_";
        switch (patName) {
            case "Some": return "None";
            case "Ok": return "Err(..)";
            case "Err": return "Ok(..)";
            case "None": return "Some(..)";
            default: return "_";
        }
    }

    private Context extractIfLetStatementIfAny(RsIfExpr iflet, Context ctx) {
        RsCondition condition = iflet.getCondition();
        if (condition == null) return null;
        if (!(condition.getExpr() instanceof RsLetExpr)) return null;
        RsLetExpr letExpr = (RsLetExpr) condition.getExpr();

        RsPat pat = letExpr.getPat();
        if (pat == null) return null;
        RsExpr target = letExpr.getExpr();
        if (target == null) return null;
        RsBlock ifBody = iflet.getBlock();
        if (ifBody == null) return null;

        MatchArm matchArm = new MatchArm(pat, ifBody);
        Context context;
        if (ctx != null) {
            if (!ctx.target.getText().equals(target.getText())) return null;
            context = ctx;
            context.matchArms.add(matchArm);
        } else {
            List<MatchArm> arms = new ArrayList<>();
            arms.add(matchArm);
            context = new Context(iflet, target, arms, null);
        }

        if (iflet.getElseBranch() != null) {
            RsElseBranch elseBranch = iflet.getElseBranch();
            if (elseBranch.getIfExpr() != null) {
                context = extractIfLetStatementIfAny(elseBranch.getIfExpr(), context);
                if (context == null) return null;
            } else if (elseBranch.getBlock() != null) {
                context.elseBody = elseBranch.getBlock();
            }
        }

        if (!PsiModificationUtil.canReplace(context.ifStmt)) return null;
        return context;
    }

    private static boolean allOptionOrResultVariantsCovered(RsEnumItem item, List<RsPat> pats) {
        if (item == null || !RsEnumItemUtil.isStdOptionOrResult(item) || pats.size() < 2) return false;
        Set<String> patNames = pats.stream().map(IfLetToMatchIntention::getPatName).collect(Collectors.toSet());
        Set<String> allVariantNames;
        if (item == KnownItems.getKnownItems(item).getOption()) {
            allVariantNames = Set.of("Some", "None");
        } else {
            allVariantNames = Set.of("Ok", "Err");
        }
        return patNames.equals(allVariantNames);
    }

    private static String getPatName(RsPat pat) {
        String text = RsPsiJavaUtil.skipUnnecessaryTupDown(pat).getText();
        int parenIndex = text.indexOf('(');
        return parenIndex >= 0 ? text.substring(0, parenIndex) : text;
    }

    private static boolean isPossibleOptionOrResultVariant(RsPat pat) {
        String name = getPatName(pat);
        if ("None".equals(name)) return true;
        RsPat descendant = RsPsiJavaUtil.descendantOfTypeStrict(pat, RsPat.class);
        return descendant != null && RsPatUtil.isIrrefutable(descendant);
    }
}
