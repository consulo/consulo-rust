/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.psi.PsiAnchor;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.RsConstants;
import org.rust.lang.RsFileType;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.macros.MacroCallBody;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.NameResolution;
import org.rust.lang.core.resolve2.util.DollarCrateHelper;
import org.rust.lang.core.resolve2.util.DollarCrateMap;
import org.rust.lang.core.resolve2.util.RsBlockStubBuilder;
import org.rust.lang.core.stubs.*;
import org.rust.openapiext.VirtualFileExtUtil;
import org.rust.lang.utils.evaluation.ThreeValuedLogic;
import org.rust.stdext.HashCode;

import java.nio.file.InvalidPathException;
import java.util.*;
// import org.rust.lang.core.stubs.RsBlockStubBuilderUtil; // placeholder removed

/**
 * Collects explicitly declared items in all modules of a crate.
 */
public final class ModCollector {

    private ModCollector() {}

    /**
     * Collects items from a scope and returns legacy macros.
     */
    @NotNull
    public static Map<String, DeclMacroDefInfo> collectScope(
        @NotNull RsItemsOwner scope,
        @NotNull ModData modData,
        @NotNull ModCollectorContext context
    ) {
        return collectScope(scope, modData, context, modData.getMacroIndex(), null, null, false);
    }

    @NotNull
    public static Map<String, DeclMacroDefInfo> collectScope(
        @NotNull RsItemsOwner scope,
        @NotNull ModData modData,
        @NotNull ModCollectorContext context,
        @NotNull MacroIndex modMacroIndex,
        @Nullable DollarCrateHelper dollarCrateHelper,
        @Nullable MacroIndex includeMacroIndex,
        boolean propagateLegacyMacros
    ) {
        FileModificationTracker.HashCalculator hashCalculator = modData.isNormalCrate()
            ? new FileModificationTracker.HashCalculator(modData.isEnabledByCfgInner())
            : null;

        VirtualFile includeMacroFile = null;
        if (includeMacroIndex != null) {
            includeMacroFile = ((RsFile) scope).getVirtualFile();
        }

        ModCollectorImpl collector = new ModCollectorImpl(
            modData, context, modMacroIndex, hashCalculator, dollarCrateHelper, includeMacroFile
        );
        StubElement<?> stub = getOrBuildStub(scope);
        if (stub == null) return Collections.emptyMap();
        collector.collectMod(stub, propagateLegacyMacros);

        if (hashCalculator != null && scope instanceof RsFile) {
            HashCode fileHash = hashCalculator.getFileHash();
            context.getDefMap().addVisitedFile((RsFile) scope, modData, fileHash, includeMacroIndex);
        }

        return collector.legacyMacros;
    }

    /**
     * Collects expanded elements from a macro expansion.
     */
    public static void collectExpandedElements(
        @NotNull StubElement<? extends RsItemsOwner> scope,
        @NotNull MacroCallInfo call,
        @NotNull ModCollectorContext context,
        @Nullable DollarCrateHelper dollarCrateHelper
    ) {
        ModCollectorImpl collector = new ModCollectorImpl(
            call.getContainingMod(),
            context,
            call.getMacroIndex(),
            null, // hashCalculator
            dollarCrateHelper,
            null  // includeMacroFile
        );
        collector.collectMod(scope, true);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static StubElement<? extends RsItemsOwner> getOrBuildStub(@NotNull RsItemsOwner owner) {
        if (owner instanceof RsFileBase) return ModCollector.getOrBuildFileStub((RsFileBase) owner);
        if (owner instanceof StubBasedPsiElement) {
            StubElement<?> greenStub = ((StubBasedPsiElement<?>) owner).getStub();
            if (greenStub != null) return (StubElement<? extends RsItemsOwner>) greenStub;
        }
        if (owner instanceof RsBlock) return RsBlockStubBuilder.buildStub((RsBlock) owner);
        if (owner instanceof RsModItem) {
            RsFile containingFile = (RsFile) owner.getContainingFile();
            if (containingFile == null) return null;
            int stubIndex = PsiAnchor.calcStubIndex((StubBasedPsiElement<?>) owner);
            if (stubIndex == -1) return null;
            return (StubElement<? extends RsItemsOwner>) containingFile.calcStubTree().getPlainList().get(stubIndex);
        }
        return null;
    }

    /**
     * Gets or builds file stub for an RsFileBase.
     */
    @Nullable
    public static RsFileStub getOrBuildFileStub(@NotNull RsFileBase file) {
        Object greenStubTree = file.getGreenStubTree();
        StubElement<?> stub;
        if (greenStubTree != null) {
            stub = ((com.intellij.psi.stubs.StubTree) greenStubTree).getRoot();
        } else if (file instanceof RsFile) {
            stub = ((RsFile) file).calcStubTree().getRoot();
        } else {
            stub = RsFileStub.Type.getBuilder().buildStubTree(file);
        }
        if (!(stub instanceof RsFileStub)) {
            CrateDefMap.RESOLVE_LOG.error("No stub for file " + file.getViewProvider().getVirtualFile().getPath());
            return null;
        }
        return (RsFileStub) stub;
    }

    /**
     * Imports extern crate macros.
     * Extension function on CrateDefMap.
     */
    public static void importExternCrateMacros(@NotNull CrateDefMap defMap, @NotNull String externCrateName) {
        CrateDefMap externCrateDefMap = PathResolutionUtil.resolveExternCrateAsDefMap(defMap, externCrateName);
        if (externCrateDefMap == null) return;
        defMap.importAllMacrosExported(externCrateDefMap);
    }

    /**
     * Records a child file in an unusual location for parent mod data.
     */
    public static void recordChildFileInUnusualLocation(@NotNull ModData parent, int childFileId) {
        PersistentFS persistentFS = PersistentFS.getInstance();
        VirtualFile childFile = persistentFS.findFileById(childFileId);
        if (childFile == null) return;
        VirtualFile childDirectory = childFile.getParent();
        if (childDirectory == null) return;
        for (ModData modData : parent.getParentsIterable()) {
            Integer containedDirId = modData.getDirectoryContainedAllChildFiles();
            if (containedDirId == null) continue;
            VirtualFile containedDirectory = persistentFS.findFileById(containedDirId);
            if (containedDirectory == null) continue;
            if (VfsUtil.isAncestor(containedDirectory, childDirectory, false)) return;
            VirtualFile commonAncestor = VfsUtil.getCommonAncestor(containedDirectory, childDirectory);
            if (commonAncestor == null) continue;
            modData.setDirectoryContainedAllChildFiles(VirtualFileExtUtil.getFileId(commonAncestor));
        }
    }

    private static class ModCollectorImpl implements ModVisitor {
        @NotNull private final ModData modData;
        @NotNull private final ModCollectorContext context;
        @NotNull private final MacroIndex parentMacroIndex;
        @Nullable private final FileModificationTracker.HashCalculator hashCalculator;
        @Nullable private final DollarCrateHelper dollarCrateHelper;
        @Nullable private final VirtualFile includeMacroFile;

        @NotNull private final CrateDefMap defMap;
        private final int macroDepth;
        @NotNull private final Crate crate;
        @NotNull private final Project project;

        @NotNull final Map<String, DeclMacroDefInfo> legacyMacros = new HashMap<>();

        ModCollectorImpl(
            @NotNull ModData modData,
            @NotNull ModCollectorContext context,
            @NotNull MacroIndex parentMacroIndex,
            @Nullable FileModificationTracker.HashCalculator hashCalculator,
            @Nullable DollarCrateHelper dollarCrateHelper,
            @Nullable VirtualFile includeMacroFile
        ) {
            this.modData = modData;
            this.context = context;
            this.parentMacroIndex = parentMacroIndex;
            this.hashCalculator = hashCalculator;
            this.dollarCrateHelper = dollarCrateHelper;
            this.includeMacroFile = includeMacroFile;
            this.defMap = context.getDefMap();
            this.macroDepth = context.getMacroDepth();
            this.crate = context.getContext().getCrate();
            this.project = context.getContext().getProject();
        }

        void collectMod(@NotNull StubElement<?> mod, boolean propagateLegacyMacros) {
            ModVisitor visitor;
            if (hashCalculator != null) {
                RsFile.Attributes stdlibAttributes = defMap.getStdlibAttributes();
                if (!modData.isNormalCrate() || !modData.isCrateRoot()) stdlibAttributes = null;
                ModVisitor hashVisitor = hashCalculator.getVisitor(crate, modData.getFileRelativePath(), stdlibAttributes);
                visitor = new DelegatingModVisitor(hashVisitor, this);
            } else {
                visitor = this;
            }
            @SuppressWarnings("unchecked")
            StubElement<? extends RsElement> modAsRsElement = (StubElement<? extends RsElement>) mod;
            ModCollectorBase.collectMod(modAsRsElement, modData.isDeeplyEnabledByCfg(), visitor, crate);
            if (propagateLegacyMacros) propagateLegacyMacros(modData);
        }

        @Override
        public void collectImport(@NotNull ImportLight importItem) {
            String[] usePath = importItem.getUsePath();
            if (dollarCrateHelper != null && !importItem.isExternCrate()) {
                usePath = dollarCrateHelper.convertPath(usePath, importItem.getOffsetInExpansion());
            }
            Visibility visibility = convertVisibility(importItem.getVisibility(), importItem.isDeeplyEnabledByCfg());
            String nameInScope = importItem.getAlias() != null
                ? importItem.getAlias()
                : usePath[usePath.length - 1];
            context.getContext().getImports().add(new Import(
                modData,
                usePath,
                nameInScope,
                visibility,
                importItem.isGlob(),
                importItem.isExternCrate(),
                importItem.isPrelude()
            ));

            if (importItem.isDeeplyEnabledByCfg() && importItem.isExternCrate()
                && importItem.isMacroUse() && !context.isHangingMode()) {
                ModCollector.importExternCrateMacros(defMap, importItem.getUsePath()[0]);
            }
        }

        @Override
        public void collectSimpleItem(@NotNull SimpleItemLight item) {
            VisItem visItem = convertToVisItem(item, false, false);
            PerNs perNs = PerNs.from(visItem, item.getNamespaces());
            context.addItem(modData, item.getName(), perNs, visItem.getVisibility());

            if (item.getProcMacroKind() != null) {
                modData.getProcMacros().put(item.getName(), item.getProcMacroKind());
            }
        }

        @Override
        public void collectModOrEnumItem(@NotNull ModOrEnumItemLight item, @NotNull RsNamedStub stub) {
            ModData childModData = tryCollectChildModule(item, stub, item.getMacroIndexInParent());
            boolean forceCfgDisabledVisibility = childModData != null && !childModData.isEnabledByCfgInner();
            VisItem visItem = convertToVisItem(item, true, forceCfgDisabledVisibility);
            if (childModData != null) childModData.setAsVisItem(visItem);
            if (childModData == null) return;
            PerNs perNs = PerNs.types(visItem);
            boolean changed = context.addItem(modData, item.getName(), perNs, visItem.getVisibility());
            if (changed) {
                modData.getChildModules().put(item.getName(), childModData);
            }
        }

        @NotNull
        private VisItem convertToVisItem(@NotNull ItemLight item, boolean isModOrEnum, boolean forceCfgDisabledVisibility) {
            Visibility visibility = forceCfgDisabledVisibility
                ? Visibility.CFG_DISABLED
                : convertVisibility(item.getVisibility(), item.isDeeplyEnabledByCfg());
            ModPath itemPath = modData.getPath().append(item.getName());
            boolean isTrait = item instanceof SimpleItemLight && ((SimpleItemLight) item).isTrait();
            return new VisItem(itemPath, visibility, isModOrEnum, isTrait);
        }

        @Nullable
        private ModData tryCollectChildModule(@NotNull ModOrEnumItemLight item, @NotNull RsNamedStub stub, int index) {
            if (stub instanceof RsEnumItemStub) return collectEnumAsModData(item, (RsEnumItemStub) stub);

            VirtualFile parentOwnedDirectory = includeMacroFile != null
                ? includeMacroFile.getParent()
                : getOwnedDirectory(modData);

            if (stub instanceof RsModItemStub) {
                return collectChildModInline((RsModItemStub) stub, item, index, parentOwnedDirectory);
            }
            if (stub instanceof RsModDeclItemStub) {
                return collectChildModFile(item, (RsModDeclItemStub) stub, index, parentOwnedDirectory);
            }
            return null;
        }

        @Nullable
        private ModData collectChildModInline(
            @NotNull RsModItemStub modStub,
            @NotNull ModOrEnumItemLight item,
            int index,
            @Nullable VirtualFile parentOwnedDirectory
        ) {
            ProgressManager.checkCanceled();
            String name = item.getName();
            ModPath childModPath = modData.getPath().append(name);
            String fileRelativePath = modData.getFileRelativePath() + "::" + name;
            VirtualFile ownedDir = getOwnedDirectoryForInlineMod(parentOwnedDirectory, item.getPathAttribute(), name);
            ModData childModData = new ModData(
                modData,
                modData.getCrate(),
                childModPath,
                parentMacroIndex.append(index),
                item.isDeeplyEnabledByCfg(),
                true, // isEnabledByCfgInner - inline mods always enabled
                modData.getFileId(),
                fileRelativePath,
                ownedDir != null ? VirtualFileExtUtil.getFileId(ownedDir) : null,
                item.getPathAttribute() != null,
                item.isHasMacroUse(),
                defMap.getCrateDescription()
            );
            // Propagate parent legacy macros
            for (Map.Entry<String, List<MacroDefInfo>> entry : modData.getLegacyMacros().entrySet()) {
                childModData.getLegacyMacros().put(entry.getKey(), entry.getValue());
            }
            ModCollectorImpl childCollector = new ModCollectorImpl(
                childModData, context, childModData.getMacroIndex(), hashCalculator, dollarCrateHelper, includeMacroFile
            );
            childCollector.collectMod(modStub, false);
            if (item.isHasMacroUse() && childModData.isDeeplyEnabledByCfg()) {
                modData.addLegacyMacros(childCollector.legacyMacros);
                legacyMacros.putAll(childCollector.legacyMacros);
            }
            return childModData;
        }

        @Nullable
        private ModData collectChildModFile(
            @NotNull ModOrEnumItemLight item,
            @NotNull RsModDeclItemStub stub,
            int index,
            @Nullable VirtualFile parentOwnedDirectory
        ) {
            RsFile childModFile = resolveModDecl(item.getName(), item.getPathAttribute(), parentOwnedDirectory);
            if (childModFile == null) return null;

            ProgressManager.checkCanceled();
            String name = item.getName();
            ModPath childModPath = modData.getPath().append(name);
            boolean isEnabledByCfgInner = RsElementUtil.isEnabledByCfgSelf(childModFile, crate);
            boolean hasMacroUse = item.isHasMacroUse() || childModFile.hasMacroUseInner(crate);
            VirtualFile ownedDir = getOwnedDirectoryForFileMod(childModFile, name, item.getPathAttribute(), parentOwnedDirectory);

            ModData childModData = new ModData(
                modData,
                modData.getCrate(),
                childModPath,
                parentMacroIndex.append(index),
                item.isDeeplyEnabledByCfg(),
                isEnabledByCfgInner,
                VirtualFileExtUtil.getFileId(childModFile.getVirtualFile()),
                "",
                ownedDir != null ? VirtualFileExtUtil.getFileId(ownedDir) : null,
                item.getPathAttribute() != null,
                hasMacroUse,
                defMap.getCrateDescription()
            );
            for (Map.Entry<String, List<MacroDefInfo>> entry : modData.getLegacyMacros().entrySet()) {
                childModData.getLegacyMacros().put(entry.getKey(), entry.getValue());
            }
            Map<String, DeclMacroDefInfo> childLegacyMacros = collectScope(childModFile, childModData, context);
            if (hasMacroUse && childModData.isDeeplyEnabledByCfg()) {
                modData.addLegacyMacros(childLegacyMacros);
                legacyMacros.putAll(childLegacyMacros);
            }
            if (childModData.isRsFile() && childModData.getHasPathAttributeRelativeToParentFile()
                && childModData.getFileId() != null) {
                recordChildFileInUnusualLocation(modData, childModData.getFileId());
            }
            return childModData;
        }

        @NotNull
        private ModData collectEnumAsModData(@NotNull ModOrEnumItemLight enumItem, @NotNull RsEnumItemStub enumStub) {
            String enumName = enumItem.getName();
            ModPath enumPath = modData.getPath().append(enumName);
            ModData enumData = new ModData(
                modData,
                modData.getCrate(),
                enumPath,
                new MacroIndex(new int[0]),
                enumItem.isDeeplyEnabledByCfg(),
                true,
                modData.getFileId(),
                modData.getFileRelativePath() + "::" + enumName,
                modData.getOwnedDirectoryId(),
                modData.isHasPathAttribute(),
                false, // hasMacroUse
                true,  // isEnum
                true,  // isNormalCrate (inherit from defMap context)
                null,  // context
                false, // isBlock
                defMap.getCrateDescription()
            );
            for (Object variant : RsEnumItemUtil.getVariants(enumStub)) {
                if (!(variant instanceof RsNamedStub)) continue;
                RsNamedStub variantStub = (RsNamedStub) variant;
                String variantName = variantStub.getName();
                if (variantName == null) continue;
                ModPath variantPath = enumPath.append(variantName);
                boolean isVariantEnabled = enumData.isDeeplyEnabledByCfg()
                    && RsDocAndAttributeOwnerUtil.evaluateCfg((RsAttributeOwnerStub) variantStub, crate) != ThreeValuedLogic.False;
                Visibility variantVisibility = isVariantEnabled ? Visibility.PUBLIC : Visibility.CFG_DISABLED;
                VisItem variantVis = new VisItem(variantPath, variantVisibility);
                PerNs variantPerNs = PerNs.from(variantVis, NameResolution.getENUM_VARIANT_NS());
                enumData.addVisibleItem(variantName, variantPerNs);
            }
            return enumData;
        }

        @Override
        public void collectMacroCall(@NotNull MacroCallLight call, @NotNull RsMacroCallStub stub) {
            if (!modData.isDeeplyEnabledByCfg()) {
                throw new IllegalStateException("for performance reasons cfg-disabled macros should not be collected");
            }
            HashCode bodyHash = call.getBodyHash();
            if (bodyHash == null && !call.getPath()[call.getPath().length - 1].equals("include")) return;
            String[] path = dollarCrateHelper != null
                ? dollarCrateHelper.convertPath(call.getPath(), call.getPathOffsetInExpansion())
                : call.getPath();
            DollarCrateMap dollarCrateMap = dollarCrateHelper != null
                ? dollarCrateHelper.getDollarCrateMap(call.getBodyStartOffsetInExpansion(), call.getBodyEndOffsetInExpansion())
                : DollarCrateMap.EMPTY;
            MacroIndex macroIndex = parentMacroIndex.append(call.getMacroIndexInParent());
            Integer containingFileId = includeMacroFile != null
                ? VirtualFileExtUtil.getFileId(includeMacroFile)
                : modData.getFileId();
            context.getContext().getMacroCalls().add(new MacroCallInfo(
                modData,
                macroIndex,
                path,
                call.getBody(),
                bodyHash,
                containingFileId,
                macroDepth,
                dollarCrateMap
            ));
        }

        @Override
        public void collectProcMacroCall(@NotNull ProcMacroCallLight call) {
            if (!modData.isDeeplyEnabledByCfg()) {
                throw new IllegalStateException("for performance reasons cfg-disabled macros should not be collected");
            }
            // Simplified: proc macro call collection
        }

        @Override
        public void collectMacroDef(@NotNull MacroDefLight def) {
            String bodyHashStr = def.getBodyHash();
            if (bodyHashStr == null || bodyHashStr.isEmpty()) return;
            HashCode bodyHash = HashCode.compute(bodyHashStr);
            ModPath macroPath = modData.getPath().append(def.getName());
            MacroIndex macroIndex = parentMacroIndex.append(def.getMacroIndexInParent());

            DeclMacroDefInfo defInfo = new DeclMacroDefInfo(
                modData.getCrate(),
                macroPath,
                macroIndex,
                def.getBody(),
                bodyHash,
                def.isHasMacroExport(),
                def.isHasLocalInnerMacros(),
                def.isHasRustcBuiltinMacro(),
                project
            );
            modData.addLegacyMacro(def.getName(), defInfo);
            legacyMacros.put(def.getName(), defInfo);

            if (def.isHasMacroExport() && !context.isHangingMode()) {
                Visibility visibility = Visibility.PUBLIC;
                VisItem visItem = new VisItem(macroPath, visibility);
                PerNs perNs = PerNs.macros(visItem);
                context.addItem(defMap.getRoot(), def.getName(), perNs, visibility);
            }
        }

        @Override
        public void collectMacro2Def(@NotNull Macro2DefLight def) {
            modData.getMacros2().put(def.getName(), new DeclMacro2DefInfo(
                modData.getCrate(),
                modData.getPath().append(def.getName()),
                def.getBody(),
                HashCode.compute(def.getBodyHash()),
                def.isHasRustcBuiltinMacro(),
                project
            ));

            Visibility visibility = convertVisibility(def.getVisibility(), true);
            VisItem visItem = new VisItem(modData.getPath().append(def.getName()), visibility);
            PerNs perNs = PerNs.macros(visItem);
            context.addItem(modData, def.getName(), perNs, visibility);
        }

        private void propagateLegacyMacros(@NotNull ModData modData) {
            if (legacyMacros.isEmpty()) return;
            for (ModData childMod : modData.getChildModules().values()) {
                if (!childMod.isEnum() && MacroIndex.shouldPropagate(parentMacroIndex, childMod.getMacroIndex())) {
                    childMod.visitDescendants(it -> {
                        it.addLegacyMacros(legacyMacros);
                    });
                }
            }
            if (modData.isHasMacroUse()) {
                ModData parent = modData.getParent();
                if (parent == null) return;
                parent.addLegacyMacros(legacyMacros);
                propagateLegacyMacros(parent);
            }
        }

        @NotNull
        private Visibility convertVisibility(@NotNull VisibilityLight visibility, boolean isDeeplyEnabledByCfg) {
            if (!isDeeplyEnabledByCfg) return Visibility.CFG_DISABLED;
            if (visibility.getKind() == VisibilityLight.Kind.PUB) return Visibility.PUBLIC;
            if (visibility.getKind() == VisibilityLight.Kind.PUB_CRATE) return defMap.getRoot().getVisibilityInSelf();
            if (visibility.getKind() == VisibilityLight.Kind.PRIV) return modData.getVisibilityInSelf();
            if (visibility.getKind() == VisibilityLight.Kind.RESTRICTED && visibility.getRestrictedPath() != null) {
                String[] inPath = visibility.getRestrictedPath();
                Visibility.Restricted resolved = resolveRestrictedVisibility(inPath, modData);
                return resolved != null ? resolved : defMap.getRoot().getVisibilityInSelf();
            }
            return Visibility.PUBLIC;
        }

        @Nullable
        private RsFile resolveModDecl(
            @NotNull String name,
            @Nullable String pathAttribute,
            @Nullable VirtualFile parentOwnedDirectory
        ) {
            VirtualFile parentDirectory;
            String[] fileNames;
            if (pathAttribute == null) {
                if (parentOwnedDirectory == null) return null;
                parentDirectory = parentOwnedDirectory;
                fileNames = new String[]{name + ".rs", name + "/mod.rs"};
            } else {
                if (modData.isRsFile()) {
                    VirtualFile containingFile = asVirtualFile(modData);
                    parentDirectory = containingFile != null ? containingFile.getParent() : null;
                } else {
                    parentDirectory = parentOwnedDirectory;
                }
                if (parentDirectory == null) return null;
                String explicitPath = FileUtil.toSystemIndependentName(pathAttribute);
                fileNames = new String[]{explicitPath};
            }

            List<VirtualFile> virtualFiles = new ArrayList<>();
            for (String fileName : fileNames) {
                VirtualFile f = VirtualFileExtUtil.findFileByMaybeRelativePath(parentDirectory, fileName);
                if (f != null) virtualFiles.add(f);
            }
            if (virtualFiles.isEmpty() && !context.isHangingMode()) {
                for (String fileName : fileNames) {
                    try {
                        java.nio.file.Path path = VirtualFileExtUtil.getPathAsPath(parentDirectory).resolve(fileName);
                        defMap.getMissedFiles().add(path);
                    } catch (InvalidPathException ignored) {
                    }
                }
            }
            if (virtualFiles.size() != 1) return null;
            return VirtualFileExtUtil.toPsiFile(virtualFiles.get(0), project) instanceof RsFile
                ? (RsFile) VirtualFileExtUtil.toPsiFile(virtualFiles.get(0), project) : null;
        }
    }

    @Nullable
    private static Visibility.Restricted resolveRestrictedVisibility(@NotNull String[] path, @NotNull ModData containingMod) {
        boolean allSuper = true;
        for (String segment : path) {
            if (!"super".equals(segment)) { allSuper = false; break; }
        }
        if (allSuper) {
            ModData modData = containingMod.getNthParent(path.length);
            return modData != null ? modData.getVisibilityInSelf() : null;
        }
        List<ModData> parents = new ArrayList<>();
        for (ModData p : containingMod.getParentsIterable()) {
            parents.add(p);
        }
        Collections.reverse(parents);
        if (path.length < parents.size()) {
            ModData target = parents.get(path.length);
            if (Arrays.equals(path, target.getPath().getSegments())) {
                return target.getVisibilityInSelf();
            }
        }
        return null;
    }

    @Nullable
    private static VirtualFile getOwnedDirectory(@NotNull ModData modData) {
        Integer ownedDirectoryId = modData.getOwnedDirectoryId();
        if (ownedDirectoryId == null) return null;
        return PersistentFS.getInstance().findFileById(ownedDirectoryId);
    }

    @Nullable
    private static VirtualFile asVirtualFile(@NotNull ModData modData) {
        if (!modData.isRsFile()) throw new IllegalStateException("Not an RsFile");
        Integer fileId = modData.getFileId();
        if (fileId == null) return null;
        VirtualFile f = PersistentFS.getInstance().findFileById(fileId);
        if (f == null) {
            CrateDefMap.RESOLVE_LOG.error("Can't find VirtualFile for " + modData);
        }
        return f;
    }

    @Nullable
    private static VirtualFile getOwnedDirectoryForInlineMod(
        @Nullable VirtualFile parentOwnedDirectory,
        @Nullable String pathAttribute,
        @NotNull String name
    ) {
        if (pathAttribute != null) {
            if (parentOwnedDirectory == null) return null;
            String directoryPath = FileUtil.toSystemIndependentName(pathAttribute)
                .replaceAll("\\." + RsFileType.INSTANCE.getDefaultExtension() + "$", "");
            return VirtualFileExtUtil.findFileByMaybeRelativePath(parentOwnedDirectory, directoryPath);
        }
        if (parentOwnedDirectory == null) return null;
        return VirtualFileExtUtil.findFileByMaybeRelativePath(parentOwnedDirectory, name);
    }

    @Nullable
    private static VirtualFile getOwnedDirectoryForFileMod(
        @NotNull RsFile file,
        @NotNull String name,
        @Nullable String pathAttribute,
        @Nullable VirtualFile parentOwnedDirectory
    ) {
        if (RsConstants.MOD_RS_FILE.equals(name)) {
            return file.getVirtualFile().getParent();
        }
        if (pathAttribute != null) {
            return file.getVirtualFile().getParent();
        }
        if (parentOwnedDirectory == null) return null;
        return VirtualFileExtUtil.findFileByMaybeRelativePath(parentOwnedDirectory, name);
    }

    /**
     * A ModVisitor that delegates all calls to two visitors in sequence.
     */
    private static class DelegatingModVisitor implements ModVisitor {
        @NotNull private final ModVisitor first;
        @NotNull private final ModVisitor second;

        DelegatingModVisitor(@NotNull ModVisitor first, @NotNull ModVisitor second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public void collectSimpleItem(@NotNull SimpleItemLight item) {
            first.collectSimpleItem(item);
            second.collectSimpleItem(item);
        }

        @Override
        public void collectModOrEnumItem(@NotNull ModOrEnumItemLight item, @NotNull RsNamedStub stub) {
            first.collectModOrEnumItem(item, stub);
            second.collectModOrEnumItem(item, stub);
        }

        @Override
        public void collectImport(@NotNull ImportLight importItem) {
            first.collectImport(importItem);
            second.collectImport(importItem);
        }

        @Override
        public void collectMacroCall(@NotNull MacroCallLight call, @NotNull RsMacroCallStub stub) {
            first.collectMacroCall(call, stub);
            second.collectMacroCall(call, stub);
        }

        @Override
        public void collectProcMacroCall(@NotNull ProcMacroCallLight call) {
            first.collectProcMacroCall(call);
            second.collectProcMacroCall(call);
        }

        @Override
        public void collectMacroDef(@NotNull MacroDefLight def) {
            first.collectMacroDef(def);
            second.collectMacroDef(def);
        }

        @Override
        public void collectMacro2Def(@NotNull Macro2DefLight def) {
            first.collectMacro2Def(def);
            second.collectMacro2Def(def);
        }

        @Override
        public void afterCollectMod() {
            first.afterCollectMod();
            second.afterCollectMod();
        }
    }
}
