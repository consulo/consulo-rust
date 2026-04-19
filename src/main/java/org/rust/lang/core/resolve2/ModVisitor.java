/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.stubs.RsMacroCallStub;
import org.rust.lang.core.stubs.RsNamedStub;

/**
 * Visitor interface for mod collection.
 */
public interface ModVisitor {
    default void collectSimpleItem(@NotNull SimpleItemLight item) {}
    default void collectModOrEnumItem(@NotNull ModOrEnumItemLight item, @NotNull RsNamedStub stub) {}
    default void collectImport(@NotNull ImportLight importItem) {}
    default void collectMacroCall(@NotNull MacroCallLight call, @NotNull RsMacroCallStub stub) {}
    default void collectProcMacroCall(@NotNull ProcMacroCallLight call) {}
    default void collectMacroDef(@NotNull MacroDefLight def) {}
    default void collectMacro2Def(@NotNull Macro2DefLight def) {}
    default void afterCollectMod() {}
}
