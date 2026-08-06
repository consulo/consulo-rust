/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.structure;

import consulo.fileEditor.structureView.tree.ActionPresentation;
import consulo.fileEditor.structureView.tree.ActionPresentationData;
import consulo.fileEditor.structureView.tree.Sorter;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.psi.ext.RsVisibility;
import org.rust.lang.core.psi.ext.RsVisible;

import java.util.Comparator;

public class RsVisibilitySorter implements Sorter {

    public static final String ID = "STRUCTURE_VIEW_VISIBILITY_SORTER";

    private enum Order {
        Public,
        Restricted,
        Private,
        Unknown;

        static Order fromVisibility(@Nonnull RsVisibility vis) {
            if (vis instanceof RsVisibility.Public) return Public;
            if (vis instanceof RsVisibility.Private) return Private;
            if (vis instanceof RsVisibility.Restricted) return Restricted;
            return Unknown;
        }
    }

    private static Order getOrdering(Object x) {
        if (!(x instanceof RsStructureViewElement)) return Order.Unknown;
        var psi = ((RsStructureViewElement) x).getValue();
        if (!(psi instanceof RsVisible)) return Order.Unknown;
        RsVisibility visibility = ((RsVisible) psi).getVisibility();
        return visibility != null ? Order.fromVisibility(visibility) : Order.Unknown;
    }

    @Override
    @Nonnull
    public ActionPresentation getPresentation() {
        return new ActionPresentationData(
            RsBundle.message("structure.view.sort.visibility"),
            null,
            RsIcons.VISIBILITY_SORT
        );
    }

    @Override
    @Nonnull
    public String getName() {
        return ID;
    }

    @Override
    @Nonnull
    public Comparator<?> getComparator() {
        return (Comparator<Object>) (p0, p1) -> {
            Order ord0 = getOrdering(p0);
            Order ord1 = getOrdering(p1);
            return ord0.compareTo(ord1);
        };
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}
