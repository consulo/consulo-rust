/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import consulo.language.editor.postfixTemplate.PostfixTemplateProvider;
import consulo.language.editor.refactoring.postfixTemplate.SurroundPostfixTemplateBase;
import consulo.language.editor.surroundWith.Surrounder;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.ide.surroundWith.expression.RsWithIfExpSurrounder;
import org.rust.ide.surroundWith.expression.RsWithParenthesesSurrounder;
import org.rust.ide.surroundWith.expression.RsWithWhileExpSurrounder;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.utils.NegateUtil;

public final class SurrounderBasedPostfixTemplates {

    private SurrounderBasedPostfixTemplates() {
    }

    public static class IfExpressionPostfixTemplate extends SurroundPostfixTemplateBase {
        public IfExpressionPostfixTemplate(@Nonnull PostfixTemplateProvider provider) {
            super("if", "if exp {}", PostfixUtil.RsPostfixTemplatePsiInfo,
                new RsExprParentsSelector(PostfixUtil::isBool), provider);
        }

        @Nonnull
        @Override
        protected Surrounder getSurrounder() {
            return new RsWithIfExpSurrounder();
        }
    }

    public static class ElseExpressionPostfixTemplate extends SurroundPostfixTemplateBase {
        public ElseExpressionPostfixTemplate(@Nonnull PostfixTemplateProvider provider) {
            super("else", "if !exp {}", PostfixUtil.RsPostfixTemplatePsiInfo,
                new RsExprParentsSelector(PostfixUtil::isBool), provider);
        }

        @Nonnull
        @Override
        protected Surrounder getSurrounder() {
            return new RsWithIfExpSurrounder();
        }

        @Nonnull
        @Override
        protected PsiElement getWrappedExpression(@Nonnull PsiElement expression) {
            return NegateUtil.negate(expression);
        }
    }

    public static class WhileExpressionPostfixTemplate extends SurroundPostfixTemplateBase {
        public WhileExpressionPostfixTemplate(@Nonnull PostfixTemplateProvider provider) {
            super("while", "while exp {}", PostfixUtil.RsPostfixTemplatePsiInfo,
                new RsExprParentsSelector(PostfixUtil::isBool), provider);
        }

        @Nonnull
        @Override
        protected Surrounder getSurrounder() {
            return new RsWithWhileExpSurrounder();
        }
    }

    public static class WhileNotExpressionPostfixTemplate extends SurroundPostfixTemplateBase {
        public WhileNotExpressionPostfixTemplate(@Nonnull PostfixTemplateProvider provider) {
            super("whilenot", "while !exp {}", PostfixUtil.RsPostfixTemplatePsiInfo,
                new RsExprParentsSelector(PostfixUtil::isBool), provider);
        }

        @Nonnull
        @Override
        protected Surrounder getSurrounder() {
            return new RsWithWhileExpSurrounder();
        }

        @Nonnull
        @Override
        protected PsiElement getWrappedExpression(@Nonnull PsiElement expression) {
            return NegateUtil.negate(expression);
        }
    }

    public static class ParenPostfixTemplate extends SurroundPostfixTemplateBase {
        public ParenPostfixTemplate(@Nonnull PostfixTemplateProvider provider) {
            super("par", "(expr)", PostfixUtil.RsPostfixTemplatePsiInfo,
                new RsExprParentsSelector(), provider);
        }

        @Nonnull
        @Override
        protected Surrounder getSurrounder() {
            return new RsWithParenthesesSurrounder();
        }
    }
}
