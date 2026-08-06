/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsProcMacroKind;
import org.rust.lang.core.resolve2.util.PerNsHashMap;
import org.rust.lang.core.resolve2.util.SmartListMap;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.ManagingFS;
import consulo.language.psi.PsiFile;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsModUtil;
import org.rust.openapiext.OpenApiUtil;

public class ModData {
    @Nullable
    private final ModData parent;
    private final int crate;
    @Nonnull
    private final ModPath path;
    @Nonnull
    private final MacroIndex macroIndex;
    private final boolean isDeeplyEnabledByCfgOuter;
    private final boolean isEnabledByCfgInner;
    @Nullable
    private final Integer fileId;
    @Nonnull
    private final String fileRelativePath;
    @Nullable
    private final Integer ownedDirectoryId;
    private final boolean hasPathAttribute;
    private final boolean hasMacroUse;
    private final boolean isEnum;
    private final boolean isNormalCrate;
    @Nullable
    private final ModData context;
    private final boolean isBlock;
    @Nonnull
    private final String crateDescription;

    @Nonnull
    private final ModData rootModData;
    @Nonnull
    private final Visibility.Restricted visibilityInSelf;
    @Nonnull
    private final PerNsHashMap<String> visibleItems;
    @Nonnull
    private final Map<String, ModData> childModules = new HashMap<>();
    @Nonnull
    private final SmartListMap<String, MacroDefInfo> legacyMacros = new SmartListMap<>();
    @Nonnull
    private final Map<String, DeclMacro2DefInfo> macros2 = new Object2ObjectOpenHashMap<>();
    @Nonnull
    private final Map<String, RsProcMacroKind> procMacros = new HashMap<>();
    @Nonnull
    private final Map<ModPath, Visibility> unnamedTraitImports = new Object2ObjectOpenHashMap<>();

    private boolean isShadowedByOtherFile = true;
    @Nullable
    private VisItem asVisItem;
    @Nullable
    private Integer directoryContainedAllChildFiles;

    private final long timestamp = System.nanoTime();

    public ModData(
        @Nullable ModData parent,
        int crate,
        @Nonnull ModPath path,
        @Nonnull MacroIndex macroIndex,
        boolean isDeeplyEnabledByCfgOuter,
        boolean isEnabledByCfgInner,
        @Nullable Integer fileId,
        @Nonnull String fileRelativePath,
        @Nullable Integer ownedDirectoryId,
        boolean hasPathAttribute,
        boolean hasMacroUse,
        boolean isEnum,
        boolean isNormalCrate,
        @Nullable ModData context,
        boolean isBlock,
        @Nonnull String crateDescription
    ) {
        this.parent = parent;
        this.crate = crate;
        this.path = path;
        this.macroIndex = macroIndex;
        this.isDeeplyEnabledByCfgOuter = isDeeplyEnabledByCfgOuter;
        this.isEnabledByCfgInner = isEnabledByCfgInner;
        this.fileId = fileId;
        this.fileRelativePath = fileRelativePath;
        this.ownedDirectoryId = ownedDirectoryId;
        this.hasPathAttribute = hasPathAttribute;
        this.hasMacroUse = hasMacroUse;
        this.isEnum = isEnum;
        this.isNormalCrate = isNormalCrate;
        this.context = context;
        this.isBlock = isBlock;
        this.crateDescription = crateDescription;
        this.rootModData = parent != null ? parent.rootModData : this;
        this.visibilityInSelf = Visibility.Restricted.create(this);
        this.visibleItems = new PerNsHashMap<>(this, rootModData);
        if (isNormalCrate) {
            this.directoryContainedAllChildFiles = ownedDirectoryId != null ? ownedDirectoryId :
                (parent != null ? parent.directoryContainedAllChildFiles : null);
        } else {
            this.directoryContainedAllChildFiles = null;
        }
    }

    /** Convenience constructor with defaults for optional fields. */
    public ModData(
        @Nullable ModData parent,
        int crate,
        @Nonnull ModPath path,
        @Nonnull MacroIndex macroIndex,
        boolean isDeeplyEnabledByCfgOuter,
        boolean isEnabledByCfgInner,
        @Nullable Integer fileId,
        @Nonnull String fileRelativePath,
        @Nullable Integer ownedDirectoryId,
        boolean hasPathAttribute,
        boolean hasMacroUse,
        @Nonnull String crateDescription
    ) {
        this(parent, crate, path, macroIndex, isDeeplyEnabledByCfgOuter, isEnabledByCfgInner,
            fileId, fileRelativePath, ownedDirectoryId, hasPathAttribute, hasMacroUse,
            false, true, null, false, crateDescription);
    }

    public boolean isRsFile() {
        return fileRelativePath.isEmpty();
    }

    public boolean isCrateRoot() {
        return parent == null;
    }

    @Nonnull
    public String getName() {
        return path.getName();
    }

    public boolean isDeeplyEnabledByCfg() {
        return isDeeplyEnabledByCfgOuter && isEnabledByCfgInner;
    }

    public boolean isHanging() {
        return context != null;
    }

    @Nullable
    public ModData getParent() {
        return parent;
    }

    public int getCrate() {
        return crate;
    }

    @Nonnull
    public ModPath getPath() {
        return path;
    }

    @Nonnull
    public MacroIndex getMacroIndex() {
        return macroIndex;
    }

    public boolean isDeeplyEnabledByCfgOuter() {
        return isDeeplyEnabledByCfgOuter;
    }

    public boolean isEnabledByCfgInner() {
        return isEnabledByCfgInner;
    }

    @Nullable
    public Integer getFileId() {
        return fileId;
    }

    @Nonnull
    public String getFileRelativePath() {
        return fileRelativePath;
    }

    @Nullable
    public Integer getOwnedDirectoryId() {
        return ownedDirectoryId;
    }

    public boolean isHasPathAttribute() {
        return hasPathAttribute;
    }

    public boolean isHasMacroUse() {
        return hasMacroUse;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public boolean isNormalCrate() {
        return isNormalCrate;
    }

    @Nullable
    public ModData getContext() {
        return context;
    }

    public boolean isBlock() {
        return isBlock;
    }

    @Nonnull
    public String getCrateDescription() {
        return crateDescription;
    }

    @Nonnull
    public ModData getRootModData() {
        return rootModData;
    }

    @Nonnull
    public Visibility.Restricted getVisibilityInSelf() {
        return visibilityInSelf;
    }

    @Nonnull
    public PerNsHashMap<String> getVisibleItems() {
        return visibleItems;
    }

    @Nonnull
    public Map<String, ModData> getChildModules() {
        return childModules;
    }

    @Nonnull
    public SmartListMap<String, MacroDefInfo> getLegacyMacros() {
        return legacyMacros;
    }

    @Nonnull
    public Map<String, DeclMacro2DefInfo> getMacros2() {
        return macros2;
    }

    @Nonnull
    public Map<String, RsProcMacroKind> getProcMacros() {
        return procMacros;
    }

    @Nonnull
    public Map<ModPath, Visibility> getUnnamedTraitImports() {
        return unnamedTraitImports;
    }

    public boolean isShadowedByOtherFile() {
        return isShadowedByOtherFile;
    }

    public void setShadowedByOtherFile(boolean shadowedByOtherFile) {
        isShadowedByOtherFile = shadowedByOtherFile;
    }

    @Nullable
    public VisItem getAsVisItem() {
        return asVisItem;
    }

    public void setAsVisItem(@Nullable VisItem asVisItem) {
        this.asVisItem = asVisItem;
    }

    @Nullable
    public Integer getDirectoryContainedAllChildFiles() {
        return directoryContainedAllChildFiles;
    }

    public void setDirectoryContainedAllChildFiles(@Nullable Integer directoryContainedAllChildFiles) {
        this.directoryContainedAllChildFiles = directoryContainedAllChildFiles;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean getHasPathAttributeRelativeToParentFile() {
        if (parent == null) return false;
        if (parent.isRsFile()) return hasPathAttribute;
        return parent.getHasPathAttributeRelativeToParentFile() || hasPathAttribute;
    }

    @Nonnull
    public PerNs getVisibleItem(@Nonnull String name) {
        PerNs result = visibleItems.getOrDefault(name, null);
        return result != null ? result : PerNs.Empty;
    }

    @Nonnull
    public List<Map.Entry<String, PerNs>> getVisibleItems(@Nonnull Predicate<Visibility> filterVisibility) {
        List<Map.Entry<String, PerNs>> usualItems = new ArrayList<>();
        for (Map.Entry<String, PerNs> entry : visibleItems.entrySet()) {
            PerNs filtered = entry.getValue().filterVisibility(filterVisibility);
            if (!filtered.isEmpty()) {
                usualItems.add(new AbstractMap.SimpleEntry<>(entry.getKey(), filtered));
            }
        }
        if (unnamedTraitImports.isEmpty()) return usualItems;

        for (Map.Entry<ModPath, Visibility> entry : unnamedTraitImports.entrySet()) {
            if (!filterVisibility.test(entry.getValue())) continue;
            VisItem trait = new VisItem(entry.getKey(), entry.getValue(), false, true);
            usualItems.add(new AbstractMap.SimpleEntry<>("_", PerNs.types(trait)));
        }
        return usualItems;
    }

    /** Returns true if visibleItems were changed */
    public boolean addVisibleItem(@Nonnull String name, @Nonnull PerNs def) {
        return DefCollectorUtil.pushResolutionFromImport(this, name, def);
    }

    @Nonnull
    public VisItem asVisItem() {
        if (isCrateRoot()) throw new IllegalStateException("Use CrateDefMap.rootAsPerNs for root ModData");
        assert asVisItem != null;
        return asVisItem;
    }

    @Nonnull
    public PerNs asPerNs() {
        if (context != null) return context.asPerNs();
        return PerNs.types(asVisItem());
    }

    @Nullable
    public ModData getChildModData(@Nonnull String[] relativePath) {
        ModData current = this;
        for (String segment : relativePath) {
            if (current == null) return null;
            current = current.childModules.get(segment);
        }
        return current;
    }

    @Nullable
    public ModData getNthParent(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0");
        ModData current = this;
        for (int i = 0; i < n; i++) {
            if (current.parent == null) return null;
            current = current.parent;
        }
        return current;
    }

    public void addLegacyMacro(@Nonnull String name, @Nonnull MacroDefInfo defInfo) {
        legacyMacros.addValue(name, defInfo);
    }

    public void addLegacyMacros(@Nonnull Map<String, DeclMacroDefInfo> defs) {
        for (Map.Entry<String, DeclMacroDefInfo> entry : defs.entrySet()) {
            addLegacyMacro(entry.getKey(), entry.getValue());
        }
    }

    public void visitDescendants(@Nonnull Consumer<ModData> visitor) {
        visitor.accept(this);
        for (ModData childMod : childModules.values()) {
            childMod.visitDescendants(visitor);
        }
    }

    /** Returns an iterable of parents starting from this */
    @Nonnull
    public Iterable<ModData> getParentsIterable() {
        return () -> new Iterator<ModData>() {
            ModData current = ModData.this;
            @Override
            public boolean hasNext() {
                return current != null;
            }
            @Override
            public ModData next() {
                ModData result = current;
                current = current.parent;
                return result;
            }
        };
    }

    /**
     * Converts this ModData to a list of corresponding RsMod PSI elements.
     */
    @Nonnull
    public List<RsMod> toRsMod(@Nonnull Project project) {
        if (isEnum || fileId == null) return Collections.emptyList();
        VirtualFile vFile = ManagingFS.getInstance().findFileById(fileId);
        if (vFile == null) return Collections.emptyList();
        PsiFile psiFile = OpenApiUtil.toPsiFile(vFile, project);
        if (!(psiFile instanceof RsFile)) return Collections.emptyList();
        RsFile rsFile = (RsFile) psiFile;
        if (isRsFile()) return Collections.singletonList(rsFile);

        String[] segments = fileRelativePath.split("::");
        List<RsMod> mods = Collections.singletonList(rsFile);
        for (int i = 1; i < segments.length; i++) {
            String segment = segments[i];
            List<RsMod> nextMods = new ArrayList<>();
            for (RsMod mod : mods) {
                RsMod child = RsModUtil.getChildModule(mod, segment);
                if (child != null) {
                    nextMods.add(child);
                }
            }
            mods = nextMods;
        }
        return mods;
    }

    /**
     * Process macros visible in this module.
     * <p>
     * (visibleItems lookup, legacy-macros walk, macro-index ordering, single-public-or-first
     * selection). Porting it depends on items-owner expanded-items + name-resolution
     * infrastructure that isn't yet ported (see also VisItem / ImportCandidatesCollector in
     * TODO_CONVERSION_PLAN.md). Returning {@code false} means "no macros found" — callers
     * gracefully fall back to other resolution strategies.
     */
    public boolean processMacros(
        @Nullable org.rust.lang.core.psi.RsPath macroPath,
        @Nonnull org.rust.lang.core.resolve.RsResolveProcessor processor,
        @Nonnull RsModInfo info
    ) {
        return false;
    }

    @Override
    @Nonnull
    public String toString() {
        return "ModData(" + path + ", crate=" + crateDescription + ")";
    }
}
