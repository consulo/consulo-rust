/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import org.rust.RsBundle;
import org.rust.lang.core.psi.ext.RsItemElement;

/**
 * Matches directly inside an item.
 */
@ExtensionImpl
public class RsItemContextType extends RsContextType {

    public RsItemContextType() {
        super("RUST_ITEM", LocalizeValue.of(RsBundle.message("label.item")), RsGenericContextType.class);
    }

    @Override
    protected boolean isInContext(PsiElement element) {
        return owner(element) instanceof RsItemElement;
    }
}
