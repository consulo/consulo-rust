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
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsExprUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReplaceWithStdMemDropFix extends RsQuickFixBase<PsiElement> {

    public ReplaceWithStdMemDropFix(@NotNull PsiElement call) {
        super(call);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.replace.with.std.mem.drop");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
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

    private RsExpr createStdMemDropCall(@NotNull Project project, @NotNull Iterable<RsExpr> args) {
        return new RsPsiFactory(project).createFunctionCall("std::mem::drop", args);
    }
}
