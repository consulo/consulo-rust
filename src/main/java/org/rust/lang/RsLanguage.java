/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang;

import com.intellij.lang.Language;

public final class RsLanguage extends Language {
    public static final RsLanguage INSTANCE = new RsLanguage();

    private RsLanguage() {
        super("Rust", "text/rust", "text/x-rust", "application/x-rust");
    }

    @Override
    public boolean isCaseSensitive() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "Rust";
    }
}
