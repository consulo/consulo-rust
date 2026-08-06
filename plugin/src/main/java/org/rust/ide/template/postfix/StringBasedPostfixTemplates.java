/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import consulo.language.editor.template.Template;
import consulo.language.editor.template.TextExpression;
import consulo.language.editor.refactoring.postfixTemplate.PostfixTemplateExpressionSelector;
import com.intellij.codeInsight.template.postfix.templates.StringBasedPostfixTemplate;
import consulo.language.psi.PsiElement;
import org.rust.lang.core.psi.RsBinaryExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsUnaryExpr;
import org.rust.lang.core.psi.ext.RsBinaryOpUtil;
import org.rust.lang.core.psi.ext.EqualityOp;
import org.rust.lang.core.psi.ext.RsUnaryExprUtil;
import org.rust.lang.core.psi.ext.UnaryOperator;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.*;

/**
 * Container for string-based postfix templates.
 */
public final class StringBasedPostfixTemplates {
    private StringBasedPostfixTemplates() {}
}
