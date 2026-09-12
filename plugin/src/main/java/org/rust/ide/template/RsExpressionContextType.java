/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsFieldLookup;
import org.rust.lang.core.psi.RsLabel;
import org.rust.lang.core.psi.RsMethodCall;
import org.rust.lang.core.psi.RsPath;

/**
 * Matches where an expression may start: directly inside a block, and not in a
 * qualified path, a field lookup, a method call or a label.
 */
@ExtensionImpl
public class RsExpressionContextType extends RsContextType {

    public RsExpressionContextType() {
        super("RUST_EXPRESSION", LocalizeValue.of(RsBundle.message("label.expression")), RsGenericContextType.class);
    }

    @Override
    protected boolean isInContext(PsiElement element) {
        if (!(owner(element) instanceof RsBlock)) {
            return false;
        }

        PsiElement parent = element.getParent();

        // foo::element
        if (parent instanceof RsPath && ((RsPath) parent).getColoncolon() != null) {
            return false;
        }

        // foo.element
        if (parent instanceof RsFieldLookup) {
            return false;
        }

        // foo.element()
        if (parent instanceof RsMethodCall) {
            return false;
        }

        // 'label
        if (parent instanceof RsLabel) {
            return false;
        }

        return true;
    }
}
