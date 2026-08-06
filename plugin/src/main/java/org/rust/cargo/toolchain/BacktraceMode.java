/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

public enum BacktraceMode {
    NO(0, "No"),
    SHORT(1, "Short"),
    FULL(2, "Full");

    private final int index;
    private final String title;

    BacktraceMode(int index, String title) {
        this.index = index;
        this.title = title;
    }

    public int getIndex() {
        return index;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }

    public static final BacktraceMode DEFAULT = SHORT;

    public static BacktraceMode fromIndex(int index) {
        for (BacktraceMode mode : values()) {
            if (mode.index == index) {
                return mode;
            }
        }
        return DEFAULT;
    }
}
