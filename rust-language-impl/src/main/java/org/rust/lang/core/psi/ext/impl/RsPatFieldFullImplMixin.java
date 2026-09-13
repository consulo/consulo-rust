/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.psi.PsiElement;
import consulo.language.ast.ASTNode;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsPatFieldFull;
import org.rust.lang.core.resolve.NameResolution;
import org.rust.lang.core.resolve.Processors;
import org.rust.lang.core.resolve.ref.ResolveCacheDependency;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.resolve.ref.RsReferenceCached;

import java.util.List;
import org.rust.lang.core.psi.ext.*;

public abstract class RsPatFieldFullImplMixin extends RsElementImpl implements RsPatFieldFull {

    public RsPatFieldFullImplMixin(@Nonnull ASTNode type) {
        super(type);
    }

    @Nonnull
    @Override
    public PsiElement getReferenceNameElement() {
        PsiElement identifier = getIdentifier();
        return identifier != null ? identifier : getIntegerLiteral();
    }

    @Nonnull
    @Override
    public RsReference getReference() {
        return new RsReferenceCached<RsPatFieldFull>(this) {
            @Nonnull
            @Override
            protected List<RsElement> resolveInner() {
                return Processors.collectResolveVariants(
                    getElement().getReferenceName(),
                    processor -> NameResolution.processStructPatternFieldResolveVariants(getElement(), processor)
                );
            }

            @Nonnull
            @Override
            public ResolveCacheDependency getCacheDependency() {
                return ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE;
            }
        };
    }
}
