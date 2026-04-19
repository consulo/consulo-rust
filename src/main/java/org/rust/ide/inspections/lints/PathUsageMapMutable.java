/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.*;

public class PathUsageMapMutable implements PathUsageMap {
    private final Map<String, Set<RsElement>> myPathUsages = new HashMap<>();
    private final Set<String> myUnresolvedPaths = new HashSet<>();
    private final Set<RsTraitItem> myTraitUsages = new HashSet<>();
    private final Set<String> myUnresolvedMethods = new HashSet<>();

    @NotNull
    @Override
    public Map<String, Set<RsElement>> getPathUsages() {
        return myPathUsages;
    }

    @NotNull
    @Override
    public Set<String> getUnresolvedPaths() {
        return myUnresolvedPaths;
    }

    @NotNull
    @Override
    public Set<RsTraitItem> getTraitUsages() {
        return myTraitUsages;
    }

    @NotNull
    @Override
    public Set<String> getUnresolvedMethods() {
        return myUnresolvedMethods;
    }

    public void recordPath(@NotNull String name, @NotNull List<RsElement> items) {
        if (items.isEmpty()) {
            myUnresolvedPaths.add(name);
        } else {
            myPathUsages.computeIfAbsent(name, k -> new HashSet<>()).addAll(items);
        }
    }

    public void recordMethod(@NotNull String methodName, @NotNull Set<RsTraitItem> traits) {
        if (traits.isEmpty()) {
            myUnresolvedMethods.add(methodName);
        } else {
            myTraitUsages.addAll(traits);
        }
    }
}
