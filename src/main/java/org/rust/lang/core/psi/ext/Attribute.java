/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Attribute {
    @NotNull private final String name;
    @Nullable private final String argText;

    public Attribute(@NotNull String name, @Nullable String argText) {
        this.name = name;
        this.argText = argText;
    }

    public Attribute(@NotNull String name) {
        this(name, null);
    }

    @NotNull
    public String getName() { return name; }

    @Nullable
    public String getArgText() { return argText; }

    @NotNull
    public String getText() {
        return argText == null ? name : name + "(" + argText + ")";
    }
}
