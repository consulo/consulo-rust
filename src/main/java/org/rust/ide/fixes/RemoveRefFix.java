/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @IntentionName
    private final String _text;

    private RemoveRefFix(@NotNull RsUnaryExpr expr) {
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

    @NotNull
    @Override
    public String getText() {
        return _text;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.remove.reference");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsUnaryExpr element) {
        RsExpr expr = element.getExpr();
        if (expr != null) {
            element.replace(expr);
        }
    }

    @Nullable
    public static RemoveRefFix createIfCompatible(@NotNull RsExpr expr) {
        if (expr instanceof RsUnaryExpr) {
            UnaryOperator opType = RsUnaryExprUtil.getOperatorType((RsUnaryExpr) expr);
            if (opType == UnaryOperator.REF || opType == UnaryOperator.REF_MUT) {
                return new RemoveRefFix((RsUnaryExpr) expr);
            }
        }
        return null;
    }
}
