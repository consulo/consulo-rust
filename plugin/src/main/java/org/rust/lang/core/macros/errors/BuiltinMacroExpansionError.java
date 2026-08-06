/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.errors;

public final class BuiltinMacroExpansionError extends MacroExpansionError {
    public static final BuiltinMacroExpansionError INSTANCE = new BuiltinMacroExpansionError();

    private BuiltinMacroExpansionError() {}

    @Override
    public String toString() {
        return "BuiltinMacroExpansionError";
    }
}
