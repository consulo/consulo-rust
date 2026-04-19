/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.stubs.*;

import java.util.EnumSet;
import java.util.Set;

public enum Namespace {
    Types("type"),
    Values("value"),
    Lifetimes("lifetime"),
    Macros("macro");

    @NotNull
    private final String myItemName;

    Namespace(@NotNull String itemName) {
        myItemName = itemName;
    }

    @NotNull
    public String getItemName() {
        return myItemName;
    }

    @NotNull
    public static final Set<Namespace> TYPES = EnumSet.of(Types);
    @NotNull
    public static final Set<Namespace> VALUES = EnumSet.of(Values);
    @NotNull
    public static final Set<Namespace> LIFETIMES = EnumSet.of(Lifetimes);
    @NotNull
    public static final Set<Namespace> MACROS_NS = EnumSet.of(Macros);
    @NotNull
    public static final Set<Namespace> MACROS = MACROS_NS;
    @NotNull
    public static final Set<Namespace> TYPES_N_VALUES = EnumSet.of(Types, Values);
    @NotNull
    public static final Set<Namespace> TYPES_N_VALUES_N_MACROS = EnumSet.of(Types, Values, Macros);

    /**
     * https://rust-lang.github.io/rfcs/0234-variants-namespace.html
     */
    @NotNull
    public static final Set<Namespace> ENUM_VARIANT_NS = TYPES_N_VALUES;

    @NotNull
    public static Set<Namespace> getNamespaces(@NotNull RsNamedElement element) {
        if (element instanceof RsMod
            || element instanceof RsModDeclItem
            || element instanceof RsEnumItem
            || element instanceof RsTraitItem
            || element instanceof RsTypeParameter
            || element instanceof RsTypeAlias) {
            return TYPES;
        }
        if (element instanceof RsPatBinding
            || element instanceof RsConstParameter
            || element instanceof RsConstant) {
            return VALUES;
        }
        if (element instanceof RsFunction) {
            if (RsFunctionUtil.isProcMacroDef((RsFunction) element)) {
                return MACROS_NS;
            }
            return VALUES;
        }
        if (element instanceof RsEnumVariant) {
            return ENUM_VARIANT_NS;
        }
        if (element instanceof RsStructItem) {
            if (((RsStructItem) element).getBlockFields() == null) {
                return TYPES_N_VALUES;
            }
            return TYPES;
        }
        if (element instanceof RsLifetimeParameter) {
            return LIFETIMES;
        }
        if (element instanceof RsMacro || element instanceof RsMacro2) {
            return MACROS_NS;
        }
        return TYPES_N_VALUES;
    }

    @NotNull
    public static Set<Namespace> getNamespaces(@NotNull StubElement<?> stub, @NotNull Crate crate) {
        if (stub instanceof RsModItemStub
            || stub instanceof RsModDeclItemStub
            || stub instanceof RsEnumItemStub
            || stub instanceof RsTraitItemStub
            || stub instanceof RsTypeParameterStub
            || stub instanceof RsTypeAliasStub) {
            return TYPES;
        }
        if (stub instanceof RsConstantStub) {
            return VALUES;
        }
        if (stub instanceof RsFunctionStub) {
            if (RsFunctionUtil.IS_PROC_MACRO_DEF_PROP.getByStub((RsFunctionStub) stub, crate)) {
                return MACROS_NS;
            }
            return VALUES;
        }
        if (stub instanceof RsEnumVariantStub) {
            return ENUM_VARIANT_NS;
        }
        if (stub instanceof RsStructItemStub) {
            if (((RsStructItemStub) stub).getBlockFields() == null) {
                return TYPES_N_VALUES;
            }
            return TYPES;
        }
        if (stub instanceof RsLifetimeParameterStub) {
            return LIFETIMES;
        }
        if (stub instanceof RsMacroStub || stub instanceof RsMacro2Stub) {
            return MACROS_NS;
        }
        return TYPES_N_VALUES;
    }

    @Nullable
    public static Set<Namespace> getUseSpeckNamespaces(@NotNull RsUseSpeck useSpeck) {
        RsPath path = useSpeck.getPath();
        if (path == null) return null;
        // Simplified: resolve and collect namespaces
        return null; // This needs to be implemented with proper reference resolution
    }
}
