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
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsLambdaExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsFunctionOrLambda;
import org.rust.lang.core.psi.ext.RsFunctionOrLambdaUtil;

public class MakeAsyncFix extends RsQuickFixBase<RsFunctionOrLambda> {

    private final boolean isFunction;

    public MakeAsyncFix(@Nonnull RsFunctionOrLambda function) {
        super(function);
        this.isFunction = function instanceof RsFunction;
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        String item = isFunction
            ? RsBundle.message("intention.name.function")
            : RsBundle.message("intention.name.lambda");
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.make.async", item));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.make.async"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsFunctionOrLambda element) {
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
