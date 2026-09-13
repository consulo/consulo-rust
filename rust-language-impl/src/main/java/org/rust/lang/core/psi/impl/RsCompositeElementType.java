/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.impl;

import consulo.language.ast.ASTNode;
import consulo.language.ast.ICompositeElementType;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.RsLanguage;
import consulo.language.impl.ast.CompositeElement;
import org.rust.lang.core.psi.*;

public class RsCompositeElementType extends IElementType implements ICompositeElementType {

    public RsCompositeElementType(String s) {
        super(s, RsLanguage.INSTANCE);
    }

    @Nonnull
    @Override
    public ASTNode createCompositeNode() {
        return new consulo.language.impl.ast.CompositeElement(this);
    }
}
