/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.experiments;

import org.rust.openapiext.OpenApiUtil;

/**
 * Whether proc-macro support is switched on. Read from the project model as well as from the
 * macro engine, so it lives with the experiment flags rather than with either caller.
 */
public final class ProcMacroExperiments {
    private ProcMacroExperiments() {
    }

    /** Any proc-macro kind enabled at all - build scripts must be evaluated for any of them to work. */
    public static boolean isAnyEnabled() {
        return OpenApiUtil.isFeatureEnabled(RsExperiments.EVALUATE_BUILD_SCRIPTS)
            && (OpenApiUtil.isFeatureEnabled(RsExperiments.PROC_MACROS)
            || OpenApiUtil.isFeatureEnabled(RsExperiments.FN_LIKE_PROC_MACROS)
            || OpenApiUtil.isFeatureEnabled(RsExperiments.DERIVE_PROC_MACROS)
            || OpenApiUtil.isFeatureEnabled(RsExperiments.ATTR_PROC_MACROS));
    }
}
