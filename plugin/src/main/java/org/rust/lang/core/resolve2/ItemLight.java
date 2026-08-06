/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import consulo.index.io.data.IOUtil;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.resolve.Namespace;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;

/**
 * Base class for lightweight item representations used during mod collection.
 */
public class ItemLight {
    @Nonnull
    private final String name;
    @Nonnull
    private final VisibilityLight visibility;
    private final boolean isDeeplyEnabledByCfg;
    @Nonnull
    private final Set<Namespace> namespaces;

    public ItemLight(
        @Nonnull String name,
        @Nonnull VisibilityLight visibility,
        boolean isDeeplyEnabledByCfg,
        @Nonnull Set<Namespace> namespaces
    ) {
        this.name = name;
        this.visibility = visibility;
        this.isDeeplyEnabledByCfg = isDeeplyEnabledByCfg;
        this.namespaces = namespaces;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public VisibilityLight getVisibility() {
        return visibility;
    }

    public boolean isDeeplyEnabledByCfg() {
        return isDeeplyEnabledByCfg;
    }

    @Nonnull
    public Set<Namespace> getNamespaces() {
        return namespaces;
    }

    public void writeTo(@Nonnull DataOutput data) throws IOException {
        IOUtil.writeUTF(data, name);
        visibility.writeTo(data);
    }
}
