/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.decl;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

/**
 * Represents the result of matching a macro pattern against a macro call body.
 * Maps meta variable names to their matched values.
 */
public final class MacroSubstitution {
    @NotNull
    private final Map<String, MetaVarValue> myVariables;

    public MacroSubstitution(@NotNull Map<String, MetaVarValue> variables) {
        myVariables = variables;
    }

    @NotNull
    public Map<String, MetaVarValue> getVariables() {
        return myVariables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MacroSubstitution)) return false;
        return Objects.equals(myVariables, ((MacroSubstitution) o).myVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myVariables);
    }

    @Override
    public String toString() {
        return "MacroSubstitution(variables=" + myVariables + ")";
    }
}
