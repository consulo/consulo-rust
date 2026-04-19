/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.formatter.settings.RsCodeStyleSettings;
import org.rust.openapiext.Testmark;

/**
 * <p>
 * Contains formatting utility methods and testmarks.
 */
public final class Util {

    private Util() {
    }

    /**
     * Extension property equivalent: retrieves the {@link RsCodeStyleSettings} from {@link CodeStyleSettings}.
     */
    @NotNull
    public static RsCodeStyleSettings getRustSettings(@NotNull CodeStyleSettings settings) {
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
