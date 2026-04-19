/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.Map;
import java.util.Set;

public interface PathUsageMap {
    @NotNull
    Map<String, Set<RsElement>> getPathUsages();

    @NotNull
    Set<String> getUnresolvedPaths();

    @NotNull
    Set<RsTraitItem> getTraitUsages();

    @NotNull
    Set<String> getUnresolvedMethods();
}
