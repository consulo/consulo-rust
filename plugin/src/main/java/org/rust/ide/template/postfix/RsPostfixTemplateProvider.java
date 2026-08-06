/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.TemplateSettings;
import consulo.language.editor.postfixTemplate.PostfixTemplate;
import consulo.language.editor.postfixTemplate.PostfixTemplateProvider;
import consulo.language.editor.refactoring.postfixTemplate.PostfixTemplateWithExpressionSelector;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplatesUtils;
import com.intellij.codeInsight.template.postfix.templates.editable.DefaultPostfixTemplateEditor;
import com.intellij.codeInsight.template.postfix.templates.editable.PostfixTemplateEditor;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
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

public class RsPostfixTemplateProvider extends PostfixTemplateProvider {

    public RsPostfixTemplateProvider() {
    }

    @jakarta.annotation.Nonnull
    @Override
    public consulo.language.Language getLanguage() { return RsLanguage.INSTANCE; }

    @Override
    protected Set<PostfixTemplate> buildTemplates() {
        Set<PostfixTemplate> myTemplates = new LinkedHashSet<>();
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

}
