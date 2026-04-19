/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.ssr;

import com.intellij.structuralsearch.PredefinedConfigurationUtil;
import com.intellij.structuralsearch.plugin.ui.Configuration;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.lang.RsFileType;

public final class RsPredefinedConfigurations {

    public static final RsPredefinedConfigurations INSTANCE = new RsPredefinedConfigurations();

    private static final String STRUCT_TYPE = "Rust/Structs";
    private static final String DECLARATIONS_TYPE = "Rust/Declarations";

    private RsPredefinedConfigurations() {
    }

    @NotNull
    private static Configuration searchTemplate(
        @Nls @NotNull String name,
        @NonNls @NotNull String refName,
        @NonNls @NotNull String pattern,
        @Nls @NotNull String category
    ) {
        return PredefinedConfigurationUtil.createConfiguration(name, refName, pattern, category, RsFileType.INSTANCE);
    }

    @NotNull
    public static Configuration[] createPredefinedTemplates() {
        return new Configuration[]{
            // Declarations
            searchTemplate(
                RsBundle.message("constants.equal.to.1"),
                "constants = 1",
                "const 'Name\\: '_t = 1;",
                DECLARATIONS_TYPE
            ),

            // Structs
            searchTemplate(
                RsBundle.message("structs.deriving.default"),
                "structs deriving default",
                "#[derive(Default)]\nstruct 'Name",
                STRUCT_TYPE
            ),
            searchTemplate(
                RsBundle.message("structs.with.a.u8.field"),
                "structs with a u8 field",
                "struct 'Name {\n    '_before*,\n    '_field\\: u8,\n    '_after*,\n}",
                STRUCT_TYPE
            ),
        };
    }
}
