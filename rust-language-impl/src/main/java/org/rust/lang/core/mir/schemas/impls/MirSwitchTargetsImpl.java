/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas.impls;

import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.MirBasicBlock;
import org.rust.lang.core.mir.schemas.MirSwitchTargets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MirSwitchTargetsImpl<BB extends MirBasicBlock> implements MirSwitchTargets<BB> {
    @Nonnull
    private final List<Long> values;
    @Nonnull
    private final List<BB> targets;

    public MirSwitchTargetsImpl(@Nonnull List<Long> values, @Nonnull List<BB> targets) {
        this.values = values;
        this.targets = targets;
    }

    @Override
    @Nonnull
    public List<Long> getValues() {
        return values;
    }

    @Override
    @Nonnull
    public List<BB> getTargets() {
        return targets;
    }

    @Nonnull
    public static <BB extends MirBasicBlock> MirSwitchTargetsImpl<BB> create(
        @Nonnull List<Pair<Long, BB>> valuesAndTargets,
        @Nonnull BB otherwise
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

    @Nonnull
    public static <BB extends MirBasicBlock> MirSwitchTargetsImpl<BB> ifTargets(long value, @Nonnull BB thenBlock, @Nonnull BB elseBlock) {
        return new MirSwitchTargetsImpl<>(
            Collections.singletonList(value),
            Arrays.asList(thenBlock, elseBlock)
        );
    }
}
