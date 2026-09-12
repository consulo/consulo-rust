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
import org.rust.lang.core.psi.RsStructItem;

/**
 * Matches anywhere inside a struct definition.
 */
@ExtensionImpl
public class RsStructContextType extends RsContextType {

    public RsStructContextType() {
        super("RUST_STRUCT", LocalizeValue.of(RsBundle.message("label.structure")), RsItemContextType.class);
    }

    @Override
    protected boolean isInContext(PsiElement element) {
        return PsiTreeUtil.getParentOfType(element, RsStructItem.class, true) != null;
    }
}
