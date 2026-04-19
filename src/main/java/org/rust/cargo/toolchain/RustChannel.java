/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

import org.jetbrains.annotations.Nullable;

public enum RustChannel {
    DEFAULT(0, null),
    STABLE(1, "stable"),
    BETA(2, "beta"),
    NIGHTLY(3, "nightly"),
    DEV(4, "dev");

    private final int index;
    @Nullable
    private final String channel;

    RustChannel(int index, @Nullable String channel) {
        this.index = index;
        this.channel = channel;
    }

    public int getIndex() {
        return index;
    }

    @Nullable
    public String getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return channel != null ? channel : "[default]";
    }

    public static RustChannel fromIndex(int index) {
        for (RustChannel ch : values()) {
            if (ch.index == index) {
                return ch;
            }
        }
        return DEFAULT;
    }

    public static RustChannel fromPreRelease(String releaseSuffix) {
        if (releaseSuffix.isEmpty()) return STABLE;
        if (releaseSuffix.startsWith("beta")) return BETA;
        if (releaseSuffix.startsWith("nightly")) return NIGHTLY;
        if (releaseSuffix.startsWith("dev")) return DEV;
        return DEFAULT;
    }
}
