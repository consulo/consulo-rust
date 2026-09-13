/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.resolve.Namespace;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class PerNs {

    public static final PerNs Empty = new PerNs(VisItem.EMPTY_ARRAY, VisItem.EMPTY_ARRAY, VisItem.EMPTY_ARRAY);

    /** Alias for code that references PerNs.EMPTY */
    public static final PerNs EMPTY = Empty;

    @Nonnull
    private final VisItem[] types;
    @Nonnull
    private final VisItem[] values;
    @Nonnull
    private final VisItem[] macros;

    public PerNs() {
        this(VisItem.EMPTY_ARRAY, VisItem.EMPTY_ARRAY, VisItem.EMPTY_ARRAY);
    }

    public PerNs(@Nonnull VisItem[] types, @Nonnull VisItem[] values, @Nonnull VisItem[] macros) {
        this.types = types;
        this.values = values;
        this.macros = macros;
    }

    @Nonnull
    public VisItem[] getTypes() {
        return types;
    }

    @Nonnull
    public VisItem[] getValues() {
        return values;
    }

    @Nonnull
    public VisItem[] getMacros() {
        return macros;
    }

    public boolean isEmpty() {
        return types.length == 0 && values.length == 0 && macros.length == 0;
    }

    public boolean hasAllNamespaces() {
        return types.length > 0 && values.length > 0 && macros.length > 0;
    }

    @Nonnull
    public PerNs adjust(@Nonnull Visibility visibility, boolean isFromNamedImport) {
        return new PerNs(
            map2Array(types, v -> v.adjust(visibility, isFromNamedImport)),
            map2Array(values, v -> v.adjust(visibility, isFromNamedImport)),
            map2Array(macros, v -> v.adjust(visibility, isFromNamedImport))
        );
    }

    @Nonnull
    public PerNs filterVisibility(@Nonnull Predicate<Visibility> filter) {
        return new PerNs(
            filterArray(types, v -> filter.test(v.getVisibility())),
            filterArray(values, v -> filter.test(v.getVisibility())),
            filterArray(macros, v -> filter.test(v.getVisibility()))
        );
    }

    @Nonnull
    public PerNs or(@Nonnull PerNs other) {
        if (isEmpty()) return other;
        if (other.isEmpty()) return this;
        return new PerNs(
            orArray(types, other.types),
            orArray(values, other.values),
            orArray(macros, other.macros)
        );
    }

    @Nonnull
    private static VisItem[] orArray(@Nonnull VisItem[] a, @Nonnull VisItem[] b) {
        if (a.length == 0) return b;
        if (b.length == 0) return a;
        VisibilityType aType = visibilityType(a);
        VisibilityType bType = visibilityType(b);
        return bType.isWider(aType) ? b : a;
    }

    @Nonnull
    public PerNs mapItems(@Nonnull Function<VisItem, VisItem> f) {
        return new PerNs(
            map2Array(types, f),
            map2Array(values, f),
            map2Array(macros, f)
        );
    }

    /**
     * Keeps only items with greatest visibility.
     * If all items have CfgDisabled visibility, keep only one item for performance reasons.
     */
    @Nonnull
    public PerNs adjustMultiresolve() {
        if (types.length <= 1 && values.length <= 1 && macros.length <= 1) return this;
        return new PerNs(
            adjustMultiresolveArray(types),
            adjustMultiresolveArray(values),
            adjustMultiresolveArray(macros)
        );
    }

    @Nonnull
    private static VisItem[] adjustMultiresolveArray(@Nonnull VisItem[] items) {
        if (items.length <= 1) return items;
        VisibilityType maxType = null;
        for (VisItem item : items) {
            VisibilityType t = item.getVisibility().getType();
            if (maxType == null || t.ordinal() > maxType.ordinal()) {
                maxType = t;
            }
        }
        if (maxType == VisibilityType.CfgDisabled) return new VisItem[]{items[0]};
        final VisibilityType finalMaxType = maxType;
        List<VisItem> filtered = new ArrayList<>();
        for (VisItem item : items) {
            if (item.getVisibility().getType() == finalMaxType) {
                filtered.add(item);
            }
        }
        return filtered.toArray(VisItem.EMPTY_ARRAY);
    }

    /**
     * The namespaces an item can occupy, in the order resolution visits them. Iterate these and ask
     * {@link #getVisItems(Namespace)}: one item often sits in several namespaces and shares a single
     * array between them, so the arrays cannot be used to tell the namespaces apart.
     */
    private static final Namespace[] ITEM_NAMESPACES = {Namespace.Types, Namespace.Values, Namespace.Macros};

    @Nonnull
    public static Namespace[] itemNamespaces() {
        return ITEM_NAMESPACES;
    }

    @Nonnull
    public VisItem[] getVisItems(@Nonnull Namespace namespace) {
        switch (namespace) {
            case Types: return types;
            case Values: return values;
            case Macros: return macros;
            default: return VisItem.EMPTY_ARRAY;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PerNs)) return false;
        PerNs perNs = (PerNs) other;
        return Arrays.equals(types, perNs.types)
            && Arrays.equals(values, perNs.values)
            && Arrays.equals(macros, perNs.macros);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(types);
        result = 31 * result + Arrays.hashCode(values);
        result = 31 * result + Arrays.hashCode(macros);
        return result;
    }

    @Nonnull
    public static PerNs from(@Nonnull VisItem visItem, @Nonnull Set<Namespace> ns) {
        VisItem[] visItemList = new VisItem[]{visItem};
        return new PerNs(
            ns.contains(Namespace.Types) ? visItemList : VisItem.EMPTY_ARRAY,
            ns.contains(Namespace.Values) ? visItemList : VisItem.EMPTY_ARRAY,
            ns.contains(Namespace.Macros) ? visItemList : VisItem.EMPTY_ARRAY
        );
    }

    @Nonnull
    public static PerNs types(@Nonnull VisItem visItem) {
        return new PerNs(new VisItem[]{visItem}, VisItem.EMPTY_ARRAY, VisItem.EMPTY_ARRAY);
    }

    @Nonnull
    public static PerNs macros(@Nonnull VisItem visItem) {
        return new PerNs(VisItem.EMPTY_ARRAY, VisItem.EMPTY_ARRAY, new VisItem[]{visItem});
    }

    @Nonnull
    public static VisibilityType visibilityType(@Nonnull VisItem[] items) {
        return items[0].getVisibility().getType();
    }

    // Helper methods

    @Nonnull
    private static VisItem[] map2Array(@Nonnull VisItem[] items, @Nonnull Function<VisItem, VisItem> f) {
        if (items.length == 0) return items;
        VisItem[] result = new VisItem[items.length];
        for (int i = 0; i < items.length; i++) {
            result[i] = f.apply(items[i]);
        }
        return result;
    }

    @Nonnull
    private static VisItem[] filterArray(@Nonnull VisItem[] items, @Nonnull Predicate<VisItem> predicate) {
        if (items.length == 0) return items;
        List<VisItem> result = new ArrayList<>();
        for (VisItem item : items) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result.toArray(VisItem.EMPTY_ARRAY);
    }
}
