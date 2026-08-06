/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter;

import consulo.language.codeStyle.CodeStyleSettings;
import jakarta.annotation.Nonnull;
import org.rust.ide.formatter.settings.RsCodeStyleSettings;
import org.rust.openapiext.Testmark;

public final class RsFormatterUtil {

    private RsFormatterUtil() {
    }

    @Nonnull
    public static RsCodeStyleSettings getRustSettings(@Nonnull CodeStyleSettings settings) {
        return settings.getCustomSettings(RsCodeStyleSettings.class);
    }
}
