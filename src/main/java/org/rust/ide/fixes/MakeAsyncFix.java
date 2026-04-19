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
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsLambdaExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsFunctionOrLambda;
import org.rust.lang.core.psi.ext.RsFunctionOrLambdaUtil;

public class MakeAsyncFix extends RsQuickFixBase<RsFunctionOrLambda> {

    private final boolean isFunction;

    public MakeAsyncFix(@NotNull RsFunctionOrLambda function) {
        super(function);
        this.isFunction = function instanceof RsFunction;
    }

    @NotNull
    @Override
    public String getText() {
        String item = isFunction
            ? RsBundle.message("intention.name.function")
            : RsBundle.message("intention.name.lambda");
        return RsBundle.message("intention.name.make.async", item);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.make.async");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsFunctionOrLambda element) {
        if (RsFunctionOrLambdaUtil.isAsync(element)) return;
        PsiElement anchor;
        if (element instanceof RsFunction) {
            RsFunction fn = (RsFunction) element;
            if (fn.getUnsafe() != null) {
                anchor = fn.getUnsafe();
            } else if (fn.getExternAbi() != null) {
                anchor = fn.getExternAbi();
            } else {
                anchor = fn.getFn();
            }
        } else if (element instanceof RsLambdaExpr) {
            RsLambdaExpr lambda = (RsLambdaExpr) element;
            if (lambda.getMove() != null) {
                anchor = lambda.getMove();
            } else {
                anchor = lambda.getValueParameterList();
            }
        } else {
            throw new IllegalStateException("unreachable");
        }
        element.addBefore(new RsPsiFactory(project).createAsyncKeyword(), anchor);
    }
}
