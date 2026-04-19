/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public interface MirSwitchTargets<BB extends MirBasicBlock> extends Iterable<Pair<Long, BB>> {
    @NotNull
    List<Long> getValues();

    @NotNull
    List<BB> getTargets();

    @NotNull
    default BB getOtherwise() {
        List<BB> targets = getTargets();
        return targets.get(targets.size() - 1);
    }

    @NotNull
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
