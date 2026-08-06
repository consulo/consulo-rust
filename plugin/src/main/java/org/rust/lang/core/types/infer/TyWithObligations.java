/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

public class TyWithObligations<T> {
    @Nonnull
    private final T myValue;
    @Nonnull
    private final List<Obligation> myObligations;

    public TyWithObligations(@Nonnull T value) {
        this(value, Collections.emptyList());
    }

    public TyWithObligations(@Nonnull T value, @Nonnull List<Obligation> obligations) {
        myValue = value;
        myObligations = obligations;
    }

    @Nonnull
    public T getValue() {
        return myValue;
    }

    @Nonnull
    public List<Obligation> getObligations() {
        return myObligations;
    }

    @Nonnull
    public TyWithObligations<T> withObligations(@Nonnull List<Obligation> additionalObligations) {
        if (additionalObligations.isEmpty()) return this;
        if (myObligations.isEmpty()) return new TyWithObligations<>(myValue, additionalObligations);
        List<Obligation> combined = new java.util.ArrayList<>(myObligations.size() + additionalObligations.size());
        combined.addAll(myObligations);
        combined.addAll(additionalObligations);
        return new TyWithObligations<>(myValue, combined);
    }
}
