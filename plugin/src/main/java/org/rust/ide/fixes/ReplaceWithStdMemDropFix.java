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
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsExprUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReplaceWithStdMemDropFix extends RsQuickFixBase<PsiElement> {

    public ReplaceWithStdMemDropFix(@Nonnull PsiElement call) {
        super(call);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.replace.with.std.mem.drop"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement element) {
        PsiElement old;
        List<RsExpr> args;

        if (element instanceof RsCallExpr) {
            RsCallExpr callExpr = (RsCallExpr) element;
            List<RsExpr> callArgs = callExpr.getValueArgumentList().getExprList();

            List<RsExpr> dropArgs;
            if (callArgs.size() == 1) {
                RsExpr self = RsExprUtil.unwrapReference(callArgs.get(0));
                dropArgs = Collections.singletonList(self);
            } else {
                dropArgs = callArgs;
            }
            old = callExpr;
            args = dropArgs;
        } else if (element instanceof RsMethodCall) {
            RsMethodCall methodCall = (RsMethodCall) element;
            PsiElement dotExprElement = methodCall.getParent();
            if (!(dotExprElement instanceof RsDotExpr)) return;
            RsDotExpr dotExpr = (RsDotExpr) dotExprElement;
            RsExpr expr = dotExpr.getExpr();
            List<RsExpr> methodArgs = methodCall.getValueArgumentList().getExprList();

            List<RsExpr> dropArgs;
            if (methodArgs.isEmpty()) {
                dropArgs = Collections.singletonList(expr);
            } else {
                dropArgs = new ArrayList<>();
                dropArgs.add(expr);
                dropArgs.addAll(methodArgs);
            }
            old = dotExpr;
            args = dropArgs;
        } else {
            return;
        }

        old.replace(createStdMemDropCall(project, args));
    }

    private RsExpr createStdMemDropCall(@Nonnull Project project, @Nonnull Iterable<RsExpr> args) {
        return new RsPsiFactory(project).createFunctionCall("std::mem::drop", args);
    }
}
