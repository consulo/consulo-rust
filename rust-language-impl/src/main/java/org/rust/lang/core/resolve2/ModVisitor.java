/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.stubs.RsMacroCallStub;
import org.rust.lang.core.stubs.RsNamedStub;

/**
 * Visitor interface for mod collection.
 */
public interface ModVisitor {
    default void collectSimpleItem(@Nonnull SimpleItemLight item) {}
    default void collectModOrEnumItem(@Nonnull ModOrEnumItemLight item, @Nonnull RsNamedStub stub) {}
    default void collectImport(@Nonnull ImportLight importItem) {}
    default void collectMacroCall(@Nonnull MacroCallLight call, @Nonnull RsMacroCallStub stub) {}
    default void collectProcMacroCall(@Nonnull ProcMacroCallLight call) {}
    default void collectMacroDef(@Nonnull MacroDefLight def) {}
    default void collectMacro2Def(@Nonnull Macro2DefLight def) {}
    default void afterCollectMod() {}
}
