/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.stubs.RsAttributeOwnerStub;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A helper to check attribute existence on a PSI element backed by a stub.
 * When a stub is available, uses the quick stub-based check.
 * Otherwise, falls through to checking attributes on the PSI.
 *
 * @param <T> the PSI element type
 * @param <S> the stub type
 */
public final class StubbedAttributeProperty<T extends RsDocAndAttributeOwner, S extends RsAttributeOwnerStub> {

    @NotNull
    private final Predicate<QueryAttributes<?>> psiCheck;
    @NotNull
    private final Function<S, Boolean> stubCheck;

    public StubbedAttributeProperty(@NotNull Predicate<QueryAttributes<?>> psiCheck,
                                    @NotNull Function<S, Boolean> stubCheck) {
        this.psiCheck = psiCheck;
        this.stubCheck = stubCheck;
    }

    @SuppressWarnings("unchecked")
    public boolean getByPsi(@NotNull T psi) {
        if (psi instanceof StubBasedPsiElementBase<?>) {
            Object stub = ((StubBasedPsiElementBase<?>) psi).getStub();
            if (stub instanceof RsAttributeOwnerStub) {
                return stubCheck.apply((S) stub);
            }
        }
        return getByQueryAttributes(RsDocAndAttributeOwnerUtil.getQueryAttributes(psi));
    }

    public boolean getByQueryAttributes(@NotNull QueryAttributes<?> queryAttributes) {
        return psiCheck.test(queryAttributes);
    }

    /**
     * Check using the stub directly (without a crate parameter).
     */
    public boolean getByStub(@NotNull S stub) {
        return stubCheck.apply(stub);
    }

    /**
     * Check using the stub directly with a crate parameter (crate is ignored, kept for API compat).
     */
    public boolean getByStub(@NotNull S stub, @NotNull org.rust.lang.core.crate.Crate crate) {
        return stubCheck.apply(stub);
    }
}
