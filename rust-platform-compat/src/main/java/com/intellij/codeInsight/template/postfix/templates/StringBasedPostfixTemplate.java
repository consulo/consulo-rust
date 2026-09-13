package com.intellij.codeInsight.template.postfix.templates;

import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.postfixTemplate.PostfixTemplateProvider;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplatesUtils;
import consulo.language.editor.refactoring.postfixTemplate.PostfixTemplateExpressionSelector;
import consulo.language.editor.refactoring.postfixTemplate.PostfixTemplateWithExpressionSelector;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateManager;
import consulo.language.editor.template.TextExpression;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Postfix template whose expansion is driven by the template text returned from
 * {@link #getTemplateString}. The selected expression is removed from the document and replaced by a
 * live template built from that text; {@code $expr$} is pre-bound to the original expression text.
 */
public abstract class StringBasedPostfixTemplate extends PostfixTemplateWithExpressionSelector {

    /** Name of the template variable holding the text of the expression the template was invoked on. */
    public static final String EXPR = "expr";

    protected StringBasedPostfixTemplate(String name, String example,
                                         PostfixTemplateExpressionSelector selector,
                                         PostfixTemplateProvider provider) {
        super(null, name, example, selector, provider);
    }

    protected StringBasedPostfixTemplate(String name, String key, String example,
                                         PostfixTemplateExpressionSelector selector,
                                         PostfixTemplateProvider provider) {
        super(null, name, key, example, selector, provider);
    }

    /** Template text to expand for {@code element}, or {@code null} when the template does not apply. */
    @Nullable
    public abstract String getTemplateString(PsiElement element);

    /** Element replaced by the expansion; by default the parent of the selected expression. */
    @Nullable
    public PsiElement getElementToRemove(PsiElement expr) {
        return expr.getParent();
    }

    /** Binds the template variables referenced by {@link #getTemplateString}. */
    public void setVariables(@Nonnull Template template, @Nonnull PsiElement element) {
    }

    /** Whether the expanded text is reformatted after the template finishes. */
    protected boolean shouldReformat() {
        return true;
    }

    @Nonnull
    public Template createTemplate(@Nonnull TemplateManager manager, @Nonnull String templateString) {
        Template template = manager.createTemplate("", "", templateString);
        template.setToReformat(shouldReformat());
        return template;
    }

    @Override
    protected void expandForChooseExpression(@Nonnull PsiElement expression, @Nonnull Editor editor) {
        Project project = expression.getProject();
        Document document = editor.getDocument();

        PsiElement elementToRemove = getElementToRemove(expression);
        if (elementToRemove != null) {
            TextRange range = elementToRemove.getTextRange();
            document.deleteString(range.getStartOffset(), range.getEndOffset());
        }

        String templateString = getTemplateString(expression);
        if (templateString == null) {
            consulo.language.editor.postfixTemplate.PostfixTemplatesUtils.showErrorHint(project, editor);
            return;
        }

        TemplateManager manager = TemplateManager.getInstance(project);
        Template template = createTemplate(manager, templateString);
        template.addVariable(EXPR, new TextExpression(expression.getText()), false);
        setVariables(template, expression);
        manager.startTemplate(editor, template);
    }
}
