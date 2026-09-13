/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.RsLanguage;

public class RsElementType extends IElementType {
    public RsElementType(@Nonnull String debugName) {
        super(debugName, RsLanguage.INSTANCE);
    }
}
