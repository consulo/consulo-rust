/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.RsLanguage;

public class RsElementType extends IElementType {
    public RsElementType(@NotNull String debugName) {
        super(debugName, RsLanguage.INSTANCE);
    }
}
