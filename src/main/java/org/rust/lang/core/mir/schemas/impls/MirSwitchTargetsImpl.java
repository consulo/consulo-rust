/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas.impls;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.MirBasicBlock;
import org.rust.lang.core.mir.schemas.MirSwitchTargets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MirSwitchTargetsImpl<BB extends MirBasicBlock> implements MirSwitchTargets<BB> {
    @NotNull
    private final List<Long> values;
    @NotNull
    private final List<BB> targets;

    public MirSwitchTargetsImpl(@NotNull List<Long> values, @NotNull List<BB> targets) {
        this.values = values;
        this.targets = targets;
    }

    @Override
    @NotNull
    public List<Long> getValues() {
        return values;
    }

    @Override
    @NotNull
    public List<BB> getTargets() {
        return targets;
    }

    @NotNull
    public static <BB extends MirBasicBlock> MirSwitchTargetsImpl<BB> create(
        @NotNull List<Pair<Long, BB>> valuesAndTargets,
        @NotNull BB otherwise
    ) {
        List<Long> values = new ArrayList<>(valuesAndTargets.size());
        List<BB> targets = new ArrayList<>(valuesAndTargets.size() + 1);
        for (Pair<Long, BB> pair : valuesAndTargets) {
            values.add(pair.getFirst());
            targets.add(pair.getSecond());
        }
        targets.add(otherwise);
        return new MirSwitchTargetsImpl<>(values, targets);
    }

    @NotNull
    public static <BB extends MirBasicBlock> MirSwitchTargetsImpl<BB> ifTargets(long value, @NotNull BB thenBlock, @NotNull BB elseBlock) {
        return new MirSwitchTargetsImpl<>(
            Collections.singletonList(value),
            Arrays.asList(thenBlock, elseBlock)
        );
    }
}
