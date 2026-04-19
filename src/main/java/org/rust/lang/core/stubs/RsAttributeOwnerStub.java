/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwner;
import org.rust.lang.core.stubs.common.RsAttributeOwnerPsiOrStub;
import org.rust.stdext.BitFlagsBuilder;
import org.rust.stdext.HashCode;

public interface RsAttributeOwnerStub extends RsAttributeOwnerPsiOrStub<RsMetaItemStub> {
    boolean getHasAttrs();
    boolean getMayHaveCfg();
    boolean getHasCfgAttr();
    boolean getMayHaveCustomDerive();
    boolean getMayHaveCustomAttrs();

    /**
     * Extract common attribute flags from a PSI element.
     */
    static int extractFlags(@NotNull RsDocAndAttributeOwner psi) {
        return extractFlags(psi, new CommonStubAttrFlags());
    }

    /**
     * Walks the element's traversed-and-flattened raw attributes (including those nested under
     * {@code #[cfg_attr(...)]}) and packs the relevant feature flags into a single int. Mirrors
     * {@code fun RsAttributeOwnerStub.Companion.extractFlags(element, bitflagsKind)} from
     * <p>
     * The {@code additionalFlags} parameter selects which subclass-specific bits to set
     * (mod, file, function, use, macro, macro2, impl); pass a {@link CommonStubAttrFlags}
     * (or any other type) to skip subclass extras.
     */
    static int extractFlags(@NotNull RsDocAndAttributeOwner psi, @NotNull BitFlagsBuilder additionalFlags) {
        org.rust.lang.core.psi.ext.QueryAttributes<org.rust.lang.core.psi.RsMetaItem> attrs =
            org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil.getTraversedRawAttributes(psi, true);

        boolean hasAttrs = false;
        boolean hasCfg = false;
        boolean hasCfgAttr = false;
        boolean hasCustomDerive = false;
        boolean hasCustomAttrs = false;
        boolean hasMacroUse = false;
        boolean hasStdlibAttrs = false;
        boolean hasRecursionLimit = false;
        boolean isProcMacroDef = false;
        boolean isPreludeImport = false;
        boolean hasMacroExport = false;
        boolean hasMacroExportLocalInnerMacros = false;
        boolean hasRustcBuiltinMacro = false;
        boolean isReservationImpl = false;

        java.util.Map<String, org.rust.lang.core.resolve.KnownDerivableTrait> derivable =
            org.rust.lang.core.resolve.KnownItems.getKNOWN_DERIVABLE_TRAITS();
        java.util.Map<String, ?> builtinAttrs = org.rust.lang.core.psi.BuiltinAttributes.RS_BUILTIN_ATTRIBUTES;
        java.util.Set<String> builtinTools = org.rust.lang.core.psi.BuiltinAttributes.RS_BUILTIN_TOOL_ATTRIBUTES;

        for (org.rust.lang.core.psi.RsMetaItem meta : attrs.getMetaItems()) {
            hasAttrs = true;
            org.rust.lang.core.psi.RsPath path = meta.getPath();
            if (path == null) continue;
            if (org.rust.lang.core.psi.ext.RsPathUtil.getHasColonColon(path)) {
                org.rust.lang.core.psi.RsPath basePath = org.rust.lang.core.psi.ext.RsPathUtil.basePath(path);
                if (basePath != path && !builtinTools.contains(basePath.getReferenceName())) {
                    hasCustomAttrs = true;
                }
            } else {
                String name = path.getReferenceName();
                if (name == null) continue;
                switch (name) {
                    case "cfg":
                        hasCfg = true;
                        break;
                    case "cfg_attr":
                        hasCfgAttr = true;
                        break;
                    case "derive": {
                        org.rust.lang.core.psi.RsMetaItemArgs args = meta.getMetaItemArgs();
                        if (args != null) {
                            for (org.rust.lang.core.psi.RsMetaItem arg : args.getMetaItemList()) {
                                org.rust.lang.core.resolve.KnownDerivableTrait kd =
                                    derivable.get(org.rust.lang.core.psi.ext.RsMetaItemUtil.getName(arg));
                                if (kd == null || !kd.isStd()) {
                                    hasCustomDerive = true;
                                    break;
                                }
                            }
                        }
                        break;
                    }
                    case "macro_use":
                        hasMacroUse = true;
                        break;
                    case "no_std":
                    case "no_core":
                        hasStdlibAttrs = true;
                        break;
                    case "recursion_limit":
                        hasRecursionLimit = true;
                        break;
                    case "proc_macro":
                    case "proc_macro_attribute":
                    case "proc_macro_derive":
                        isProcMacroDef = true;
                        break;
                    case "prelude_import":
                        isPreludeImport = true;
                        break;
                    case "macro_export": {
                        hasMacroExport = true;
                        for (org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub item : meta.getMetaItemArgsList()) {
                            if ("local_inner_macros".equals(item.getName())) {
                                hasMacroExportLocalInnerMacros = true;
                                break;
                            }
                        }
                        break;
                    }
                    case "rustc_builtin_macro":
                        hasRustcBuiltinMacro = true;
                        break;
                    case "rustc_reservation_impl":
                        isReservationImpl = true;
                        break;
                    default:
                        if (!builtinAttrs.containsKey(name)) {
                            hasCustomAttrs = true;
                        }
                        break;
                }
            }
        }

        int flags = 0;
        flags = BitUtil.set(flags, CommonStubAttrFlags.HAS_ATTRS, hasAttrs);
        flags = BitUtil.set(flags, CommonStubAttrFlags.MAY_HAVE_CFG, hasCfg);
        flags = BitUtil.set(flags, CommonStubAttrFlags.HAS_CFG_ATTR, hasCfgAttr);
        flags = BitUtil.set(flags, CommonStubAttrFlags.MAY_HAVE_CUSTOM_DERIVE, hasCustomDerive);
        flags = BitUtil.set(flags, CommonStubAttrFlags.MAY_HAVE_CUSTOM_ATTRS, hasCustomAttrs);

        if (additionalFlags instanceof ModStubAttrFlags) {
            flags = BitUtil.set(flags, ModStubAttrFlags.MAY_HAVE_MACRO_USE, hasMacroUse);
        } else if (additionalFlags instanceof FileStubAttrFlags) {
            flags = BitUtil.set(flags, ModStubAttrFlags.MAY_HAVE_MACRO_USE, hasMacroUse);
            flags = BitUtil.set(flags, FileStubAttrFlags.MAY_HAVE_STDLIB_ATTRIBUTES, hasStdlibAttrs);
            flags = BitUtil.set(flags, FileStubAttrFlags.MAY_HAVE_RECURSION_LIMIT, hasRecursionLimit);
        } else if (additionalFlags instanceof FunctionStubAttrFlags) {
            flags = BitUtil.set(flags, FunctionStubAttrFlags.MAY_BE_PROC_MACRO_DEF, isProcMacroDef);
        } else if (additionalFlags instanceof UseItemStubAttrFlags) {
            flags = BitUtil.set(flags, UseItemStubAttrFlags.MAY_HAVE_PRELUDE_IMPORT, isPreludeImport);
        } else if (additionalFlags instanceof MacroStubAttrFlags) {
            flags = BitUtil.set(flags, MacroStubAttrFlags.MAY_HAVE_MACRO_EXPORT, hasMacroExport);
            flags = BitUtil.set(flags, MacroStubAttrFlags.MAY_HAVE_MACRO_EXPORT_LOCAL_INNER_MACROS, hasMacroExportLocalInnerMacros);
            flags = BitUtil.set(flags, MacroStubAttrFlags.MAY_HAVE_RUSTC_BUILTIN_MACRO, hasRustcBuiltinMacro);
        } else if (additionalFlags instanceof Macro2StubAttrFlags) {
            flags = BitUtil.set(flags, Macro2StubAttrFlags.MAY_HAVE_RUSTC_BUILTIN_MACRO, hasRustcBuiltinMacro);
        } else if (additionalFlags instanceof ImplStubAttrFlags) {
            flags = BitUtil.set(flags, ImplStubAttrFlags.MAY_BE_RESERVATION_IMPL, isReservationImpl);
        }
        // CommonStubAttrFlags or unknown -> no additional bits
        return flags;
    }

    final class CommonStubAttrFlags extends BitFlagsBuilder {
        public static final CommonStubAttrFlags INSTANCE = new CommonStubAttrFlags();

        public static final int HAS_ATTRS;
        public static final int MAY_HAVE_CFG;
        public static final int HAS_CFG_ATTR;
        public static final int MAY_HAVE_CUSTOM_DERIVE;
        public static final int MAY_HAVE_CUSTOM_ATTRS;

        private CommonStubAttrFlags() {
            super(Limit.BYTE);
        }

        static {
            CommonStubAttrFlags instance = new CommonStubAttrFlags();
            HAS_ATTRS = instance.nextBitMask();
            MAY_HAVE_CFG = instance.nextBitMask();
            HAS_CFG_ATTR = instance.nextBitMask();
            MAY_HAVE_CUSTOM_DERIVE = instance.nextBitMask();
            MAY_HAVE_CUSTOM_ATTRS = instance.nextBitMask();
        }
    }

    final class ModStubAttrFlags extends BitFlagsBuilder {
        public static final int MAY_HAVE_MACRO_USE;

        static {
            ModStubAttrFlags instance = new ModStubAttrFlags();
            // skip common flags
            instance.nextBitMask(); instance.nextBitMask(); instance.nextBitMask(); instance.nextBitMask(); instance.nextBitMask();
            MAY_HAVE_MACRO_USE = instance.nextBitMask();
        }

        public ModStubAttrFlags() {
            super(Limit.BYTE);
        }
    }

    final class FileStubAttrFlags extends BitFlagsBuilder {
        public static final int MAY_HAVE_STDLIB_ATTRIBUTES;
        public static final int MAY_HAVE_RECURSION_LIMIT;

        static {
            FileStubAttrFlags instance = new FileStubAttrFlags();
            // skip common + mod flags
            for (int i = 0; i < 6; i++) instance.nextBitMask();
            MAY_HAVE_STDLIB_ATTRIBUTES = instance.nextBitMask();
            MAY_HAVE_RECURSION_LIMIT = instance.nextBitMask();
        }

        public FileStubAttrFlags() {
            super(Limit.BYTE);
        }
    }

    final class FunctionStubAttrFlags extends BitFlagsBuilder {
        public static final int MAY_BE_PROC_MACRO_DEF;

        static {
            FunctionStubAttrFlags instance = new FunctionStubAttrFlags();
            for (int i = 0; i < 5; i++) instance.nextBitMask();
            MAY_BE_PROC_MACRO_DEF = instance.nextBitMask();
        }

        public FunctionStubAttrFlags() {
            super(Limit.BYTE);
        }
    }

    final class UseItemStubAttrFlags extends BitFlagsBuilder {
        public static final int MAY_HAVE_PRELUDE_IMPORT;

        static {
            UseItemStubAttrFlags instance = new UseItemStubAttrFlags();
            for (int i = 0; i < 5; i++) instance.nextBitMask();
            MAY_HAVE_PRELUDE_IMPORT = instance.nextBitMask();
        }

        public UseItemStubAttrFlags() {
            super(Limit.BYTE);
        }
    }

    final class MacroStubAttrFlags extends BitFlagsBuilder {
        public static final int MAY_HAVE_MACRO_EXPORT;
        public static final int MAY_HAVE_MACRO_EXPORT_LOCAL_INNER_MACROS;
        public static final int MAY_HAVE_RUSTC_BUILTIN_MACRO;

        static {
            MacroStubAttrFlags instance = new MacroStubAttrFlags();
            for (int i = 0; i < 5; i++) instance.nextBitMask();
            MAY_HAVE_MACRO_EXPORT = instance.nextBitMask();
            MAY_HAVE_MACRO_EXPORT_LOCAL_INNER_MACROS = instance.nextBitMask();
            MAY_HAVE_RUSTC_BUILTIN_MACRO = instance.nextBitMask();
        }

        public MacroStubAttrFlags() {
            super(Limit.BYTE);
        }
    }

    final class Macro2StubAttrFlags extends BitFlagsBuilder {
        public static final int MAY_HAVE_RUSTC_BUILTIN_MACRO;

        static {
            Macro2StubAttrFlags instance = new Macro2StubAttrFlags();
            for (int i = 0; i < 5; i++) instance.nextBitMask();
            MAY_HAVE_RUSTC_BUILTIN_MACRO = instance.nextBitMask();
        }

        public Macro2StubAttrFlags() {
            super(Limit.BYTE);
        }
    }

    final class ImplStubAttrFlags extends BitFlagsBuilder {
        public static final int MAY_BE_RESERVATION_IMPL;

        static {
            ImplStubAttrFlags instance = new ImplStubAttrFlags();
            for (int i = 0; i < 5; i++) instance.nextBitMask();
            MAY_BE_RESERVATION_IMPL = instance.nextBitMask();
        }

        public ImplStubAttrFlags() {
            super(Limit.BYTE);
        }
    }
}
