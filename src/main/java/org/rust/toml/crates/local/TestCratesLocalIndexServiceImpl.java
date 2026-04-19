/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.crates.local;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.rust.stdext.RsResult;

import java.util.*;

public class TestCratesLocalIndexServiceImpl implements CratesLocalIndexService {
    private Map<String, CargoRegistryCrate> myTestCrates = Collections.emptyMap();

    public void setTestCrates(@NotNull Map<String, CargoRegistryCrate> testCrates) {
        myTestCrates = testCrates;
    }

    @NotNull
    public Map<String, CargoRegistryCrate> getTestCrates() {
        return myTestCrates;
    }

    @NotNull
    @Override
    public RsResult<CargoRegistryCrate, Error> getCrate(@NotNull String crateName) {
        return new RsResult.Ok<>(myTestCrates.get(crateName));
    }

    @NotNull
    @Override
    public RsResult<List<String>, Error> getAllCrateNames() {
        return new RsResult.Ok<>(new ArrayList<>(myTestCrates.keySet()));
    }

    @TestOnly
    public static void withMockedCrates(@NotNull Map<String, CargoRegistryCrate> crates, @NotNull Runnable action) {
        TestCratesLocalIndexServiceImpl resolver = (TestCratesLocalIndexServiceImpl) CratesLocalIndexService.getInstance();
        Map<String, CargoRegistryCrate> orgCrates = resolver.getTestCrates();
        try {
            resolver.setTestCrates(crates);
            action.run();
        } finally {
            resolver.setTestCrates(orgCrates);
        }
    }
}
