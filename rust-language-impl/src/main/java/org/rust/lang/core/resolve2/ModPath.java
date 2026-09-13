/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;

import java.util.Arrays;

/** Path to a module or an item in module */
public class ModPath {
    private final int crate;
    @Nonnull
    private final String[] segments;

    public ModPath(int crate, @Nonnull String[] segments) {
        this.crate = crate;
        this.segments = segments;
    }

    public int getCrate() {
        return crate;
    }

    @Nonnull
    public String[] getSegments() {
        return segments;
    }

    @Nonnull
    public String getName() {
        return segments[segments.length - 1];
    }

    @Nonnull
    public ModPath getParent() {
        return new ModPath(crate, Arrays.copyOfRange(segments, 0, segments.length - 1));
    }

    @Nonnull
    public ModPath append(@Nonnull String segment) {
        String[] newSegments = Arrays.copyOf(segments, segments.length + 1);
        newSegments[segments.length] = segment;
        return new ModPath(crate, newSegments);
    }

    /** {@code mod1::mod2} isSubPathOf {@code mod1::mod2::mod3} */
    public boolean isSubPathOf(@Nonnull ModPath child) {
        if (crate != child.crate) return false;
        if (segments.length > child.segments.length) return false;
        for (int i = 0; i < segments.length; i++) {
            if (!segments[i].equals(child.segments[i])) return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        ModPath modPath = (ModPath) other;
        return crate == modPath.crate && Arrays.equals(segments, modPath.segments);
    }

    @Override
    public int hashCode() {
        return 31 * crate + Arrays.hashCode(segments);
    }

    @Override
    @Nonnull
    public String toString() {
        String joined = String.join("::", segments);
        return joined.isEmpty() ? "crate" : joined;
    }
}
