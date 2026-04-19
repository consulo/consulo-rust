/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.stubs.RsMetaItemStub;
import org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Allows for easy querying {@link RsDocAndAttributeOwner} for specific attributes.
 *
 * <b>Do not instantiate directly</b>, use {@link RsDocAndAttributeOwnerKt#getQueryAttributes} instead.
 */
public class QueryAttributes<T extends RsMetaItemPsiOrStub> {

    @NotNull
    private final Iterable<T> metaItems;

    public QueryAttributes(@NotNull Iterable<T> metaItems) {
        this.metaItems = metaItems;
    }

    @SuppressWarnings("unchecked")
    public QueryAttributes(@NotNull Stream<? extends T> metaItems) {
        this.metaItems = (Iterable<T>) (Iterable<?>) metaItems.collect(java.util.stream.Collectors.toList());
    }

    @NotNull
    public Iterable<T> getMetaItems() {
        return metaItems;
    }

    // #[doc(hidden)]
    public boolean isDocHidden() {
        return hasAttributeWithArg("doc", "hidden");
    }

    // #[cfg(test)], #[cfg(target_has_atomic = "ptr")]
    public boolean hasCfgAttr() {
        return hasAttribute("cfg");
    }

    // `#[attributeName]`, `#[attributeName(arg)]`, `#[attributeName = "Xxx"]`
    public boolean hasAttribute(@NotNull String attributeName) {
        for (T item : metaItems) {
            if (attributeName.equals(RsMetaItemUtil.getName(item))) return true;
        }
        return false;
    }

    public boolean hasAnyOfAttributes(@NotNull String... names) {
        for (T item : metaItems) {
            String name = RsMetaItemUtil.getName(item);
            if (name != null) {
                for (String n : names) {
                    if (n.equals(name)) return true;
                }
            }
        }
        return false;
    }

    // `#[attributeName]`
    public boolean hasAtomAttribute(@NotNull String attributeName) {
        for (T item : metaItems) {
            if (attributeName.equals(RsMetaItemUtil.getName(item))
                && !item.getHasEq()
                && item.getMetaItemArgs() == null) {
                return true;
            }
        }
        return false;
    }

    // `#[attributeName(arg)]`
    public boolean hasAttributeWithArg(@NotNull String attributeName, @NotNull String arg) {
        for (T item : metaItems) {
            if (attributeName.equals(RsMetaItemUtil.getName(item))) {
                for (RsMetaItemPsiOrStub sub : item.getMetaItemArgsList()) {
                    if (arg.equals(RsMetaItemUtil.getName(sub))) return true;
                }
            }
        }
        return false;
    }

    // `#[attributeName = "value"]`
    public boolean hasAttributeWithValue(@NotNull String attributeName, @NotNull String value) {
        for (T item : metaItems) {
            if (attributeName.equals(RsMetaItemUtil.getName(item)) && value.equals(item.getValue())) {
                return true;
            }
        }
        return false;
    }

    // `#[attributeName(arg)]`
    @Nullable
    public String getFirstArgOfSingularAttribute(@NotNull String attributeName) {
        T singular = null;
        for (T item : metaItems) {
            if (attributeName.equals(RsMetaItemUtil.getName(item))) {
                if (singular != null) return null; // more than one
                singular = item;
            }
        }
        if (singular == null) return null;
        java.util.List<? extends RsMetaItemPsiOrStub> args = singular.getMetaItemArgsList();
        return args.isEmpty() ? null : RsMetaItemUtil.getName(args.get(0));
    }

    // `#[attributeName(key = "value")]`
    public boolean hasAttributeWithKeyValue(@NotNull String attributeName, @NotNull String key, @NotNull String value) {
        for (T item : metaItems) {
            if (attributeName.equals(RsMetaItemUtil.getName(item))) {
                for (RsMetaItemPsiOrStub sub : item.getMetaItemArgsList()) {
                    if (key.equals(RsMetaItemUtil.getName(sub)) && value.equals(sub.getValue())) return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public String lookupStringValueForKey(@NotNull String key) {
        String result = null;
        for (T item : metaItems) {
            if (key.equals(RsMetaItemUtil.getName(item))) {
                String value = item.getValue();
                if (value != null) {
                    if (result != null) return null; // more than one
                    result = value;
                }
            }
        }
        return result;
    }

    // #[lang = "copy"]
    @Nullable
    public String getLangAttribute() {
        for (T item : metaItems) {
            if ("lang".equals(RsMetaItemUtil.getName(item)) && item.getValue() != null) {
                return item.getValue();
            }
        }
        return null;
    }

    // #[lang = "copy"]
    @NotNull
    public Iterable<String> getLangAttributes() {
        return () -> {
            java.util.Iterator<T> iter = metaItems.iterator();
            return new java.util.Iterator<String>() {
                String next = advance();

                private String advance() {
                    while (iter.hasNext()) {
                        T item = iter.next();
                        if ("lang".equals(RsMetaItemUtil.getName(item)) && item.getValue() != null) {
                            return item.getValue();
                        }
                    }
                    return null;
                }

                @Override
                public boolean hasNext() {
                    return next != null;
                }

                @Override
                public String next() {
                    String result = next;
                    next = advance();
                    return result;
                }
            };
        };
    }

    // #[derive(Clone)], #[derive(Copy, Clone, Debug)]
    @NotNull
    public Iterable<T> getDeriveAttributes() {
        return attrsByName("derive");
    }

    // #[repr(u16)], #[repr(C, packed)], #[repr(simd)], #[repr(align(8))]
    @NotNull
    public Iterable<T> getReprAttributes() {
        return attrsByName("repr");
    }

    // #[deprecated(since, note)]
    @Nullable
    public T getDeprecatedAttribute() {
        for (T item : metaItems) {
            if ("deprecated".equals(RsMetaItemUtil.getName(item))) return item;
        }
        return null;
    }

    @NotNull
    public Iterable<T> getCfgAttributes() {
        return attrsByName("cfg");
    }

    @NotNull
    public Iterable<T> getUnstableAttributes() {
        return attrsByName("unstable");
    }

    @NotNull
    public Iterable<T> attrsByName(@NotNull String name) {
        return () -> {
            java.util.Iterator<T> iter = metaItems.iterator();
            return new java.util.Iterator<T>() {
                T next = advance();

                private T advance() {
                    while (iter.hasNext()) {
                        T item = iter.next();
                        if (name.equals(RsMetaItemUtil.getName(item))) return item;
                    }
                    return null;
                }

                @Override
                public boolean hasNext() {
                    return next != null;
                }

                @Override
                public T next() {
                    T result = next;
                    next = advance();
                    return result;
                }
            };
        };
    }


    // hasMacroExport
    public boolean hasMacroExport() {
        return hasAttribute("macro_export");
    }

    // hasMacroExportLocalInnerMacros
    public boolean hasMacroExportLocalInnerMacros() {
        return hasAttributeWithArg("macro_export", "local_inner_macros");
    }

    // hasRustcBuiltinMacro
    public boolean hasRustcBuiltinMacro() {
        return hasAttribute("rustc_builtin_macro");
    }

    // isCustomDeriveProcMacroDef
    public boolean isCustomDeriveProcMacroDef() {
        return hasAttribute("proc_macro_derive");
    }

    // isProcMacroDef
    public boolean isProcMacroDef() {
        return hasAnyOfAttributes("proc_macro", "proc_macro_attribute", "proc_macro_derive");
    }

    // isRustcDocOnlyMacro
    public boolean isRustcDocOnlyMacro() {
        return hasAttribute("rustc_doc_only_macro");
    }

    // isTest
    public boolean isTest() {
        for (Object meta : (Iterable<?>) metaItems) {
            if (meta instanceof RsMetaItemPsiOrStub) {
                org.rust.lang.core.stubs.common.RsPathPsiOrStub path = ((RsMetaItemPsiOrStub) meta).getPath();
                if (path != null) {
                    String refName = path.getReferenceName();
                    if (refName != null && refName.contains("test")) return true;
                }
            }
        }
        return hasAtomAttribute("quickcheck");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("QueryAttributes(");
        boolean first = true;
        for (T item : metaItems) {
            if (!first) sb.append(", ");
            first = false;
            if (item instanceof RsMetaItem) {
                sb.append(((RsMetaItem) item).getText());
            } else if (item instanceof RsMetaItemStub) {
                sb.append(((RsMetaItemStub) item).getPsi().getText());
            } else {
                sb.append("?");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Returns the derive meta items that can be custom derive proc macros.
     */
    @NotNull
    public Iterable<T> getCustomDeriveMetaItems() {
        java.util.List<T> result = new java.util.ArrayList<>();
        for (T meta : metaItems) {
            if ("derive".equals(meta.getName())) {
                org.rust.lang.core.stubs.common.RsMetaItemArgsPsiOrStub args = meta.getMetaItemArgs();
                if (args != null) {
                    for (T derive : (Iterable<T>) (Iterable<?>) args.getMetaItemList()) {
                        if (org.rust.lang.core.psi.RsProcMacroPsiUtil.canBeCustomDeriveWithoutContextCheck(derive)) {
                            result.add(derive);
                        }
                    }
                }
            }
        }
        return result;
    }

    // #[stable], #[unstable]
    @Nullable
    public org.rust.lang.core.psi.RsStability getStability() {
        for (T item : metaItems) {
            String name = RsMetaItemUtil.getName(item);
            if ("stable".equals(name)) return org.rust.lang.core.psi.RsStability.Stable;
            if ("unstable".equals(name)) return org.rust.lang.core.psi.RsStability.Unstable;
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private static final QueryAttributes EMPTY = new QueryAttributes<>(java.util.Collections.emptyList());

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T extends RsMetaItemPsiOrStub> QueryAttributes<T> empty() {
        return (QueryAttributes<T>) EMPTY;
    }
}
