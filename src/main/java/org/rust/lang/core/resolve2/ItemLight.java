/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import com.intellij.util.io.IOUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.resolve.Namespace;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;

/**
 * Base class for lightweight item representations used during mod collection.
 */
public class ItemLight {
    @NotNull
    private final String name;
    @NotNull
    private final VisibilityLight visibility;
    private final boolean isDeeplyEnabledByCfg;
    @NotNull
    private final Set<Namespace> namespaces;

    public ItemLight(
        @NotNull String name,
        @NotNull VisibilityLight visibility,
        boolean isDeeplyEnabledByCfg,
        @NotNull Set<Namespace> namespaces
    ) {
        this.name = name;
        this.visibility = visibility;
        this.isDeeplyEnabledByCfg = isDeeplyEnabledByCfg;
        this.namespaces = namespaces;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public VisibilityLight getVisibility() {
        return visibility;
    }

    public boolean isDeeplyEnabledByCfg() {
        return isDeeplyEnabledByCfg;
    }

    @NotNull
    public Set<Namespace> getNamespaces() {
        return namespaces;
    }

    public void writeTo(@NotNull DataOutput data) throws IOException {
        IOUtil.writeUTF(data, name);
        visibility.writeTo(data);
    }
}
