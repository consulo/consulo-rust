/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class MirSourceInfo {
    @NotNull
    private final MirSpan span;
    @NotNull
    private final MirSourceScope scope;

    public MirSourceInfo(@NotNull MirSpan span, @NotNull MirSourceScope scope) {
        this.span = span;
        this.scope = scope;
    }

    @NotNull
    public MirSpan getSpan() {
        return span;
    }

    @NotNull
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
