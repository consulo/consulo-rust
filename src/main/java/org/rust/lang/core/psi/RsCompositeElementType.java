/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.ICompositeElementType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.RsLanguage;

public class RsCompositeElementType extends IElementType implements ICompositeElementType {

    public RsCompositeElementType(String s) {
        super(s, RsLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public ASTNode createCompositeNode() {
        return RsElementTypes.Factory.createElement(this);
    }
}
