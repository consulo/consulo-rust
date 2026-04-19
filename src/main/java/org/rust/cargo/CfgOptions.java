/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo;

import org.jetbrains.annotations.TestOnly;

import java.util.*;

public record CfgOptions(
    Map<String, Set<String>> keyValueOptions,
    Set<String> nameOptions
) {
    public boolean isNameEnabled(String name) {
        return nameOptions.contains(name);
    }

    public boolean isNameValueEnabled(String name, String value) {
        Set<String> values = keyValueOptions.get(name);
        return values != null && values.contains(value);
    }

    public CfgOptions plus(CfgOptions other) {
        Map<String, Set<String>> mergedKeyValue = new HashMap<>(keyValueOptions);
        mergedKeyValue.putAll(other.keyValueOptions);
        Set<String> mergedNames = new HashSet<>(nameOptions);
        mergedNames.addAll(other.nameOptions);
        return new CfgOptions(mergedKeyValue, mergedNames);
    }

    public static CfgOptions parse(List<String> rawCfgOptions) {
        HashMap<String, Set<String>> knownKeyValueOptions = new HashMap<>();
        HashSet<String> knownNameOptions = new HashSet<>();

        for (String option : rawCfgOptions) {
            String[] parts = option.split("=", 2);
            String key = parts.length > 0 ? parts[0] : null;
            String value = parts.length > 1 ? parts[1].replaceAll("^\"|\"$", "") : null;

            if (key != null && value != null) {
                knownKeyValueOptions.computeIfAbsent(key, k -> new HashSet<>()).add(value);
            } else if (key != null) {
                knownNameOptions.add(key);
            }
        }

        return new CfgOptions(knownKeyValueOptions, knownNameOptions);
    }

    public static final CfgOptions EMPTY = new CfgOptions(Collections.emptyMap(), Collections.emptySet());

    @TestOnly
    public static final CfgOptions DEFAULT = new CfgOptions(
        new HashMap<>(Map.ofEntries(
            Map.entry("target_has_atomic", new HashSet<>(Set.of("8", "16", "32", "64", "ptr", "cas"))),
            Map.entry("target_arch", new HashSet<>(Set.of("x86_64"))),
            Map.entry("target_endian", new HashSet<>(Set.of("little"))),
            Map.entry("target_env", new HashSet<>(Set.of("gnu"))),
            Map.entry("target_family", new HashSet<>(Set.of("unix"))),
            Map.entry("target_os", new HashSet<>(Set.of("linux"))),
            Map.entry("target_pointer_width", new HashSet<>(Set.of("64"))),
            Map.entry("feature", new HashSet<>(Set.of("use_std")))
        )),
        new HashSet<>(Set.of("debug_assertions", "unix"))
    );
}
