/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.template.context.EverywhereContextType;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import org.rust.RsBundle;

/**
 * Matches anywhere inside a Rust file.
 */
@ExtensionImpl
public class RsGenericContextType extends RsContextType {

    public RsGenericContextType() {
        super("RUST_FILE", LocalizeValue.of(RsBundle.message("label.rust")), EverywhereContextType.class);
    }

    @Override
    protected boolean isInContext(PsiElement element) {
        return true;
    }
}
