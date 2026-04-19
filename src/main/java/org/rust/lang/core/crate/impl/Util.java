/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.crate.impl;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.crate.Crate;
import org.rust.openapiext.Testmark;

import java.util.LinkedHashSet;

public final class Util {
    private Util() {
    }

    @NotNull
    public static LinkedHashSet<Crate> flattenTopSortedDeps(@NotNull Iterable<Crate.Dependency> dependencies) {
        LinkedHashSet<Crate> flatDeps = new LinkedHashSet<>();

        for (Crate.Dependency dep : dependencies) {
            for (Crate flatDep : dep.getCrate().getFlatDependencies()) {
                flatDeps.add(flatDep);
            }
            flatDeps.add(dep.getCrate());
        }

        return flatDeps;
    }

    public static final class CrateGraphTestmarks {
        public static final CyclicDevDependency INSTANCE = new CyclicDevDependency();

        private CrateGraphTestmarks() {
        }

        public static final class CyclicDevDependency extends Testmark {
        }
    }
}
