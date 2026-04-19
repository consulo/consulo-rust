/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template;

import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.rust.RsBundle;
import org.rust.ide.highlight.RsHighlighter;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.doc.psi.RsDocComment;
import org.rust.lang.core.psi.ext.RsMod;

public abstract class RsContextType extends TemplateContextType {

    protected RsContextType(String presentableName) {
        super(presentableName);
    }

    @Override
    public final boolean isInContext(TemplateActionContext context) {
        if (!PsiUtilCore.getLanguageAtOffset(context.getFile(), context.getStartOffset()).isKindOf(RsLanguage.INSTANCE)) {
            return false;
        }

        PsiElement element = context.getFile().findElementAt(context.getStartOffset());
        if (element == null || element instanceof PsiComment || element.getParent() instanceof RsLitExpr) {
            return false;
        }

        return isInContext(element);
    }

    protected abstract boolean isInContext(PsiElement element);

    @Override
    public SyntaxHighlighter createHighlighter() {
        return new RsHighlighter();
    }

    private static PsiElement owner(PsiElement element) {
        return PsiTreeUtil.findFirstParent(element, e ->
            e instanceof RsBlock || e instanceof RsPat || e instanceof RsItemElement || e instanceof PsiFile
                || e instanceof RsAttr || e instanceof RsDocComment || e instanceof RsMacro || e instanceof RsMacroCall
        );
    }

    public static class Generic extends RsContextType {
        public Generic() {
            super(RsBundle.message("label.rust"));
        }

        @Override
        protected boolean isInContext(PsiElement element) {
            return true;
        }
    }

    public static class Statement extends RsContextType {
        public Statement() {
            super(RsBundle.message("label.statement"));
        }

        @Override
        protected boolean isInContext(PsiElement element) {
            RsExprStmt stmt = PsiTreeUtil.getParentOfType(element, RsExprStmt.class, true);
            if (stmt == null) return false;
            return element.getTextRange().getStartOffset() == stmt.getTextRange().getStartOffset();
        }
    }

    public static class Expression extends RsContextType {
        public Expression() {
            super(RsBundle.message("label.expression"));
        }

        @Override
        protected boolean isInContext(PsiElement element) {
            if (!(owner(element) instanceof RsBlock)) return false;
            PsiElement parent = element.getParent();
            if (parent instanceof RsPath && ((RsPath) parent).getColoncolon() != null) return false;
            if (parent instanceof RsFieldLookup) return false;
            if (parent instanceof RsMethodCall) return false;
            if (parent instanceof RsLabel) return false;
            return true;
        }
    }

    public static class Item extends RsContextType {
        public Item() {
            super(RsBundle.message("label.item"));
        }

        @Override
        protected boolean isInContext(PsiElement element) {
            return owner(element) instanceof RsItemElement;
        }
    }

    public static class Struct extends RsContextType {
        public Struct() {
            super(RsBundle.message("label.structure"));
        }

        @Override
        protected boolean isInContext(PsiElement element) {
            return PsiTreeUtil.getParentOfType(element, RsStructItem.class, true) != null;
        }
    }

    public static class Mod extends RsContextType {
        public Mod() {
            super(RsBundle.message("label.module"));
        }

        @Override
        protected boolean isInContext(PsiElement element) {
            return owner(element) instanceof RsMod;
        }
    }

    public static class Attribute extends RsContextType {
        public Attribute() {
            super(RsBundle.message("label.attribute"));
        }

        @Override
        protected boolean isInContext(PsiElement element) {
            return PsiTreeUtil.getParentOfType(element, RsAttr.class, true) != null;
        }
    }
}
