/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MirVarBindingForm {
    @NotNull
    private final MirBindingMode bindingMode;
    @Nullable
    private final MirSpan tyInfo;
    @Nullable
    private final Pair<MirPlace, MirSpan> matchPlace;
    @NotNull
    private final MirSpan patternSource;

    public MirVarBindingForm(
        @NotNull MirBindingMode bindingMode,
        @Nullable MirSpan tyInfo,
        @Nullable Pair<MirPlace, MirSpan> matchPlace,
        @NotNull MirSpan patternSource
    ) {
        this.bindingMode = bindingMode;
        this.tyInfo = tyInfo;
        this.matchPlace = matchPlace;
        this.patternSource = patternSource;
    }

    @NotNull
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

    @NotNull
    public MirSpan getPatternSource() {
        return patternSource;
    }
}
