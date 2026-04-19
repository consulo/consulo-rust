/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;

public class SuggestedNames {
    @NotNull
    private final String myDefault;
    @NotNull
    private final LinkedHashSet<String> myAll;

    public SuggestedNames(@NotNull String defaultName, @NotNull LinkedHashSet<String> all) {
        myDefault = defaultName;
        myAll = all;
    }

    @NotNull
    public String getDefault() {
        return myDefault;
    }

    @NotNull
    public LinkedHashSet<String> getAll() {
        return myAll;
    }
}
