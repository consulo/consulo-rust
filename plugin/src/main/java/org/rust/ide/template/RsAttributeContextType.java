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
import org.rust.lang.core.psi.ext.RsAttr;

/**
 * Matches anywhere inside an attribute.
 */
@ExtensionImpl
public class RsAttributeContextType extends RsContextType {

    public RsAttributeContextType() {
        super("RUST_ATTRIBUTE", LocalizeValue.of(RsBundle.message("label.attribute")), RsItemContextType.class);
    }

    @Override
    protected boolean isInContext(PsiElement element) {
        return PsiTreeUtil.getParentOfType(element, RsAttr.class, true) != null;
    }
}
