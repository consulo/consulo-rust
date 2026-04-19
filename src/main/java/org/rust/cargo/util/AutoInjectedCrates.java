/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import java.util.List;

public final class AutoInjectedCrates {
    public static final String STD = "std";
    public static final String CORE = "core";
    public static final String TEST = "test";

    public static final List<StdLibInfo> stdlibCrates = List.of(
        // Roots
        new StdLibInfo(CORE, StdLibType.ROOT),
        new StdLibInfo(STD, StdLibType.ROOT, List.of("alloc", "panic_unwind", "panic_abort",
            CORE, "libc", "compiler_builtins", "profiler_builtins", "unwind")),
        new StdLibInfo("alloc", StdLibType.ROOT, List.of(CORE, "compiler_builtins")),
        new StdLibInfo("proc_macro", StdLibType.ROOT, List.of(STD)),
        new StdLibInfo(TEST, StdLibType.ROOT, List.of(STD, CORE, "libc", "getopts", "term")),
        // Feature gated
        new StdLibInfo("libc", StdLibType.FEATURE_GATED),
        new StdLibInfo("panic_unwind", StdLibType.FEATURE_GATED, List.of(CORE, "libc", "alloc",
            "unwind", "compiler_builtins")),
        new StdLibInfo("compiler_builtins", StdLibType.FEATURE_GATED, List.of(CORE)),
        new StdLibInfo("profiler_builtins", StdLibType.FEATURE_GATED, List.of(CORE, "compiler_builtins")),
        new StdLibInfo("panic_abort", StdLibType.FEATURE_GATED, List.of(CORE, "libc", "compiler_builtins")),
        new StdLibInfo("unwind", StdLibType.FEATURE_GATED, List.of(CORE, "libc", "compiler_builtins")),
        new StdLibInfo("term", StdLibType.FEATURE_GATED, List.of(STD, CORE)),
        new StdLibInfo("getopts", StdLibType.FEATURE_GATED, List.of(STD, CORE))
    );

    private AutoInjectedCrates() {
    }
}
