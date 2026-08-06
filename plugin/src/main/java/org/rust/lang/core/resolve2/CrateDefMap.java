/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.rust.lang.core.psi.ext.RsModUtil;
import consulo.logging.Logger;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.settings.RustProjectSettingsServiceUtil;
import org.rust.cargo.project.workspace.CargoWorkspace;
// import removed
import org.rust.lang.core.psi.KnownProcMacroKind;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsProcMacroKind;
import org.rust.lang.core.resolve2.util.GlobImportGraph;
import org.rust.lang.core.resolve2.util.PerNsHashMap;
import org.rust.openapiext.VirtualFileExtUtil;
import org.rust.stdext.HashCode;

import java.nio.file.Path;
import java.util.*;
import org.rust.lang.core.resolve2.HangingModData;

public class CrateDefMap {
    private final int crate;
    @Nonnull
    private final ModData root;
    @Nonnull
    private final Map<String, CrateDefMap> directDependenciesDefMaps;
    @Nonnull
    private final Map<Integer, CrateDefMap> allDependenciesDefMaps;
    @Nonnull
    private final CrateMetaData metaData;
    private final int rootModMacroIndex;
    @Nonnull
    private final RsFile.Attributes stdlibAttributes;
    private final int recursionLimitRaw;
    @Nonnull
    private final String crateDescription;

    @Nullable
    private ModData prelude;
    @Nonnull
    private final Map<String, CrateDefMap> externPrelude;
    @Nonnull
    private final Map<String, CrateDefMap> externCratesInRoot = new HashMap<>();
    @Nonnull
    private final Map<Integer, FileInfo> fileInfos = new HashMap<>();
    @Nonnull
    private final List<Path> missedFiles = new ArrayList<>();

    private final long timestamp = System.nanoTime();
    @Nonnull
    private final PerNs rootAsPerNs;
    @Nonnull
    private final GlobImportGraph globImportGraph = new GlobImportGraph();
    @Nonnull
    private final Map<String, MacroCallLightInfo> expansionNameToMacroCall = new Object2ObjectOpenHashMap<>();
    @Nonnull
    private final Map<MacroIndex, String> macroCallToExpansionName;

    private final boolean isAtLeastEdition2018;

    public CrateDefMap(
        int crate,
        @Nonnull ModData root,
        @Nonnull Map<String, CrateDefMap> directDependenciesDefMaps,
        @Nonnull Map<Integer, CrateDefMap> allDependenciesDefMaps,
        @Nonnull Map<String, CrateDefMap> initialExternPrelude,
        @Nonnull CrateMetaData metaData,
        int rootModMacroIndex,
        @Nonnull RsFile.Attributes stdlibAttributes,
        int recursionLimitRaw,
        @Nonnull String crateDescription
    ) {
        this.crate = crate;
        this.root = root;
        this.directDependenciesDefMaps = directDependenciesDefMaps;
        this.allDependenciesDefMaps = allDependenciesDefMaps;
        this.metaData = metaData;
        this.rootModMacroIndex = rootModMacroIndex;
        this.stdlibAttributes = stdlibAttributes;
        this.recursionLimitRaw = recursionLimitRaw;
        this.crateDescription = crateDescription;

        this.externPrelude = new HashMap<>(initialExternPrelude);
        this.rootAsPerNs = PerNs.types(new VisItem(root.getPath(), Visibility.PUBLIC, true));
        this.isAtLeastEdition2018 = metaData.getEdition().compareTo(CargoWorkspace.Edition.EDITION_2018) >= 0;

        this.macroCallToExpansionName = Maps.newHashMap(new HashingStrategy<MacroIndex>() {
            @Override
            public boolean equals(MacroIndex index1, MacroIndex index2) {
                return MacroIndex.equals(index1, index2);
            }

            @Override
            public int hashCode(MacroIndex index) {
                return MacroIndex.hashCode(index);
            }
        });
    }

    public int getCrate() {
        return crate;
    }

    @Nonnull
    public ModData getRoot() {
        return root;
    }

    @Nonnull
    public Map<String, CrateDefMap> getDirectDependenciesDefMaps() {
        return directDependenciesDefMaps;
    }

    @Nonnull
    public CrateMetaData getMetaData() {
        return metaData;
    }

    public int getRootModMacroIndex() {
        return rootModMacroIndex;
    }

    @Nonnull
    public RsFile.Attributes getStdlibAttributes() {
        return stdlibAttributes;
    }

    public int getRecursionLimitRaw() {
        return recursionLimitRaw;
    }

    public int getRecursionLimit() {
        return Math.min(recursionLimitRaw, org.rust.cargo.project.settings.RustAdvancedSettings.getMaximumRecursionLimit());
    }

    @Nonnull
    public String getCrateDescription() {
        return crateDescription;
    }

    @Nullable
    public ModData getPrelude() {
        return prelude;
    }

    public void setPrelude(@Nullable ModData prelude) {
        this.prelude = prelude;
    }

    @Nonnull
    public Map<String, CrateDefMap> getExternPrelude() {
        return externPrelude;
    }

    @Nonnull
    public Map<String, CrateDefMap> getExternCratesInRoot() {
        return externCratesInRoot;
    }

    @Nonnull
    public Map<Integer, FileInfo> getFileInfos() {
        return fileInfos;
    }

    @Nonnull
    public List<Path> getMissedFiles() {
        return missedFiles;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Nonnull
    public PerNs getRootAsPerNs() {
        return rootAsPerNs;
    }

    @Nullable
    public org.rust.lang.core.psi.ext.RsMod rootAsRsMod(@Nonnull consulo.project.Project project) {
        List<org.rust.lang.core.psi.ext.RsMod> mods = root.toRsMod(project);
        return mods.size() == 1 ? mods.get(0) : null;
    }

    @Nonnull
    public GlobImportGraph getGlobImportGraph() {
        return globImportGraph;
    }

    @Nonnull
    public Map<String, MacroCallLightInfo> getExpansionNameToMacroCall() {
        return expansionNameToMacroCall;
    }

    @Nonnull
    public Map<MacroIndex, String> getMacroCallToExpansionName() {
        return macroCallToExpansionName;
    }

    public boolean isAtLeastEdition2018() {
        return isAtLeastEdition2018;
    }

    @Nullable
    public CrateDefMap getDefMap(int crateId) {
        if (crateId == this.crate) return this;
        return allDependenciesDefMaps.get(crateId);
    }

    @Nullable
    public ModData getModData(@Nonnull ModPath path) {
        return getModData(path, null);
    }

    @Nullable
    public ModData getModData(@Nonnull ModPath path, @Nullable ModData hangingModData) {
        if (hangingModData == null) {
            CrateDefMap defMap = getDefMap(path.getCrate());
            if (defMap == null) throw new IllegalStateException("Can't find ModData for path " + path);
            return defMap.root.getChildModData(path.getSegments());
        } else {
            ModData result = HangingModData.findHangingModData(path, hangingModData);
            if (result != null) return result;
            return getModData(path, hangingModData.getContext());
        }
    }

    @Nullable
    public ModData tryCastToModData(@Nonnull VisItem types) {
        return tryCastToModData(types, null);
    }

    @Nullable
    public ModData tryCastToModData(@Nonnull VisItem types, @Nullable ModData hangingModData) {
        if (!types.isModOrEnum()) return null;
        return getModData(types.getPath(), hangingModData);
    }

    @Nullable
    public ModData getModData(@Nonnull org.rust.lang.core.psi.ext.RsMod mod) {
        if (mod instanceof RsFile) {
            RsFile rsFile = (RsFile) mod;
            consulo.virtualFileSystem.VirtualFile virtualFile = rsFile.getOriginalFile().getVirtualFile();
            if (virtualFile == null) return null;
            if (!(virtualFile instanceof VirtualFileWithId)) return null;
            FileInfo fileInfo = fileInfos.get(VirtualFileExtUtil.getFileId(virtualFile));
            return fileInfo != null ? fileInfo.getModData() : null;
        }
        org.rust.lang.core.psi.ext.RsMod parentMod = org.rust.lang.core.psi.ext.RsModExtUtil.getSuper(mod);
        if (parentMod == null) return null;
        ModData parentModData = getModData(parentMod);
        if (parentModData == null) return null;
        return parentModData.getChildModules().get(org.rust.lang.core.psi.ext.RsModUtil.getModName(mod));
    }

    @Nullable
    public MacroDefInfo getMacroInfo(@Nonnull VisItem macroDef) {
        CrateDefMap defMap = getDefMap(macroDef.getCrate());
        if (defMap == null) return null;
        return defMap.doGetMacroInfo(macroDef);
    }

    @Nullable
    private MacroDefInfo doGetMacroInfo(@Nonnull VisItem macroDef) {
        ModData containingMod = getModData(macroDef.getContainingMod());
        if (containingMod == null) return null;
        RsProcMacroKind procMacroKind = containingMod.getProcMacros().get(macroDef.getName());
        if (procMacroKind != null) {
            KnownProcMacroKind knownKind = (KnownProcMacroKind) (Object) org.rust.lang.core.psi.HardcodedProcMacroProperties.getHardcodeProcMacroProperties(metaData.getName(), macroDef.getName());
            return new ProcMacroDefInfo(containingMod.getCrate(), macroDef.getPath(), procMacroKind, metaData.getProcMacroArtifact(), knownKind);
        }
        DeclMacro2DefInfo macro2 = containingMod.getMacros2().get(macroDef.getName());
        if (macro2 != null) {
            return macro2;
        }
        List<MacroDefInfo> macroInfos = containingMod.getLegacyMacros().get(macroDef.getName());
        if (macroInfos == null) throw new IllegalStateException("Can't find definition for macro " + macroDef);
        return (MacroDefInfo) PathResolutionUtil.singlePublicOrFirstDefInfo(
            filterDeclMacroDefInfos(macroInfos)
        );
    }

    @Nonnull
    private static List<DeclMacroDefInfo> filterDeclMacroDefInfos(@Nonnull List<MacroDefInfo> macroInfos) {
        List<DeclMacroDefInfo> result = new ArrayList<>();
        for (MacroDefInfo info : macroInfos) {
            if (info instanceof DeclMacroDefInfo) {
                result.add((DeclMacroDefInfo) info);
            }
        }
        return result;
    }

    public void importAllMacrosExported(@Nonnull CrateDefMap from) {
        for (Map.Entry<String, PerNs> entry : from.root.getVisibleItems().entrySet()) {
            String name = entry.getKey();
            PerNs def = entry.getValue();
            for (VisItem macroDef : def.getMacros()) {
                MacroDefInfo macroInfo = from.getMacroInfo(macroDef);
                if (macroInfo == null) continue;
                root.addLegacyMacro(name, macroInfo);
            }
        }
    }

    public void addVisitedFile(@Nonnull RsFile file, @Nonnull ModData modData, @Nonnull HashCode fileHash, @Nullable MacroIndex includeMacroIndex) {
        int fileId = VirtualFileExtUtil.getFileId(file.getVirtualFile());
        FileInfo existing = fileInfos.get(fileId);
        if (existing != null && !modData.isDeeplyEnabledByCfg() && existing.getModData().isDeeplyEnabledByCfg()) return;
        fileInfos.put(fileId, new FileInfo(file.getViewProvider().getModificationStamp(), modData, fileHash, includeMacroIndex));
    }

    @Override
    @Nonnull
    public String toString() {
        return crateDescription;
    }

    public static final Logger RESOLVE_LOG = Logger.getInstance("org.rust.resolve2");
}
