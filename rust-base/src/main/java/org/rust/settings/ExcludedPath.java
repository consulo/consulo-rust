/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.settings;

import jakarta.annotation.Nonnull;

/**
 * Must have default constructor and mutable fields for deserialization.
 */
public class ExcludedPath {

    @Nonnull
    public String path;

    @Nonnull
    public ExclusionType type;

    public ExcludedPath() {
        this("", ExclusionType.ItemsAndMethods);
    }

    public ExcludedPath(@Nonnull String path) {
        this(path, ExclusionType.ItemsAndMethods);
    }

    public ExcludedPath(@Nonnull String path, @Nonnull ExclusionType type) {
        this.path = path;
        this.type = type;
    }

    @Nonnull
    public String getPath() {
        return path;
    }

    public void setPath(@Nonnull String path) {
        this.path = path;
    }

    @Nonnull
    public ExclusionType getType() {
        return type;
    }

    public void setType(@Nonnull ExclusionType type) {
        this.type = type;
    }
}
