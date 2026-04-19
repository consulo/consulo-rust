/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.settings;

import org.jetbrains.annotations.NotNull;

/**
 * Must have default constructor and mutable fields for deserialization.
 */
public class ExcludedPath {

    @NotNull
    public String path;

    @NotNull
    public ExclusionType type;

    public ExcludedPath() {
        this("", ExclusionType.ItemsAndMethods);
    }

    public ExcludedPath(@NotNull String path) {
        this(path, ExclusionType.ItemsAndMethods);
    }

    public ExcludedPath(@NotNull String path, @NotNull ExclusionType type) {
        this.path = path;
        this.type = type;
    }

    @NotNull
    public String getPath() {
        return path;
    }

    public void setPath(@NotNull String path) {
        this.path = path;
    }

    @NotNull
    public ExclusionType getType() {
        return type;
    }

    public void setType(@NotNull ExclusionType type) {
        this.type = type;
    }
}
