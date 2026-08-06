package com.intellij.codeInsight.template.postfix.templates;
import consulo.language.editor.refactoring.postfixTemplate.PostfixTemplateExpressionSelector;
import consulo.language.editor.refactoring.postfixTemplate.PostfixTemplateWithExpressionSelector;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nullable;
/** IntelliJ-compat stub: wraps PostfixTemplateWithExpressionSelector with a string-template method. */
public abstract class StringBasedPostfixTemplate extends PostfixTemplateWithExpressionSelector {
    protected StringBasedPostfixTemplate(String name, String example, PostfixTemplateExpressionSelector selector) {
        super(name, example, selector);
    }
    protected StringBasedPostfixTemplate(String name, String key, String example, PostfixTemplateExpressionSelector selector) {
        super(name, key, example, selector);
    }
    protected StringBasedPostfixTemplate(String name, String example,
                                         PostfixTemplateExpressionSelector selector,
                                         Object provider) {
        super(name, example, selector);
    }
    protected StringBasedPostfixTemplate(String name, String key, String example,
                                         PostfixTemplateExpressionSelector selector,
                                         Object provider) {
        super(name, key, example, selector);
    }
    @Nullable public abstract String getTemplateString(PsiElement element);
    @Nullable public PsiElement getElementToRemove(PsiElement expr) { return null; }
    @Override
    protected void expandForChooseExpression(@jakarta.annotation.Nonnull PsiElement expression, @jakarta.annotation.Nonnull consulo.codeEditor.Editor editor) {
        // stub — template expansion not wired into Consulo yet
    }
}
