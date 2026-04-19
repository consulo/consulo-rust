/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class TyWithObligations<T> {
    @NotNull
    private final T myValue;
    @NotNull
    private final List<Obligation> myObligations;

    public TyWithObligations(@NotNull T value) {
        this(value, Collections.emptyList());
    }

    public TyWithObligations(@NotNull T value, @NotNull List<Obligation> obligations) {
        myValue = value;
        myObligations = obligations;
    }

    @NotNull
    public T getValue() {
        return myValue;
    }

    @NotNull
    public List<Obligation> getObligations() {
        return myObligations;
    }

    @NotNull
    public TyWithObligations<T> withObligations(@NotNull List<Obligation> additionalObligations) {
        if (additionalObligations.isEmpty()) return this;
        if (myObligations.isEmpty()) return new TyWithObligations<>(myValue, additionalObligations);
        List<Obligation> combined = new java.util.ArrayList<>(myObligations.size() + additionalObligations.size());
        combined.addAll(myObligations);
        combined.addAll(additionalObligations);
        return new TyWithObligations<>(myValue, combined);
    }
}
