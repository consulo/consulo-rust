/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa.borrowck;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsNamedElement;

public class MovePlace {
    @Nonnull
    public final RsNamedElement name;

    public MovePlace(@Nonnull RsNamedElement name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MovePlace)) return false;
        return name.equals(((MovePlace) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
