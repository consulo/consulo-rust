/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.mir.WithIndex;

import java.util.Objects;

public final class MirSourceScope implements WithIndex {
    private final int index;
    @Nonnull
    private final MirSpan span;
    @Nullable
    private final MirSourceScope parentScope;

    public MirSourceScope(int index, @Nonnull MirSpan span, @Nullable MirSourceScope parentScope) {
        this.index = index;
        this.span = span;
        this.parentScope = parentScope;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Nonnull
    public MirSpan getSpan() {
        return span;
    }

    @Nullable
    public MirSourceScope getParentScope() {
        return parentScope;
    }

    public static final MirSourceScope fake = new MirSourceScope(-1, MirSpan.Fake.INSTANCE, null);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MirSourceScope that = (MirSourceScope) o;
        return index == that.index && Objects.equals(span, that.span) && Objects.equals(parentScope, that.parentScope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, span, parentScope);
    }

    @Override
    public String toString() {
        return "MirSourceScope(index=" + index + ", span=" + span + ", parentScope=" + parentScope + ")";
    }
}
