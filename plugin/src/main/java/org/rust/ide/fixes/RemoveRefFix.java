/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsUnaryExpr;
import org.rust.lang.core.psi.ext.RsUnaryExprUtil;
import org.rust.lang.core.psi.ext.UnaryOperator;

import java.util.Arrays;

/**
 * Fix that converts the given reference to owned value.
 */
public class RemoveRefFix extends RsQuickFixBase<RsUnaryExpr> {

    
    private final String _text;

    private RemoveRefFix(@Nonnull RsUnaryExpr expr) {
        super(expr);
        UnaryOperator operatorType = RsUnaryExprUtil.getOperatorType(expr);
        if (operatorType == UnaryOperator.REF) {
            _text = RsBundle.message("intention.name.remove", "&");
        } else if (operatorType == UnaryOperator.REF_MUT) {
            _text = RsBundle.message("intention.name.remove", "&mut");
        } else {
            throw new IllegalStateException("Illegal operator type: expected REF or REF_MUT, got " + operatorType);
        }
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(_text);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.remove.reference"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsUnaryExpr element) {
        RsExpr expr = element.getExpr();
        if (expr != null) {
            element.replace(expr);
        }
    }

    @Nullable
    public static RemoveRefFix createIfCompatible(@Nonnull RsExpr expr) {
        if (expr instanceof RsUnaryExpr) {
            UnaryOperator opType = RsUnaryExprUtil.getOperatorType((RsUnaryExpr) expr);
            if (opType == UnaryOperator.REF || opType == UnaryOperator.REF_MUT) {
                return new RemoveRefFix((RsUnaryExpr) expr);
            }
        }
        return null;
    }
}
