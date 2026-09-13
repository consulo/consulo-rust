package org.rust.lang.core.macros.proc;

import org.rust.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;

/**
 * Which kinds of procedural macro expansion the user has switched on.
 */
public final class ProcMacroFlags {
    private ProcMacroFlags() {
    }

    public static boolean isAnyEnabled() {
        return OpenApiUtil.isFeatureEnabled(RsExperiments.EVALUATE_BUILD_SCRIPTS)
            && (OpenApiUtil.isFeatureEnabled(RsExperiments.PROC_MACROS)
            || OpenApiUtil.isFeatureEnabled(RsExperiments.FN_LIKE_PROC_MACROS)
            || OpenApiUtil.isFeatureEnabled(RsExperiments.DERIVE_PROC_MACROS)
            || OpenApiUtil.isFeatureEnabled(RsExperiments.ATTR_PROC_MACROS));
    }

    public static boolean isDeriveEnabled() {
        return OpenApiUtil.isFeatureEnabled(RsExperiments.EVALUATE_BUILD_SCRIPTS)
            && (OpenApiUtil.isFeatureEnabled(RsExperiments.PROC_MACROS)
            || OpenApiUtil.isFeatureEnabled(RsExperiments.DERIVE_PROC_MACROS));
    }

    public static boolean isAttrEnabled() {
        return OpenApiUtil.isFeatureEnabled(RsExperiments.EVALUATE_BUILD_SCRIPTS)
            && (OpenApiUtil.isFeatureEnabled(RsExperiments.PROC_MACROS)
            || OpenApiUtil.isFeatureEnabled(RsExperiments.ATTR_PROC_MACROS));
    }

    public static boolean isFunctionLikeEnabled() {
        return OpenApiUtil.isFeatureEnabled(RsExperiments.EVALUATE_BUILD_SCRIPTS)
            && (OpenApiUtil.isFeatureEnabled(RsExperiments.PROC_MACROS)
            || OpenApiUtil.isFeatureEnabled(RsExperiments.FN_LIKE_PROC_MACROS));
    }
}
