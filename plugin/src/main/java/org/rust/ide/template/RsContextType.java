/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template;

import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.template.context.BaseTemplateContextType;
import consulo.language.editor.template.context.TemplateActionContext;
import consulo.language.editor.template.context.TemplateContextType;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nullable;
import org.rust.ide.highlight.RsHighlighter;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.doc.psi.RsDocComment;

/**
 * Base live template context type for Rust: accepts an offset only when the
 * language at that offset is Rust and the element there is neither a comment
 * nor part of a literal expression. Concrete subtypes narrow it further.
 */
public abstract class RsContextType extends BaseTemplateContextType {

    protected RsContextType(String contextId,
                            LocalizeValue presentableName,
                            @Nullable Class<? extends TemplateContextType> baseContextType) {
        super(contextId, presentableName, baseContextType);
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

    /**
     * Closest ancestor which delimits a template context: a block, a pattern,
     * an item, an attribute, a doc comment or a macro.
     */
    @Nullable
    protected static PsiElement owner(PsiElement element) {
        return PsiTreeUtil.findFirstParent(element, e ->
            e instanceof RsBlock || e instanceof RsPat || e instanceof RsItemElement || e instanceof PsiFile
                || e instanceof RsAttr || e instanceof RsDocComment || e instanceof RsMacro || e instanceof RsMacroCall
        );
    }
}
