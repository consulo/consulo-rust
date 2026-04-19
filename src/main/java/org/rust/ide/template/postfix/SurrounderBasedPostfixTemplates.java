/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.codeInsight.template.postfix.templates.SurroundPostfixTemplateBase;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.surroundWith.expression.RsWithIfExpSurrounder;
import org.rust.ide.surroundWith.expression.RsWithParenthesesSurrounder;
import org.rust.ide.surroundWith.expression.RsWithWhileExpSurrounder;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.utils.NegateUtil;

public final class SurrounderBasedPostfixTemplates {

    private SurrounderBasedPostfixTemplates() {
    }

    public static class IfExpressionPostfixTemplate extends SurroundPostfixTemplateBase {
        public IfExpressionPostfixTemplate(@NotNull PostfixTemplateProvider provider) {
            super("if", "if exp {}", PostfixUtil.RsPostfixTemplatePsiInfo,
                new RsExprParentsSelector(PostfixUtil::isBool), provider);
        }

        @NotNull
        @Override
        protected Surrounder getSurrounder() {
            return new RsWithIfExpSurrounder();
        }
    }

    public static class ElseExpressionPostfixTemplate extends SurroundPostfixTemplateBase {
        public ElseExpressionPostfixTemplate(@NotNull PostfixTemplateProvider provider) {
            super("else", "if !exp {}", PostfixUtil.RsPostfixTemplatePsiInfo,
                new RsExprParentsSelector(PostfixUtil::isBool), provider);
        }

        @NotNull
        @Override
        protected Surrounder getSurrounder() {
            return new RsWithIfExpSurrounder();
        }

        @NotNull
        @Override
        protected PsiElement getWrappedExpression(@NotNull PsiElement expression) {
            return NegateUtil.negate(expression);
        }
    }

    public static class WhileExpressionPostfixTemplate extends SurroundPostfixTemplateBase {
        public WhileExpressionPostfixTemplate(@NotNull PostfixTemplateProvider provider) {
            super("while", "while exp {}", PostfixUtil.RsPostfixTemplatePsiInfo,
                new RsExprParentsSelector(PostfixUtil::isBool), provider);
        }

        @NotNull
        @Override
        protected Surrounder getSurrounder() {
            return new RsWithWhileExpSurrounder();
        }
    }

    public static class WhileNotExpressionPostfixTemplate extends SurroundPostfixTemplateBase {
        public WhileNotExpressionPostfixTemplate(@NotNull PostfixTemplateProvider provider) {
            super("whilenot", "while !exp {}", PostfixUtil.RsPostfixTemplatePsiInfo,
                new RsExprParentsSelector(PostfixUtil::isBool), provider);
        }

        @NotNull
        @Override
        protected Surrounder getSurrounder() {
            return new RsWithWhileExpSurrounder();
        }

        @NotNull
        @Override
        protected PsiElement getWrappedExpression(@NotNull PsiElement expression) {
            return NegateUtil.negate(expression);
        }
    }

    public static class ParenPostfixTemplate extends SurroundPostfixTemplateBase {
        public ParenPostfixTemplate(@NotNull PostfixTemplateProvider provider) {
            super("par", "(expr)", PostfixUtil.RsPostfixTemplatePsiInfo,
                new RsExprParentsSelector(), provider);
        }

        @NotNull
        @Override
        protected Surrounder getSurrounder() {
            return new RsWithParenthesesSurrounder();
        }
    }
}
