/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import org.jetbrains.annotations.NotNull;
import org.toml.lang.psi.TomlElement;
import org.toml.lang.psi.TomlValue;

import java.util.Map;
import java.util.Set;

public class DependencyCrate {
    private static final Set<String> FOREIGN_PROPERTIES = Set.of("git", "path", "registry");

    private final String myCrateName;
    private final TomlElement myCrateNameElement;
    private final Map<String, TomlValue> myProperties;

    public DependencyCrate(@NotNull String crateName, @NotNull TomlElement crateNameElement, @NotNull Map<String, TomlValue> properties) {
        myCrateName = crateName;
        myCrateNameElement = crateNameElement;
        myProperties = properties;
    }

    @NotNull
    public String getCrateName() {
        return myCrateName;
    }

    @NotNull
    public TomlElement getCrateNameElement() {
        return myCrateNameElement;
    }

    @NotNull
    public Map<String, TomlValue> getProperties() {
        return myProperties;
    }

    public boolean isForeign() {
        for (String key : myProperties.keySet()) {
            if (FOREIGN_PROPERTIES.contains(key)) return true;
        }
        return false;
    }
}
