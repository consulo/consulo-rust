/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.impl.psi.stub.StubBasedPsiElementBase;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.stubs.RsAttributeOwnerStub;

import java.util.function.Function;
import java.util.function.Predicate;
import org.rust.lang.core.crate.Crate;

/**
 * A helper to check attribute existence on a PSI element backed by a stub.
 * When a stub is available, uses the quick stub-based check.
 * Otherwise, falls through to checking attributes on the PSI.
 *
 * @param <T> the PSI element type
 * @param <S> the stub type
 */
public final class StubbedAttributeProperty<T extends RsDocAndAttributeOwner, S extends RsAttributeOwnerStub> {

    @Nonnull
    private final Predicate<QueryAttributes<?>> psiCheck;
    @Nonnull
    private final Function<S, Boolean> stubCheck;

    public StubbedAttributeProperty(@Nonnull Predicate<QueryAttributes<?>> psiCheck,
                                    @Nonnull Function<S, Boolean> stubCheck) {
        this.psiCheck = psiCheck;
        this.stubCheck = stubCheck;
    }

    @SuppressWarnings("unchecked")
    public boolean getByPsi(@Nonnull T psi) {
        if (psi instanceof StubBasedPsiElementBase<?>) {
            Object stub = ((StubBasedPsiElementBase<?>) psi).getStub();
            if (stub instanceof RsAttributeOwnerStub) {
                return stubCheck.apply((S) stub);
            }
        }
        return getByQueryAttributes(RsDocAndAttributeOwnerUtil.getQueryAttributes(psi));
    }

    public boolean getByQueryAttributes(@Nonnull QueryAttributes<?> queryAttributes) {
        return psiCheck.test(queryAttributes);
    }

    /**
     * Check using the stub directly (without a crate parameter).
     */
    public boolean getByStub(@Nonnull S stub) {
        return stubCheck.apply(stub);
    }

    /**
     * Check using the stub directly with a crate parameter (crate is ignored, kept for API compat).
     */
    public boolean getByStub(@Nonnull S stub, @Nonnull org.rust.lang.core.crate.Crate crate) {
        return stubCheck.apply(stub);
    }
}
