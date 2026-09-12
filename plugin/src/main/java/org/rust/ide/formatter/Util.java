/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter;

import consulo.language.codeStyle.CodeStyleSettings;
import jakarta.annotation.Nonnull;
import org.rust.ide.formatter.settings.RsCodeStyleSettings;
import org.rust.openapiext.Testmark;

/**
 * Contains formatting utility methods and testmarks.
 */
public final class Util {

    private Util() {
    }

    /**
     * Extension property equivalent: retrieves the {@link RsCodeStyleSettings} from {@link CodeStyleSettings}.
     */
    @Nonnull
    public static RsCodeStyleSettings getRustSettings(@Nonnull CodeStyleSettings settings) {
        return RsFormatterUtil.getRustSettings(settings);
    }

    /**
     * Container for rustfmt-related testmarks.
     */
    public static final class RustfmtTestmarksCompat {
        public static final Testmark RustfmtUsed = RustfmtTestmarks.RustfmtUsed;

        private RustfmtTestmarksCompat() {
        }
    }
}
