/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsMacro2;
import org.rust.lang.core.psi.RsMacroCase;
import org.rust.lang.core.psi.RsMacroExpansionContents;
import org.rust.lang.core.psi.RsMacroPatternContents;
import org.rust.lang.core.stubs.RsMacro2Stub;

import java.util.List;

public final class RsMacro2Util {
    private RsMacro2Util() {
    }

    @Nonnull
    public static final StubbedAttributeProperty<RsMacro2, RsMacro2Stub> MACRO2_HAS_RUSTC_BUILTIN_MACRO_PROP =
        new StubbedAttributeProperty<>(QueryAttributes::hasRustcBuiltinMacro, RsMacro2Stub::getMayHaveRustcBuiltinMacro);

    @Nonnull
    public static String prepareMacroBody(@Nonnull RsMacro2 macro) {
        RsMacroPatternContents patternContents = macro.getMacroPatternContents();
        RsMacroExpansionContents expansionContents = macro.getMacroExpansionContents();
        if (patternContents != null && expansionContents != null) {
            return "{(" + patternContents.getText() + ") => {" + expansionContents.getText() + "}}";
        } else {
            List<RsMacroCase> cases = macro.getMacroCaseList();
            StringBuilder sb = new StringBuilder("{");
            for (RsMacroCase c : cases) {
                sb.append(c.getText());
            }
            sb.append("}");
            return sb.toString();
        }
    }
}
