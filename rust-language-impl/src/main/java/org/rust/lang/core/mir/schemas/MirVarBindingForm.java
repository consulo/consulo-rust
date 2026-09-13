/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class MirVarBindingForm {
    @Nonnull
    private final MirBindingMode bindingMode;
    @Nullable
    private final MirSpan tyInfo;
    @Nullable
    private final Pair<MirPlace, MirSpan> matchPlace;
    @Nonnull
    private final MirSpan patternSource;

    public MirVarBindingForm(
        @Nonnull MirBindingMode bindingMode,
        @Nullable MirSpan tyInfo,
        @Nullable Pair<MirPlace, MirSpan> matchPlace,
        @Nonnull MirSpan patternSource
    ) {
        this.bindingMode = bindingMode;
        this.tyInfo = tyInfo;
        this.matchPlace = matchPlace;
        this.patternSource = patternSource;
    }

    @Nonnull
    public MirBindingMode getBindingMode() {
        return bindingMode;
    }

    @Nullable
    public MirSpan getTyInfo() {
        return tyInfo;
    }

    @Nullable
    public Pair<MirPlace, MirSpan> getMatchPlace() {
        return matchPlace;
    }

    @Nonnull
    public MirSpan getPatternSource() {
        return patternSource;
    }
}
