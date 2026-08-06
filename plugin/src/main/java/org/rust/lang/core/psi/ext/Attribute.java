/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public final class Attribute {
    @Nonnull private final String name;
    @Nullable private final String argText;

    public Attribute(@Nonnull String name, @Nullable String argText) {
        this.name = name;
        this.argText = argText;
    }

    public Attribute(@Nonnull String name) {
        this(name, null);
    }

    @Nonnull
    public String getName() { return name; }

    @Nullable
    public String getArgText() { return argText; }

    @Nonnull
    public String getText() {
        return argText == null ? name : name + "(" + argText + ")";
    }
}
