/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

public enum ItemProcessingMode {
    WITHOUT_PRIVATE_IMPORTS(false),
    WITH_PRIVATE_IMPORTS(false),
    WITH_PRIVATE_IMPORTS_N_EXTERN_CRATES(true);

    private final boolean withExternCrates;

    ItemProcessingMode(boolean withExternCrates) {
        this.withExternCrates = withExternCrates;
    }

    public boolean isWithExternCrates() {
        return withExternCrates;
    }
}
