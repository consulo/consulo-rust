/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public interface MirSwitchTargets<BB extends MirBasicBlock> extends Iterable<Pair<Long, BB>> {
    @Nonnull
    List<Long> getValues();

    @Nonnull
    List<BB> getTargets();

    @Nonnull
    default BB getOtherwise() {
        List<BB> targets = getTargets();
        return targets.get(targets.size() - 1);
    }

    @Nonnull
    @Override
    default Iterator<Pair<Long, BB>> iterator() {
        return new Iterator<Pair<Long, BB>>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < getValues().size();
            }

            @Override
            public Pair<Long, BB> next() {
                if (!hasNext()) throw new NoSuchElementException();
                Pair<Long, BB> pair = new Pair<>(getValues().get(index), getTargets().get(index));
                index++;
                return pair;
            }
        };
    }
}
