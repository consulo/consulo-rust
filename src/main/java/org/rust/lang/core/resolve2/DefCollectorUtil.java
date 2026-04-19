/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;
import org.rust.openapiext.TestAssertUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class DefCollectorUtil {

    private DefCollectorUtil() {}

    public static boolean pushResolutionFromImport(@NotNull ModData modData, @NotNull String name, @NotNull PerNs def) {
        if (def.isEmpty()) throw new IllegalStateException("def is empty");

        // optimization: fast path
        PerNs defExisting = modData.getVisibleItems().putIfAbsent(name, def);
        if (defExisting == null) return true;

        return mergeResolutionFromImport(modData, name, def, defExisting);
    }

    private static boolean mergeResolutionFromImport(
        @NotNull ModData modData,
        @NotNull String name,
        @NotNull PerNs def,
        @NotNull PerNs defExisting
    ) {
        VisItem[] typesNew = mergeResolutionOneNs(def.getTypes(), defExisting.getTypes());
        VisItem[] valuesNew = mergeResolutionOneNs(def.getValues(), defExisting.getValues());
        VisItem[] macrosNew = mergeResolutionOneNs(def.getMacros(), defExisting.getMacros());
        if (Arrays.equals(defExisting.getTypes(), typesNew)
            && Arrays.equals(defExisting.getValues(), valuesNew)
            && Arrays.equals(defExisting.getMacros(), macrosNew)) {
            return false;
        }
        modData.getVisibleItems().put(name, new PerNs(typesNew, valuesNew, macrosNew));
        return true;
    }

    @NotNull
    private static VisItem[] mergeResolutionOneNs(
        @NotNull VisItem[] visItems,
        @NotNull VisItem[] visItemsExisting
    ) {
        if (visItems.length == 0) return visItemsExisting;
        if (visItemsExisting.length == 0) return visItems;

        VisibilityType visibilityType = visibilityType(visItems);
        VisibilityType visibilityTypeExisting = visibilityType(visItemsExisting);
        if (visibilityType.isWider(visibilityTypeExisting)) return visItems;
        if (visibilityTypeExisting.isWider(visibilityType)) return visItemsExisting;

        ImportType importType = importType(visItems);
        ImportType importTypeExisting = importType(visItemsExisting);
        if (importType == ImportType.GLOB && importTypeExisting == ImportType.NAMED) return visItemsExisting;
        if (importType == ImportType.NAMED && importTypeExisting == ImportType.GLOB) return visItems;

        if (visibilityTypeExisting == VisibilityType.CfgDisabled && visibilityType == VisibilityType.CfgDisabled) {
            return visItems;
        }

        return mergeResolutionOneNsMultiresolve(visItems, visItemsExisting);
    }

    @NotNull
    private static VisItem[] mergeResolutionOneNsMultiresolve(
        @NotNull VisItem[] visItems,
        @NotNull VisItem[] visItemsExisting
    ) {
        Map<ModPath, VisItem> result = new HashMap<>();
        for (VisItem item : visItemsExisting) {
            result.put(item.getPath(), item);
        }
        for (VisItem visItem : visItems) {
            VisItem existing = result.get(visItem.getPath());
            if (existing == null) {
                result.put(visItem.getPath(), visItem);
            } else if (visItem.getVisibility().isStrictlyMorePermissive(existing.getVisibility())) {
                result.put(visItem.getPath(), visItem);
            }
        }
        return result.values().toArray(VisItem.EMPTY_ARRAY);
    }

    @NotNull
    public static VisibilityType visibilityType(@NotNull VisItem[] items) {
        return items[0].getVisibility().getType();
    }

    @NotNull
    private static ImportType importType(@NotNull VisItem[] items) {
        boolean isFromNamedImport = items[0].isFromNamedImport();
        return isFromNamedImport ? ImportType.NAMED : ImportType.GLOB;
    }
}
