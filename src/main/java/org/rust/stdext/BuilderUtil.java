/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import com.intellij.ui.dsl.builder.Panel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.function.Function;

public final class BuilderUtil {
    private BuilderUtil() {}

    @NotNull
    public static JPanel panel(@NotNull Function<Panel, ?> init) {
        // Stub: returns empty panel
        return new JPanel();
    }
}
