/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

import org.jetbrains.annotations.PropertyKey;
import org.rust.RsBundle;

public enum ExternalLinter {
    CARGO_CHECK("rust.external.linter.cargo.check.item"),
    CLIPPY("rust.external.linter.clippy.item");

    private static final String BUNDLE = org.rust.RsBundle.BUNDLE;

    private final String titleKey;

    ExternalLinter(@PropertyKey(resourceBundle = "messages.RsBundle") String titleKey) {
        this.titleKey = titleKey;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public String getTitle() {
        return RsBundle.message(titleKey);
    }

    @Override
    public String toString() {
        return getTitle();
    }

    public static final ExternalLinter DEFAULT = CARGO_CHECK;
}
