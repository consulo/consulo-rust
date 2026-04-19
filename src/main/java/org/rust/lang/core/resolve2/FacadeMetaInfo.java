/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.resolve.NameResolution;
import org.rust.openapiext.OpenApiUtil;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsFile;

import java.util.*;

/**
 * Provides utility functions for metadata, named items, and checking if a crate has changed.
 */
public final class FacadeMetaInfo {

    private FacadeMetaInfo() {}

    /**
     * Represents a named item, holding a name and the RsNamedElement.
     */
    public static final class NamedItem {
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NamedItem)) return false;
            NamedItem that = (NamedItem) o;
            return name.equals(that.name) && item.equals(that.item);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, item);
        }
    }

    /**
     * List of items added to context by glob import to scope.
     * Extension function on RsMod: RsMod.exportedItems(context)
     */
    @NotNull
    public static List<NamedItem> exportedItems(@NotNull RsMod scope, @NotNull RsMod context) {
        RsModInfo info = FacadeResolve.getModInfo(scope);
        if (info == null) return Collections.emptyList();
        RsModInfo contextInfo = FacadeResolve.getModInfo(context);
        if (contextInfo == null) return Collections.emptyList();
        List<NamedItem> result = new ArrayList<>();
        ModData modData = info.getModData();
        for (Map.Entry<String, PerNs> entry : modData.getVisibleItems(it -> it.isVisibleFromMod(contextInfo.getModData()))) {
            String name = entry.getKey();
            PerNs perNs = entry.getValue();
            for (VisItem visItem : perNs.getTypes()) {
                for (RsNamedElement psi : visItem.toPsi(info, Namespace.Types)) {
                    result.add(new NamedItem(name, psi));
                }
            }
            for (VisItem visItem : perNs.getValues()) {
                for (RsNamedElement psi : visItem.toPsi(info, Namespace.Values)) {
                    result.add(new NamedItem(name, psi));
                }
            }
            for (VisItem visItem : perNs.getMacros()) {
                for (RsNamedElement psi : visItem.toPsi(info, Namespace.Macros)) {
                    result.add(new NamedItem(name, psi));
                }
            }
        }
        return result;
    }

    /**
     * Returns all scope item names.
     * Extension function on RsMod: RsMod.allScopeItemNames()
     */
    @NotNull
    public static Set<String> allScopeItemNames(@NotNull RsMod scope) {
        RsModInfo info = FacadeResolve.getModInfo(scope);
        if (info == null) return Collections.emptySet();
        return info.getModData().getVisibleItems().keySet();
    }

    /**
     * Returns the directory that contains all child files of a module.
     * Extension function on RsMod: RsMod.getDirectoryContainedAllChildFiles()
     */
    @Nullable
    public static VirtualFile getDirectoryContainedAllChildFiles(@NotNull RsMod scope) {
        RsModInfo info = FacadeResolve.getModInfo(scope);
        if (info == null) return null;
        Integer dirId = info.getModData().getDirectoryContainedAllChildFiles();
        if (dirId == null) return null;
        return PersistentFS.getInstance().findFileById(dirId);
    }

    /**
     * Returns whether there is a transitive glob import between source and target.
     */
    public static boolean hasTransitiveGlobImport(
        @NotNull CrateDefMap defMap,
        @NotNull RsMod source,
        @NotNull RsMod target
    ) {
        ModData sourceModData = defMap.getModData(source);
        if (sourceModData == null) return false;
        ModData targetModData = defMap.getModData(target);
        if (targetModData == null) return false;
        return defMap.getGlobImportGraph().hasTransitiveGlobImport(sourceModData, targetModData);
    }

    /**
     * Gets the recursion limit for the element's crate.
     */
    public static int getRecursionLimit(@NotNull PsiElement element) {
        RsElement rsElement = element instanceof RsElement ? (RsElement) element : null;
        if (rsElement == null) return NameResolution.DEFAULT_RECURSION_LIMIT;
        RsMod mod = RsElementUtil.getContainingMod(rsElement);
        if (mod == null) return NameResolution.DEFAULT_RECURSION_LIMIT;
        RsModInfo info = FacadeResolve.getModInfo(mod);
        if (info == null) return NameResolution.DEFAULT_RECURSION_LIMIT;
        return info.getDefMap().getRecursionLimit();
    }

    /**
     * Calculates new value of DefMapHolder.shouldRebuild field.
     */
    public static boolean getShouldRebuild(@NotNull DefMapHolder holder, @NotNull Crate crate) {
        OpenApiUtil.checkReadAccessAllowed();
        if (holder.isShouldRebuild()) return true;
        if (!holder.isShouldRecheck() && holder.getChangedFiles().isEmpty()) return false;
        CrateDefMap defMap = holder.getDefMap();
        if (defMap == null) return false;

        if (processChangedFiles(holder, crate, defMap)) return true;

        if (holder.isShouldRecheck()) {
            if (isCrateChanged(crate, defMap)) return true;
            holder.setShouldRecheck(false);
            if (processChangedFiles(holder, crate, defMap)) return true;
        }
        return false;
    }

    private static boolean processChangedFiles(
        @NotNull DefMapHolder holder,
        @NotNull Crate crate,
        @NotNull CrateDefMap defMap
    ) {
        Iterator<RsFile> iterator = holder.getChangedFiles().iterator();
        while (iterator.hasNext()) {
            ProgressManager.checkCanceled();
            RsFile file = iterator.next();
            if (FileModificationTracker.isFileChanged(file, defMap, crate)) {
                return true;
            } else {
                iterator.remove();
            }
        }
        return false;
    }

    /**
     * Checks if crate metadata has changed compared to the DefMap.
     */
    public static boolean isCrateChanged(@NotNull Crate crate, @NotNull CrateDefMap defMap) {
        ProgressManager.checkCanceled();
        CrateMetaData currentMeta = new CrateMetaData(crate.getEdition(), crate.getNormName(), crate.getProcMacroArtifact());
        return !currentMeta.equals(defMap.getMetaData());
    }
}
