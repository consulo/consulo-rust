/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import org.rust.RsBundle;
import org.rust.lang.core.psi.ext.RsMod;

/**
 * Matches directly inside a module.
 */
@ExtensionImpl
public class RsModContextType extends RsContextType {

    public RsModContextType() {
        super("RUST_MOD", LocalizeValue.of(RsBundle.message("label.module")), RsItemContextType.class);
    }

    @Override
    protected boolean isInContext(PsiElement element) {
        return owner(element) instanceof RsMod;
    }
}
