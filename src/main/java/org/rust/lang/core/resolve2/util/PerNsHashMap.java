/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2.util;

import com.intellij.util.ArrayUtil;
import gnu.trove.THash;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.resolve2.*;

/**
 * Memory-optimized implementation of HashMap&lt;String, PerNs&gt;.
 * On real projects only few percents of PerNs has more than one VisItem.
 * So almost all PerNs has exactly one VisItem,
 * and we can try to use HashMap&lt;String, Any&gt;, where value is either PerNs or VisItem.
 * Here we go even further, and inline VisItem right into items array.
 *
 * Deletion is not supported.
 */
public class PerNsHashMap<K> extends THashMapBase<K, PerNs> {

    @NotNull
    private final ModData containingMod;
    @NotNull
    private final ModData rootMod;

    private Object[] items = THash.EMPTY_OBJECT_ARRAY;
    private byte[] masks = ArrayUtil.EMPTY_BYTE_ARRAY;

    public PerNsHashMap(@NotNull ModData containingMod, @NotNull ModData rootMod) {
        this.containingMod = containingMod;
        this.rootMod = rootMod;
    }

    @Override
    @Nullable
    protected PerNs getValueAtIndex(int index) {
        Object path = items[index];
        if (path == null) return null;
        if (!(path instanceof ModPath)) {
            @SuppressWarnings("unchecked")
            PerNs result = (PerNs) path;
            return result;
        }
        ModPath modPath = (ModPath) path;
        int mask = masks[index] & 0xFF;
        boolean isModOrEnum = decodeIsModOrEnum(mask);
        boolean isTrait = decodeIsTrait(mask);
        Namespace namespace = decodeNamespace(mask);
        Visibility visibility = decodeVisibility(mask);
        boolean isFromNamedImport = decodeIsFromNamedImport(mask);
        VisItem visItem = new VisItem(modPath, visibility, isModOrEnum, isTrait, isFromNamedImport);

        VisItem[] visItemArray = new VisItem[]{visItem};
        switch (namespace) {
            case Types: return new PerNs(visItemArray, VisItem.EMPTY_ARRAY, VisItem.EMPTY_ARRAY);
            case Values: return new PerNs(VisItem.EMPTY_ARRAY, visItemArray, VisItem.EMPTY_ARRAY);
            case Macros: return new PerNs(VisItem.EMPTY_ARRAY, VisItem.EMPTY_ARRAY, visItemArray);
            default: throw new IllegalStateException("unreachable");
        }
    }

    @Override
    protected void setValueAtIndex(int index, @NotNull PerNs value) {
        com.intellij.openapi.util.Pair<VisItem, Namespace> single = PerNsHashMapUtil.asSingleVisItem(value);
        if (single == null) {
            items[index] = value;
            return;
        }
        VisItem visItem = single.getFirst();
        Namespace namespace = single.getSecond();
        Byte mask = encodeMask(visItem, namespace);
        if (mask == null) {
            items[index] = value;
            return;
        }

        items[index] = visItem.getPath();
        masks[index] = mask;
    }

    @Override
    protected void createNewArrays(int capacity) {
        items = new Object[capacity];
        masks = new byte[capacity];
    }

    @Override
    protected void rehash(int newCapacity) {
        Object[] oldItems = items;
        byte[] oldMasks = masks;
        rehashTemplate(newCapacity, (newIndex, oldIndex) -> {
            items[newIndex] = oldItems[oldIndex];
            masks[newIndex] = oldMasks[oldIndex];
        });
    }

    @Override
    public int size() {
        return _size;
    }

    @Nullable
    private Byte encodeMask(@NotNull VisItem visItem, @NotNull Namespace namespace) {
        int namespaceMask;
        if (visItem.isModOrEnum()) {
            namespaceMask = 0;
        } else if (visItem.isTrait()) {
            namespaceMask = 1;
        } else if (namespace == Namespace.Types) {
            namespaceMask = 2;
        } else if (namespace == Namespace.Values) {
            namespaceMask = 3;
        } else if (namespace == Namespace.Macros) {
            namespaceMask = 4;
        } else {
            throw new IllegalStateException("unreachable");
        }

        Visibility visibility = visItem.getVisibility();
        int visibilityMask;
        if (visibility == Visibility.CFG_DISABLED) {
            visibilityMask = 0;
        } else if (visibility == Visibility.INVISIBLE) {
            visibilityMask = 1;
        } else if (visibility == Visibility.PUBLIC) {
            visibilityMask = 2;
        } else if (visibility instanceof Visibility.Restricted) {
            ModData scope = ((Visibility.Restricted) visibility).getInMod();
            if (scope == containingMod) {
                visibilityMask = 3;
            } else if (scope.isCrateRoot()) {
                visibilityMask = 4;
            } else if (scope == containingMod.getParent()) {
                visibilityMask = 5;
            } else {
                return null;
            }
        } else {
            throw new IllegalStateException("unreachable");
        }

        int isFromNamedImportMask = visItem.isFromNamedImport() ? 1 : 0;
        int mask = namespaceMask | (visibilityMask << 3) | (isFromNamedImportMask << 6);
        return (byte) mask;
    }

    private boolean decodeIsFromNamedImport(int mask) {
        return (mask & 0b1_000_000) != 0;
    }

    private boolean decodeIsModOrEnum(int mask) {
        return (mask & 0b111) == 0;
    }

    private boolean decodeIsTrait(int mask) {
        return (mask & 0b111) == 1;
    }

    @NotNull
    private Namespace decodeNamespace(int mask) {
        switch (mask & 0b111) {
            case 0:
            case 1:
            case 2:
                return Namespace.Types;
            case 3:
                return Namespace.Values;
            case 4:
                return Namespace.Macros;
            default:
                throw new IllegalStateException("unreachable");
        }
    }

    @NotNull
    private Visibility decodeVisibility(int mask) {
        switch ((mask >> 3) & 0b111) {
            case 0: return Visibility.CFG_DISABLED;
            case 1: return Visibility.INVISIBLE;
            case 2: return Visibility.PUBLIC;
            case 3: return containingMod.getVisibilityInSelf();
            case 4: return rootMod.getVisibilityInSelf();
            case 5:
                ModData parent = containingMod.getParent();
                if (parent == null) throw new IllegalStateException("Inconsistent mask in PerNsHashMap");
                return parent.getVisibilityInSelf();
            default:
                throw new IllegalStateException("unreachable");
        }
    }
}
