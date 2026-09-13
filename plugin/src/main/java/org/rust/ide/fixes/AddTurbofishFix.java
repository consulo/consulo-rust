/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.RsElementBaseIntentionAction;
import org.rust.lang.RsFileType;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.impl.ArithmeticOp;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.impl.RsPsiJavaUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.impl.RsBinaryExprUtil;
import consulo.localize.LocalizeValue;

public class AddTurbofishFix extends RsElementBaseIntentionAction<AddTurbofishFix.Context> {

    private static final String TURBOFISH = "::";

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
        RsBinaryExpr matchExpr = ctx.myMatchExpr;
        int insertion = ctx.myOffset;
        String expression = matchExpr.getText();
        String left = expression.substring(0, insertion);
        String right = expression.substring(insertion);
        RsExpr fixed = create(project, left + TURBOFISH + right, RsExpr.class);
        if (fixed == null) return;
        matchExpr.replace(fixed);
    }

    public static class Context {
        public final RsBinaryExpr myMatchExpr;
        public final int myOffset;

        public Context(@Nonnull RsBinaryExpr matchExpr, int offset) {
            this.myMatchExpr = matchExpr;
            this.myOffset = offset;
        }
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.add.turbofish.operator"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Nullable
    private RsBinaryExpr resolveMatchExpression(@Nonnull PsiElement element) {
        RsBinaryExpr base = RsPsiJavaUtil.ancestorStrict(element, RsBinaryExpr.class);
        if (base == null) return null;
        if (!(base.getLeft() instanceof RsBinaryExpr)) {
            RsBinaryExpr resolved = resolveMatchExpression(base);
            return resolved != null ? resolved : base;
        }
        RsBinaryExpr left = (RsBinaryExpr) base.getLeft();
        if (RsBinaryExprUtil.getOperatorType(left) == ArithmeticOp.SHR) {
            RsBinaryExpr resolved = resolveMatchExpression(base);
            return resolved != null ? resolved : base;
        }
        return base;
    }

    @Nullable
    @Override
    public Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        RsBinaryExpr m = resolveMatchExpression(element);
        if (m == null) return null;
        return guessContext(project, m);
    }

    @Nullable
    private Integer innerOffset(@Nonnull PsiElement root, @Nonnull PsiElement child) {
        if (child == root) {
            return 0;
        }
        PsiElement parent = child.getParent();
        if (parent == null) return null;
        Integer upper = innerOffset(root, parent);
        if (upper == null) return null;
        return upper + child.getStartOffsetInParent();
    }

    @Nullable
    private Context guessContext(@Nonnull Project project, @Nonnull RsBinaryExpr binary) {
        List<RsExpr> nodes = bfsLeafs(binary);
        RsExpr called = rightBoundary(nodes);
        if (called == null) return null;
        int typeListEndIndex = binary.getTextLength() - called.getTextLength();

        for (RsExpr node : nodes) {
            if (node == called) break;
            Integer offset = innerOffset(binary, node);
            if (offset == null) continue;
            int off = offset + node.getTextLength();
            String typeListCandidate = binary.getText().substring(off, typeListEndIndex);
            if (isTypeArgumentList(project, typeListCandidate)) {
                return new Context(binary, off);
            }
        }
        return null;
    }

    @Nullable
    private RsExpr rightBoundary(@Nonnull List<RsExpr> nodes) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            if (isCallExpression(nodes.get(i))) {
                return nodes.get(i);
            }
        }
        return null;
    }

    private boolean isTypeArgumentList(@Nonnull Project project, @Nonnull String candidate) {
        return create(project, "something" + TURBOFISH + candidate + "()", RsCallExpr.class) != null;
    }

    @Nonnull
    private List<RsExpr> bfsLeafs(@Nullable RsExpr expr) {
        if (expr instanceof RsBinaryExpr) {
            RsBinaryExpr bin = (RsBinaryExpr) expr;
            List<RsExpr> result = new ArrayList<>(bfsLeafs(bin.getLeft()));
            result.addAll(bfsLeafs(bin.getRight()));
            return result;
        }
        if (expr == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(expr);
    }

    private boolean isCallExpression(@Nonnull RsExpr expr) {
        return expr instanceof RsParenExpr || expr.getFirstChild() instanceof RsParenExpr;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private <T extends RsElement> T create(@Nonnull Project project, @Nonnull String text, @Nonnull Class<T> clazz) {
        return createFromText(project, "fn main() { " + text + "; }", clazz);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private <T extends RsElement> T createFromText(@Nonnull Project project, @Nonnull String code, @Nonnull Class<T> clazz) {
        PsiElement file = PsiFileFactory.getInstance(project)
            .createFileFromText("DUMMY.rs", RsFileType.INSTANCE, code);
        return RsPsiJavaUtil.descendantOfTypeStrict(file, clazz);
    }
}
