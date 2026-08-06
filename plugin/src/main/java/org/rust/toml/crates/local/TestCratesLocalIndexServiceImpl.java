/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.crates.local;

import jakarta.annotation.Nonnull;

import org.rust.stdext.RsResult;

import java.util.*;

public class TestCratesLocalIndexServiceImpl implements CratesLocalIndexService {
    private Map<String, CargoRegistryCrate> myTestCrates = Collections.emptyMap();

    public void setTestCrates(@Nonnull Map<String, CargoRegistryCrate> testCrates) {
        myTestCrates = testCrates;
    }

    @Nonnull
    public Map<String, CargoRegistryCrate> getTestCrates() {
        return myTestCrates;
    }

    @Nonnull
    @Override
    public RsResult<CargoRegistryCrate, Error> getCrate(@Nonnull String crateName) {
        return new RsResult.Ok<>(myTestCrates.get(crateName));
    }

    @Nonnull
    @Override
    public RsResult<List<String>, Error> getAllCrateNames() {
        return new RsResult.Ok<>(new ArrayList<>(myTestCrates.keySet()));
    }

    
    public static void withMockedCrates(@Nonnull Map<String, CargoRegistryCrate> crates, @Nonnull Runnable action) {
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
