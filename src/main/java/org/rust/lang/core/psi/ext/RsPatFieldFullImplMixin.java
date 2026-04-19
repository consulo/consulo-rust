/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsPatFieldFull;
import org.rust.lang.core.psi.RsPatStruct;
import org.rust.lang.core.resolve.NameResolution;
import org.rust.lang.core.resolve.Processors;
import org.rust.lang.core.resolve.ref.ResolveCacheDependency;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.resolve.ref.RsReferenceCached;

import java.util.List;

public abstract class RsPatFieldFullImplMixin extends RsElementImpl implements RsPatFieldFull {

    public RsPatFieldFullImplMixin(@NotNull IElementType type) {
        super(type);
    }

    @NotNull
    @Override
    public PsiElement getReferenceNameElement() {
        PsiElement identifier = getIdentifier();
        return identifier != null ? identifier : getIntegerLiteral();
    }

    @NotNull
    @Override
    public RsReference getReference() {
        return new RsReferenceCached<RsPatFieldFull>(this) {
            @NotNull
            @Override
            protected List<RsElement> resolveInner() {
                return Processors.collectResolveVariants(
                    getElement().getReferenceName(),
                    processor -> NameResolution.processStructPatternFieldResolveVariants(getElement(), processor)
                );
            }

            @NotNull
            @Override
            public ResolveCacheDependency getCacheDependency() {
                return ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE;
            }
        };
    }
}
