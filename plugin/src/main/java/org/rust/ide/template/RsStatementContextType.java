/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsExprStmt;

/**
 * Matches at the very beginning of an expression statement.
 */
@ExtensionImpl
public class RsStatementContextType extends RsContextType {

    public RsStatementContextType() {
        super("RUST_STATEMENT", LocalizeValue.of(RsBundle.message("label.statement")), RsGenericContextType.class);
    }

    @Override
    protected boolean isInContext(PsiElement element) {
        // An identifier may be parsed together with the statement which follows it, so
        // compare offsets instead of relying on the element being the statement itself.
        RsExprStmt stmt = PsiTreeUtil.getParentOfType(element, RsExprStmt.class, true);
        if (stmt == null) {
            return false;
        }
        return element.getTextRange().getStartOffset() == stmt.getTextRange().getStartOffset();
    }
}
