/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * https://doc.rust-lang.org/cargo/reference/config.html
 */
public record CargoConfig(
    List<String> buildTargets,
    Map<String, EnvValue> env
) {
    public static final CargoConfig DEFAULT = new CargoConfig(Collections.emptyList(), Collections.emptyMap());

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnvValue(
        @JsonProperty("value") String value,
        @JsonProperty("force") boolean isForced,
        @JsonProperty("relative") boolean isRelative
    ) {
        public EnvValue(String value) {
            this(value, false, false);
        }
    }
}
