/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.RsLiteralKind;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsLitExprUtil;
import org.rust.lang.core.psi.RsLiteralKindUtil;

public class ConvertToUnsuffixedIntegerFix extends RsQuickFixBase<RsLitExpr> {

    @IntentionName
    private final String textTemplate;

    private ConvertToUnsuffixedIntegerFix(@NotNull RsLitExpr element, @NotNull @IntentionName String textTemplate) {
        super(element);
        this.textTemplate = textTemplate;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.convert.to.unsuffixed.integer");
    }

    @NotNull
    @Override
    public String getText() {
        return String.format(textTemplate, convertToUnsuffixedInteger(myStartElement.getElement()));
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsLitExpr element) {
        String integer = convertToUnsuffixedInteger(element);
        if (integer == null) return;
        RsPsiFactory psiFactory = new RsPsiFactory(project);
        element.replace(psiFactory.createExpression(integer));
    }

    @Nullable
    public static ConvertToUnsuffixedIntegerFix createIfCompatible(@NotNull RsLitExpr element, @NotNull @IntentionName String textTemplate) {
        if (convertToUnsuffixedInteger(element) != null) {
            return new ConvertToUnsuffixedIntegerFix(element, textTemplate);
        }
        return null;
    }

    @Nullable
    @IntentionName
    private static String convertToUnsuffixedInteger(@Nullable PsiElement element) {
        if (element == null) return null;
        if (!(element instanceof RsLitExpr)) return null;

        RsLiteralKind kind = RsLiteralKindUtil.getKind((RsLitExpr) element);
        Long value = null;
        if (kind instanceof RsLiteralKind.IntegerLiteral) {
            value = ((RsLiteralKind.IntegerLiteral) kind).getValue();
        } else if (kind instanceof RsLiteralKind.FloatLiteral) {
            Double dVal = ((RsLiteralKind.FloatLiteral) kind).getValue();
            if (dVal != null) value = dVal.longValue();
        } else if (kind instanceof RsLiteralKind.StringLiteral) {
            String sVal = ((RsLiteralKind.StringLiteral) kind).getValue();
            if (sVal != null) {
                try {
                    value = Long.parseLong(sVal);
                } catch (NumberFormatException ignored) {
                }
            }
        } else if (kind instanceof RsLiteralKind.CharLiteral) {
            String cVal = ((RsLiteralKind.CharLiteral) kind).getValue();
            if (cVal != null) {
                try {
                    value = Long.parseLong(cVal);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (value == null) return null;
        return Long.toString(value);
    }
}
