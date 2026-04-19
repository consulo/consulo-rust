/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.imports;

import org.jetbrains.annotations.NotNull;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.util.AutoInjectedCrates;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

import java.util.Comparator;

public class ImportCandidate implements Comparable<ImportCandidate> {
    @NotNull
    private final RsQualifiedNamedElement item;
    /**
     * First segment is crate name (can be "crate").
     * Last segment is item name.
     */
    @NotNull
    private final String[] path;
    /**
     * Corresponds to path[0].
     * May differ from item.containingCrate.
     */
    @NotNull
    private final Crate crate;
    @NotNull
    private final ImportInfo info;
    private final boolean isRootPathResolved;

    public ImportCandidate(
        @NotNull RsQualifiedNamedElement item,
        @NotNull String[] path,
        @NotNull Crate crate,
        @NotNull ImportInfo info,
        boolean isRootPathResolved
    ) {
        this.item = item;
        this.path = path;
        this.crate = crate;
        this.info = info;
        this.isRootPathResolved = isRootPathResolved;
    }

    @NotNull
    public RsQualifiedNamedElement getItem() {
        return item;
    }

    @NotNull
    public String[] getPath() {
        return path;
    }

    @NotNull
    public Crate getCrate() {
        return crate;
    }

    @NotNull
    public ImportInfo getInfo() {
        return info;
    }

    @NotNull
    public String getItemName() {
        return path[path.length - 1];
    }

    @Override
    public int compareTo(@NotNull ImportCandidate other) {
        return COMPARATOR.compare(this, other);
    }

    private static final Comparator<ImportCandidate> COMPARATOR =
        Comparator.<ImportCandidate, Boolean>comparing(c -> !c.isRootPathResolved)
            .thenComparingInt(c -> originOrder(c.crate))
            .thenComparing(c -> c.info.getUsePath());

    private static int originOrder(@NotNull Crate crate) {
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
