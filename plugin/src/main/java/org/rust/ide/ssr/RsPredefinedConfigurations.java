/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.ssr;

import com.intellij.structuralsearch.PredefinedConfigurationUtil;
import com.intellij.structuralsearch.plugin.ui.Configuration;


import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.lang.RsFileType;

public final class RsPredefinedConfigurations {

    public static final RsPredefinedConfigurations INSTANCE = new RsPredefinedConfigurations();

    private static final String STRUCT_TYPE = "Rust/Structs";
    private static final String DECLARATIONS_TYPE = "Rust/Declarations";

    private RsPredefinedConfigurations() {
    }

    @Nonnull
    private static Configuration searchTemplate(
         @Nonnull String name,
         @Nonnull String refName,
         @Nonnull String pattern,
         @Nonnull String category
    ) {
        return PredefinedConfigurationUtil.createConfiguration(name, refName, pattern, category, RsFileType.INSTANCE);
    }

    @Nonnull
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
