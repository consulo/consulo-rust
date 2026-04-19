/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.formatter.settings.RsCodeStyleSettings;
import org.rust.openapiext.Testmark;

public final class RsFormatterUtil {

    private RsFormatterUtil() {
    }

    @NotNull
    public static RsCodeStyleSettings getRustSettings(@NotNull CodeStyleSettings settings) {
        return settings.getCustomSettings(RsCodeStyleSettings.class);
    }
}
