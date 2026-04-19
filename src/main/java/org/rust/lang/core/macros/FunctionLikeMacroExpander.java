/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.macros.builtin.BuiltinMacroExpander;
import org.rust.lang.core.macros.decl.DeclMacroExpander;
import org.rust.lang.core.macros.errors.MacroExpansionError;
import org.rust.lang.core.macros.proc.ProcMacroExpander;
import org.rust.stdext.RsResult;
import com.intellij.openapi.util.Pair;

/**
 * A macro expander for macro calls like {@code foo!()}
 */
public class FunctionLikeMacroExpander extends MacroExpander<RsMacroData, MacroExpansionError> {
    private final DeclMacroExpander myDecl;
    private final ProcMacroExpander myProc;
    private final BuiltinMacroExpander myBuiltin;

    public FunctionLikeMacroExpander(
        @NotNull DeclMacroExpander decl,
        @NotNull ProcMacroExpander proc,
        @NotNull BuiltinMacroExpander builtin
    ) {
        myDecl = decl;
        myProc = proc;
        myBuiltin = builtin;
    }

    @NotNull
    @Override
    public RsResult<Pair<CharSequence, RangeMap>, MacroExpansionError> expandMacroAsTextWithErr(
        @NotNull RsMacroData def,
        @NotNull RsMacroCallData call
    ) {
        if (def instanceof RsDeclMacroData) {
            return myDecl.expandMacroAsTextWithErr((RsDeclMacroData) def, call).mapErr(e -> e);
        } else if (def instanceof RsProcMacroData) {
            return myProc.expandMacroAsTextWithErr((RsProcMacroData) def, call).mapErr(e -> e);
        } else if (def instanceof RsBuiltinMacroData) {
            return myBuiltin.expandMacroAsTextWithErr((RsBuiltinMacroData) def, call).mapErr(e -> e);
        }
        throw new IllegalStateException("Unknown RsMacroData type: " + def.getClass());
    }

    @NotNull
    public static FunctionLikeMacroExpander forCrate(@NotNull Crate crate) {
        return new FunctionLikeMacroExpander(
            new DeclMacroExpander(crate.getProject()),
            ProcMacroExpander.forCrate(crate),
            new BuiltinMacroExpander(crate.getProject())
        );
    }
}
