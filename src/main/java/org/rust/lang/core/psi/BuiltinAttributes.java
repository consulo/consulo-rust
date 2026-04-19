/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

// Bump each time RS_BUILTIN_ATTRIBUTES content is modified
// (that is, builtin-attributes.json content is modified)
public final class BuiltinAttributes {

    public static final int RS_BUILTIN_ATTRIBUTES_VERSION = 1;

    @NotNull
    public static final Map<String, AttributeInfo> RS_BUILTIN_ATTRIBUTES = BuiltinAttributeInfoLoader.loadAttributes();

    // https://github.com/rust-lang/rust/blob/76d18cfb8945f824c8777e04981e930d2037954e/compiler/rustc_resolve/src/macros.rs#L153
    @NotNull
    public static final Set<String> RS_BUILTIN_TOOL_ATTRIBUTES = Set.of("rustfmt", "clippy");

    public sealed interface AttributeInfo permits BuiltinAttributeInfo, BuiltinProcMacroInfo {}

    public record BuiltinAttributeInfo(
        @NotNull String name,
        @NotNull AttributeType type,
        @NotNull AttributeTemplate template,
        @NotNull AttributeDuplicates duplicates,
        boolean gated
    ) implements AttributeInfo {}

    public static final class BuiltinProcMacroInfo implements AttributeInfo {
        public static final BuiltinProcMacroInfo INSTANCE = new BuiltinProcMacroInfo();
        private BuiltinProcMacroInfo() {}
    }

    @SuppressWarnings("unused")
    public enum AttributeType {
        Normal, CrateLevel
    }

    public record AttributeTemplate(
        boolean word,
        @Nullable String list,
        @Nullable String nameValueStr
    ) {}

    @SuppressWarnings("unused")
    public enum AttributeDuplicates {
        DuplicatesOk,
        WarnFollowing,
        WarnFollowingWordOnly,
        ErrorFollowing,
        ErrorPreceding,
        FutureWarnFollowing,
        FutureWarnPreceding
    }

    private static final class BuiltinAttributeInfoLoader {
        private static final String BUILTIN_ATTRIBUTES_PATH = "compiler-info/builtin-attributes.json";
        private static final Logger LOG = Logger.getInstance(BuiltinAttributeInfoLoader.class);

        @NotNull
        static Map<String, AttributeInfo> loadAttributes() {
            List<BuiltinAttributeInfo> attributeList;
            try {
                InputStream stream = BuiltinAttributeInfo.class.getClassLoader()
                    .getResourceAsStream(BUILTIN_ATTRIBUTES_PATH);
                if (stream == null) {
                    LOG.error("Can't find `" + BUILTIN_ATTRIBUTES_PATH + "` file in resources");
                    return Collections.emptyMap();
                }
                try (BufferedInputStream bis = new BufferedInputStream(stream)) {
                    ObjectMapper mapper = new ObjectMapper();
                    attributeList = Arrays.asList(mapper.readValue(bis, BuiltinAttributeInfo[].class));
                }
            } catch (IOException e) {
                LOG.error(e);
                attributeList = Collections.emptyList();
            }

            HashMap<String, AttributeInfo> associateMap = new HashMap<>();
            for (BuiltinAttributeInfo info : attributeList) {
                associateMap.put(info.name(), info);
            }

            // Internal stdlib proc macros
            associateMap.put("simd_test", BuiltinProcMacroInfo.INSTANCE);
            associateMap.put("assert_instr", BuiltinProcMacroInfo.INSTANCE);

            // Proc macros from stdlib
            associateMap.put("derive", BuiltinProcMacroInfo.INSTANCE);
            associateMap.put("test", BuiltinProcMacroInfo.INSTANCE);
            associateMap.put("bench", BuiltinProcMacroInfo.INSTANCE);
            associateMap.put("test_case", BuiltinProcMacroInfo.INSTANCE);
            associateMap.put("global_allocator", BuiltinProcMacroInfo.INSTANCE);
            associateMap.put("cfg_accessible", BuiltinProcMacroInfo.INSTANCE);

            // Don't forget to bump RS_BUILTIN_ATTRIBUTES_VERSION each time associateMap is modified!

            return associateMap;
        }
    }

    private BuiltinAttributes() {}
}
