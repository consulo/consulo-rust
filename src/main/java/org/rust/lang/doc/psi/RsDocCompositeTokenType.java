/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.tree.ICompositeElementType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class RsDocCompositeTokenType extends RsDocTokenType implements ICompositeElementType {
    private final Function<IElementType, CompositeElement> astFactory;

    public RsDocCompositeTokenType(String debugName, Function<IElementType, CompositeElement> astFactory) {
        super(debugName);
        this.astFactory = astFactory;
    }

    @NotNull
    @Override
    public CompositeElement createCompositeNode() {
        return astFactory.apply(this);
    }
}
