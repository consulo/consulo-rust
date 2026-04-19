/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.impl.TemplateSettings;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateWithExpressionSelector;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplatesUtils;
import com.intellij.codeInsight.template.postfix.templates.editable.DefaultPostfixTemplateEditor;
import com.intellij.codeInsight.template.postfix.templates.editable.PostfixTemplateEditor;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jdom.Element;
import org.rust.RsBundle;
import org.rust.ide.refactoring.introduceVariable.RsIntroduceVariableUtil;
import org.rust.ide.template.postfix.editable.RsEditablePostfixTemplate;
import org.rust.ide.template.postfix.editable.RsPostfixTemplateEditor;
import org.rust.ide.template.postfix.editable.RsPostfixTemplateExpressionCondition;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsExpr;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RsPostfixTemplateProvider implements PostfixTemplateProvider {

    private final Set<PostfixTemplate> myTemplates;

    public RsPostfixTemplateProvider() {
        myTemplates = new LinkedHashSet<>();
        myTemplates.add(new AssertPostfixTemplate(this));
        myTemplates.add(new DebugAssertPostfixTemplate(this));
        myTemplates.add(new IfExpressionPostfixTemplate(this));
        myTemplates.add(new ElseExpressionPostfixTemplate(this));
        myTemplates.add(new WhileExpressionPostfixTemplate(this));
        myTemplates.add(new WhileNotExpressionPostfixTemplate(this));
        myTemplates.add(new MatchPostfixTemplate(this));
        myTemplates.add(new ParenPostfixTemplate(this));
        myTemplates.add(new LambdaPostfixTemplate(this));
        myTemplates.add(new NotPostfixTemplate(this));
        myTemplates.add(new RefExprPostfixTemplate(this));
        myTemplates.add(new RefmExprPostfixTemplate(this));
        myTemplates.add(new RefTypePostfixTemplate(this));
        myTemplates.add(new RefmTypePostfixTemplate(this));
        myTemplates.add(new DerefPostfixTemplate(this));
        myTemplates.add(new LetPostfixTemplate(this));
        myTemplates.add(new IterPostfixTemplate("iter", this));
        myTemplates.add(new IterPostfixTemplate("for", this));
        myTemplates.add(new PrintlnPostfixTemplate(this));
        myTemplates.add(new DbgPostfixTemplate(this));
        myTemplates.add(new DbgrPostfixTemplate(this));
        myTemplates.add(new OkPostfixTemplate(this));
        myTemplates.add(new SomePostfixTemplate(this));
        myTemplates.add(new ErrPostfixTemplate(this));
        myTemplates.add(new WrapTypePathPostfixTemplate(this));
        myTemplates.add(new SlicePostfixTemplate("slice", this));
        myTemplates.add(new SlicePostfixTemplate("sublist", this));
    }

    @Override
    public Set<PostfixTemplate> getTemplates() {
        return myTemplates;
    }

    @Override
    public boolean isTerminalSymbol(char currentChar) {
        return currentChar == '.' || currentChar == '!';
    }

    @Override
    public void afterExpand(PsiFile file, Editor editor) {
    }

    @Override
    public PsiFile preCheck(PsiFile copyFile, Editor realEditor, int currentOffset) {
        return copyFile;
    }

    @Override
    public void preExpand(PsiFile file, Editor editor) {
    }

    @Override
    public String getPresentableName() {
        return RsLanguage.INSTANCE.getDisplayName();
    }

    @Override
    public PostfixTemplateEditor createEditor(PostfixTemplate templateToEdit) {
        if (!(templateToEdit instanceof RsEditablePostfixTemplate) && templateToEdit != null) {
            return new DefaultPostfixTemplateEditor(this, templateToEdit);
        }
        RsPostfixTemplateEditor editor = new RsPostfixTemplateEditor(this);
        editor.setTemplate(templateToEdit);
        return editor;
    }

    @Override
    public PostfixTemplate readExternalTemplate(String id, String name, Element template) {
        Element templateChild = template.getChild(TemplateSettings.TEMPLATE);
        if (templateChild == null) return null;
        com.intellij.codeInsight.template.impl.TemplateImpl liveTemplate =
            TemplateSettings.readTemplateFromElement("", templateChild, this.getClass().getClassLoader());
        if (liveTemplate == null) return null;

        Set<RsPostfixTemplateExpressionCondition> conditions = new LinkedHashSet<>();
        PostfixTemplatesUtils.readExternalConditions(template, param -> {
            if (param != null) return RsPostfixTemplateExpressionCondition.readExternal(param);
            return null;
        }).forEach(c -> { if (c != null) conditions.add((RsPostfixTemplateExpressionCondition) c); });

        String topmostAttr = template.getAttributeValue(PostfixTemplatesUtils.TOPMOST_ATTR);
        boolean useTopmostExpression = Boolean.parseBoolean(topmostAttr);

        return new RsEditablePostfixTemplate(id, name, liveTemplate.getString(), "", conditions, useTopmostExpression, this);
    }

    @Override
    public void writeExternalTemplate(PostfixTemplate template, Element parentElement) {
        if (template instanceof RsEditablePostfixTemplate) {
            PostfixTemplatesUtils.writeExternalTemplate(template, parentElement);
        }
    }
}
