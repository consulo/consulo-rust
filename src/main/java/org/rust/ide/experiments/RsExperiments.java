/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.experiments;

public final class RsExperiments {

    public static final RsExperiments INSTANCE = new RsExperiments();

    @EnabledInStable
    public static final String BUILD_TOOL_WINDOW = "org.rust.cargo.build.tool.window";

    @EnabledInStable
    public static final String EVALUATE_BUILD_SCRIPTS = "org.rust.cargo.evaluate.build.scripts";

    public static final String CARGO_FEATURES_SETTINGS_GUTTER = "org.rust.cargo.features.settings.gutter";

    public static final String PROC_MACROS = "org.rust.macros.proc";

    @EnabledInStable
    public static final String FN_LIKE_PROC_MACROS = "org.rust.macros.proc.function-like";

    @EnabledInStable
    public static final String DERIVE_PROC_MACROS = "org.rust.macros.proc.derive";

    public static final String ATTR_PROC_MACROS = "org.rust.macros.proc.attr";

    @EnabledInStable
    public static final String FETCH_ACTUAL_STDLIB_METADATA = "org.rust.cargo.fetch.actual.stdlib.metadata";

    @EnabledInStable
    public static final String CRATES_LOCAL_INDEX = "org.rust.crates.local.index";

    @EnabledInStable
    public static final String WSL_TOOLCHAIN = "org.rust.wsl";

    public static final String EMULATE_TERMINAL = "org.rust.cargo.emulate.terminal";

    public static final String INTENTIONS_IN_FN_LIKE_MACROS = "org.rust.ide.intentions.macros.function-like";

    public static final String SSR = "org.rust.ssr";

    public static final String SOURCE_BASED_COVERAGE = "org.rust.coverage.source";

    public static final String MIR_MOVE_ANALYSIS = "org.rust.mir.move-analysis";

    public static final String MIR_BORROW_CHECK = "org.rust.mir.borrow-check";

    private RsExperiments() {
    }
}
