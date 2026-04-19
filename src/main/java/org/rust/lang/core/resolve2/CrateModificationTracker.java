/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.resolve.Namespace;

import java.util.*;

public final class CrateModificationTracker {

    private CrateModificationTracker() {}

    @NotNull
    public static List<NamedItem> exportedItems(@NotNull RsMod mod, @NotNull RsMod context) {
        RsModInfo info = FacadeResolveUtil.getModInfo(mod);
        if (info == null) return Collections.emptyList();
        RsModInfo contextInfo = FacadeResolveUtil.getModInfo(context);
        if (contextInfo == null) return Collections.emptyList();

        List<NamedItem> result = new ArrayList<>();
        List<Map.Entry<String, PerNs>> items = info.getModData().getVisibleItems(v -> v.isVisibleFromMod(contextInfo.getModData()));
        for (Map.Entry<String, PerNs> entry : items) {
            String name = entry.getKey();
            PerNs perNs = entry.getValue();
            // Simplified: just collect types for now
            for (VisItem visItem : perNs.getTypes()) {
                // Would need toPsi helper
            }
        }
        return result;
    }

    @NotNull
    public static Set<String> allScopeItemNames(@NotNull RsMod mod) {
        RsModInfo info = FacadeResolveUtil.getModInfo(mod);
        if (info == null) return Collections.emptySet();
        return info.getModData().getVisibleItems().keySet();
    }

    @Nullable
    public static VirtualFile getDirectoryContainedAllChildFiles(@NotNull RsMod mod) {
        RsModInfo info = FacadeResolveUtil.getModInfo(mod);
        if (info == null) return null;
        Integer dirId = info.getModData().getDirectoryContainedAllChildFiles();
        if (dirId == null) return null;
        return PersistentFS.getInstance().findFileById(dirId);
    }

    public static class NamedItem {
        @NotNull
        private final String name;
        @NotNull
        private final RsNamedElement item;

        public NamedItem(@NotNull String name, @NotNull RsNamedElement item) {
            this.name = name;
            this.item = item;
        }

        @NotNull
        public String getName() {
            return name;
        }

        @NotNull
        public RsNamedElement getItem() {
            return item;
        }
    }
}
