/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import org.jetbrains.annotations.NotNull;
import org.rust.openapiext.OpenApiUtil;

import java.util.List;
import java.util.Map;

/**
 * Specifies pre-defined (hardcoded) behavior of some known procedural macros.
 *
 * Maps {@code package name} to a mapping {@code macro name} => {@link KnownProcMacroKind}
 */
public final class HardcodedProcMacroProperties {

    private static final Map<String, Map<String, KnownProcMacroKind>> RS_HARDCODED_PROC_MACRO_ATTRIBUTES = Map.ofEntries(
        Map.entry("tokio_macros", Map.of(
            "main", KnownProcMacroKind.ASYNC_MAIN,
            "main_rt", KnownProcMacroKind.ASYNC_MAIN,
            "test", KnownProcMacroKind.ASYNC_TEST,
            "test_rt", KnownProcMacroKind.ASYNC_TEST
        )),
        Map.entry("async_attributes", Map.of(
            "main", KnownProcMacroKind.ASYNC_MAIN,
            "test", KnownProcMacroKind.ASYNC_TEST,
            "bench", KnownProcMacroKind.ASYNC_BENCH
        )),
        Map.entry("tracing_attributes", Map.of("instrument", KnownProcMacroKind.IDENTITY)),
        Map.entry("proc_macro_error_attr", Map.of("proc_macro_error", KnownProcMacroKind.IDENTITY)),
        Map.entry("actix_web_codegen", Map.of("main", KnownProcMacroKind.ASYNC_MAIN)),
        Map.entry("actix_derive", Map.of(
            "main", KnownProcMacroKind.ASYNC_MAIN,
            "test", KnownProcMacroKind.ASYNC_TEST
        )),
        Map.entry("serial_test_derive", Map.of("serial", KnownProcMacroKind.TEST_WRAPPER)),
        Map.entry("cortex_m_rt_macros", Map.of("entry", KnownProcMacroKind.CUSTOM_MAIN)),
        Map.entry("test_case", Map.of("test_case", KnownProcMacroKind.CUSTOM_TEST)),
        Map.entry("ndk_macro", Map.of("main", KnownProcMacroKind.CUSTOM_MAIN)),
        Map.entry("quickcheck_macros", Map.of("quickcheck", KnownProcMacroKind.CUSTOM_TEST)),
        Map.entry("async_recursion", Map.of("async_recursion", KnownProcMacroKind.IDENTITY)),
        Map.entry("paw_attributes", Map.of("main", KnownProcMacroKind.CUSTOM_MAIN)),
        Map.entry("interpolate_name", Map.of("interpolate_test", KnownProcMacroKind.CUSTOM_TEST_RENAME)),
        Map.entry("ntest_test_cases", Map.of("test_case", KnownProcMacroKind.CUSTOM_TEST_RENAME)),
        Map.entry("spandoc_attribute", Map.of("spandoc", KnownProcMacroKind.IDENTITY)),
        Map.entry("log_derive", Map.of(
            "logfn", KnownProcMacroKind.IDENTITY,
            "logfn_inputs", KnownProcMacroKind.IDENTITY
        )),
        Map.entry("wasm_bindgen_test_macro", Map.of("wasm_bindgen_test", KnownProcMacroKind.CUSTOM_TEST)),
        Map.entry("test_env_log", Map.of("test", KnownProcMacroKind.CUSTOM_TEST)),
        Map.entry("parameterized_macro", Map.of("parameterized", KnownProcMacroKind.CUSTOM_TEST_RENAME)),
        Map.entry("alloc_counter_macro", Map.of(
            "no_alloc", KnownProcMacroKind.IDENTITY,
            "count_alloc", KnownProcMacroKind.IDENTITY
        )),
        Map.entry("uefi_macros", Map.of("entry", KnownProcMacroKind.CUSTOM_MAIN)),
        Map.entry("async_trait", Map.of("async_trait", KnownProcMacroKind.ASYNC_TRAIT)),
        Map.entry("sqlx_macros", Map.of("test", KnownProcMacroKind.ASYNC_TEST))
    );

    @NotNull
    public static KnownProcMacroKind getHardcodeProcMacroProperties(@NotNull String packageName, @NotNull String macroName) {
        Map<String, KnownProcMacroKind> packageMap = RS_HARDCODED_PROC_MACRO_ATTRIBUTES.get(packageName);
        if (packageMap != null) {
            KnownProcMacroKind kind = packageMap.get(macroName);
            if (kind != null) {
                return kind;
            }
        }

        if (OpenApiUtil.isUnitTestMode()
            && "test_proc_macros".equals(packageName)
            && List.of("attr_hardcoded_not_a_macro", "attr_hardcoded_as_is").contains(macroName)) {
            return KnownProcMacroKind.IDENTITY;
        }

        return KnownProcMacroKind.DEFAULT_PURE;
    }

    private HardcodedProcMacroProperties() {}
}
