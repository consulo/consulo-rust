/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import jakarta.annotation.Nonnull;

import java.util.Objects;

public final class MirSourceInfo {
    @Nonnull
    private final MirSpan span;
    @Nonnull
    private final MirSourceScope scope;

    public MirSourceInfo(@Nonnull MirSpan span, @Nonnull MirSourceScope scope) {
        this.span = span;
        this.scope = scope;
    }

    @Nonnull
    public MirSpan getSpan() {
        return span;
    }

    @Nonnull
    public MirSourceScope getScope() {
        return scope;
    }

    public static final MirSourceInfo fake = new MirSourceInfo(MirSpan.Fake.INSTANCE, MirSourceScope.fake);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MirSourceInfo that = (MirSourceInfo) o;
        return Objects.equals(span, that.span) && Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(span, scope);
    }

    @Override
    public String toString() {
        return "MirSourceInfo(span=" + span + ", scope=" + scope + ")";
    }
}
