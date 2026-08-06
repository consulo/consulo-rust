/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import jakarta.annotation.Nonnull;

import java.util.LinkedHashSet;

public class SuggestedNames {
    @Nonnull
    private final String myDefault;
    @Nonnull
    private final LinkedHashSet<String> myAll;

    public SuggestedNames(@Nonnull String defaultName, @Nonnull LinkedHashSet<String> all) {
        myDefault = defaultName;
        myAll = all;
    }

    @Nonnull
    public String getDefault() {
        return myDefault;
    }

    @Nonnull
    public LinkedHashSet<String> getAll() {
        return myAll;
    }
}
