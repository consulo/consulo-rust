/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.imports;

import jakarta.annotation.Nonnull;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.cargo.api.util.AutoInjectedCrates;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

import java.util.Comparator;

public class ImportCandidate implements Comparable<ImportCandidate> {
    @Nonnull
    private final RsQualifiedNamedElement item;
    /**
     * First segment is crate name (can be "crate").
     * Last segment is item name.
     */
    @Nonnull
    private final String[] path;
    /**
     * Corresponds to path[0].
     * May differ from item.containingCrate.
     */
    @Nonnull
    private final Crate crate;
    @Nonnull
    private final ImportInfo info;
    private final boolean isRootPathResolved;

    public ImportCandidate(
        @Nonnull RsQualifiedNamedElement item,
        @Nonnull String[] path,
        @Nonnull Crate crate,
        @Nonnull ImportInfo info,
        boolean isRootPathResolved
    ) {
        this.item = item;
        this.path = path;
        this.crate = crate;
        this.info = info;
        this.isRootPathResolved = isRootPathResolved;
    }

    @Nonnull
    public RsQualifiedNamedElement getItem() {
        return item;
    }

    @Nonnull
    public String[] getPath() {
        return path;
    }

    @Nonnull
    public Crate getCrate() {
        return crate;
    }

    @Nonnull
    public ImportInfo getInfo() {
        return info;
    }

    @Nonnull
    public String getItemName() {
        return path[path.length - 1];
    }

    @Override
    public int compareTo(@Nonnull ImportCandidate other) {
        return COMPARATOR.compare(this, other);
    }

    private static final Comparator<ImportCandidate> COMPARATOR =
        Comparator.<ImportCandidate, Boolean>comparing(c -> !c.isRootPathResolved)
            .thenComparingInt(c -> originOrder(c.crate))
            .thenComparing(c -> c.info.getUsePath());

    private static int originOrder(@Nonnull Crate crate) {
        PackageOrigin origin = crate.getOrigin();
        if (origin == PackageOrigin.WORKSPACE) return 0;
        if (origin == PackageOrigin.STDLIB) {
            String normName = crate.getNormName();
            if (AutoInjectedCrates.STD.equals(normName)) return 1;
            if (AutoInjectedCrates.CORE.equals(normName)) return 2;
            return 3;
        }
        if (origin == PackageOrigin.DEPENDENCY) return 4;
        if (origin == PackageOrigin.STDLIB_DEPENDENCY) return 5;
        return 6;
    }
}
