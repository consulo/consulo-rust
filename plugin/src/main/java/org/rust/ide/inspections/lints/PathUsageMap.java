/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.Map;
import java.util.Set;

public interface PathUsageMap {
    @Nonnull
    Map<String, Set<RsElement>> getPathUsages();

    @Nonnull
    Set<String> getUnresolvedPaths();

    @Nonnull
    Set<RsTraitItem> getTraitUsages();

    @Nonnull
    Set<String> getUnresolvedMethods();
}
