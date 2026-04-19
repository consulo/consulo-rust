/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.Nullable;

/**
 * Rust lint warning levels.
 */
public enum RsLintLevel {
    /**
     * Warnings are suppressed.
     */
    ALLOW("allow"),

    /**
     * Warnings.
     */
    WARN("warn"),

    /**
     * Compiler errors.
     */
    DENY("deny"),

    /**
     * Compiler errors, but also forbids changing the lint level afterwards.
     */
    FORBID("forbid");

    private final String myId;

    RsLintLevel(String id) {
        myId = id;
    }

    public String getId() {
        return myId;
    }

    @Nullable
    public static RsLintLevel valueForId(String id) {
        for (RsLintLevel level : values()) {
            if (level.myId.equals(id)) {
                return level;
            }
        }
        return null;
    }
}
