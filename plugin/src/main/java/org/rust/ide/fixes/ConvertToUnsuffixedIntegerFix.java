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
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.RsLiteralKind;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsLitExprUtil;
import org.rust.lang.core.psi.RsLiteralKindUtil;

public class ConvertToUnsuffixedIntegerFix extends RsQuickFixBase<RsLitExpr> {

    
    private final String textTemplate;

    private ConvertToUnsuffixedIntegerFix(@Nonnull RsLitExpr element, @Nonnull  String textTemplate) {
        super(element);
        this.textTemplate = textTemplate;
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.convert.to.unsuffixed.integer"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(String.format(textTemplate, convertToUnsuffixedInteger(myStartElement.getElement())));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsLitExpr element) {
        String integer = convertToUnsuffixedInteger(element);
        if (integer == null) return;
        RsPsiFactory psiFactory = new RsPsiFactory(project);
        element.replace(psiFactory.createExpression(integer));
    }

    @Nullable
    public static ConvertToUnsuffixedIntegerFix createIfCompatible(@Nonnull RsLitExpr element, @Nonnull  String textTemplate) {
        if (convertToUnsuffixedInteger(element) != null) {
            return new ConvertToUnsuffixedIntegerFix(element, textTemplate);
        }
        return null;
    }

    @Nullable
    
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
