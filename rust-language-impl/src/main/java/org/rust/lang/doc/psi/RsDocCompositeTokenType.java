/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import consulo.language.impl.ast.CompositeElement;
import consulo.language.ast.ICompositeElementType;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;

import java.util.function.Function;

public class RsDocCompositeTokenType extends RsDocTokenType implements ICompositeElementType {
    private final Function<IElementType, CompositeElement> astFactory;

    public RsDocCompositeTokenType(String debugName, Function<IElementType, CompositeElement> astFactory) {
        super(debugName);
        this.astFactory = astFactory;
    }

    @Nonnull
    @Override
    public CompositeElement createCompositeNode() {
        return astFactory.apply(this);
    }
}
