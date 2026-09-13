/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.ManagingFS;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.resolve.Namespace;

import java.util.*;

public final class CrateModificationTracker {

    private CrateModificationTracker() {}

    @Nonnull
    public static List<NamedItem> exportedItems(@Nonnull RsMod mod, @Nonnull RsMod context) {
        RsModInfo info = FacadeResolveUtil.getModInfo(mod);
        if (info == null) return Collections.emptyList();
        RsModInfo contextInfo = FacadeResolveUtil.getModInfo(context);
        if (contextInfo == null) return Collections.emptyList();

        List<NamedItem> result = new ArrayList<>();
        List<Map.Entry<String, PerNs>> items = info.getModData().getVisibleItems(v -> v.isVisibleFromMod(contextInfo.getModData()));
        for (Map.Entry<String, PerNs> entry : items) {
            String name = entry.getKey();
            PerNs perNs = entry.getValue();
            collectInto(result, name, info, perNs.getTypes(), Namespace.Types);
            collectInto(result, name, info, perNs.getValues(), Namespace.Values);
            collectInto(result, name, info, perNs.getMacros(), Namespace.Macros);
        }
        return result;
    }

    private static void collectInto(
        @Nonnull List<NamedItem> result,
        @Nonnull String name,
        @Nonnull RsModInfo info,
        @Nonnull VisItem[] visItems,
        @Nonnull Namespace namespace
    ) {
        for (VisItem visItem : visItems) {
            for (RsNamedElement psi : visItem.toPsi(info, namespace)) {
                result.add(new NamedItem(name, psi));
            }
        }
    }

    @Nonnull
    public static Set<String> allScopeItemNames(@Nonnull RsMod mod) {
        RsModInfo info = FacadeResolveUtil.getModInfo(mod);
        if (info == null) return Collections.emptySet();
        return info.getModData().getVisibleItems().keySet();
    }

    @Nullable
    public static VirtualFile getDirectoryContainedAllChildFiles(@Nonnull RsMod mod) {
        RsModInfo info = FacadeResolveUtil.getModInfo(mod);
        if (info == null) return null;
        Integer dirId = info.getModData().getDirectoryContainedAllChildFiles();
        if (dirId == null) return null;
        return ManagingFS.getInstance().findFileById(dirId);
    }

    public static class NamedItem {
        @Nonnull
        private final String name;
        @Nonnull
        private final RsNamedElement item;

        public NamedItem(@Nonnull String name, @Nonnull RsNamedElement item) {
            this.name = name;
            this.item = item;
        }

        @Nonnull
        public String getName() {
            return name;
        }

        @Nonnull
        public RsNamedElement getItem() {
            return item;
        }
    }
}
