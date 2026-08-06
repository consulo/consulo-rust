/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;
import org.rust.openapiext.TestAssertUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class DefCollectorUtil {

    private DefCollectorUtil() {}

    public static boolean pushResolutionFromImport(@Nonnull ModData modData, @Nonnull String name, @Nonnull PerNs def) {
        if (def.isEmpty()) throw new IllegalStateException("def is empty");

        // optimization: fast path
        PerNs defExisting = modData.getVisibleItems().putIfAbsent(name, def);
        if (defExisting == null) return true;

        return mergeResolutionFromImport(modData, name, def, defExisting);
    }

    private static boolean mergeResolutionFromImport(
        @Nonnull ModData modData,
        @Nonnull String name,
        @Nonnull PerNs def,
        @Nonnull PerNs defExisting
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

    @Nonnull
    private static VisItem[] mergeResolutionOneNs(
        @Nonnull VisItem[] visItems,
        @Nonnull VisItem[] visItemsExisting
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

    @Nonnull
    private static VisItem[] mergeResolutionOneNsMultiresolve(
        @Nonnull VisItem[] visItems,
        @Nonnull VisItem[] visItemsExisting
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

    @Nonnull
    public static VisibilityType visibilityType(@Nonnull VisItem[] items) {
        return items[0].getVisibility().getType();
    }

    @Nonnull
    private static ImportType importType(@Nonnull VisItem[] items) {
        boolean isFromNamedImport = items[0].isFromNamedImport();
        return isFromNamedImport ? ImportType.NAMED : ImportType.GLOB;
    }
}
