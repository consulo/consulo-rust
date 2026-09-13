/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import com.intellij.ui.dsl.builder.Panel;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.function.Function;

public final class BuilderUtil {
    private BuilderUtil() {}

    @Nonnull
    public static JPanel panel(@Nonnull Function<Panel, ?> init) {
        // Stub: returns empty panel
        return new JPanel();
    }
}
