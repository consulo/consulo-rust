/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.util.AutoInjectedCrates;
import org.rust.ide.injected.RsDoctestLanguageInjector;
import org.rust.lang.RsConstants;
import org.rust.lang.RsFileType;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.completion.Utils;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.crate.CrateGraphService;
import org.rust.lang.core.crate.impl.DoctestCrate;
import org.rust.lang.core.crate.impl.FakeDetachedCrate;
import org.rust.lang.core.crate.impl.FakeInvalidCrate;
import org.rust.lang.core.macros.MacroExpansionFileSystem;
import org.rust.lang.core.macros.MacroExpansionManagerUtil;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.NameResolution;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.resolve2.DefMapService;
import org.rust.lang.core.resolve2.FacadeResolve;
import org.rust.lang.core.resolve2.FacadeResolve.FileInclusionPoint;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.stubs.index.RsModulesIndex;
import org.rust.openapiext.OpenApiUtil;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;

/**
 * Represents a Rust source file in the PSI tree.
 */
public class RsFile extends RsFileBase implements RsMod {

    private volatile Supplier<CachedData> forcedCachedData;
    private boolean hasForcedStubTree = false;

    public RsFile(@NotNull FileViewProvider fileViewProvider) {
        super(fileViewProvider);
    }

    @NotNull
    @Override
    public RsMod getContainingMod() {
        return Utils.getOriginalOrSelf(this);
    }

    @Nullable
    public CargoProject getCargoProject() {
        return getCachedData().cargoProject;
    }

    @Nullable
    public CargoWorkspace getCargoWorkspace() {
        return getCachedData().cargoWorkspace;
    }

    @NotNull
    public Crate getCrate() {
        return getCachedData().crate;
    }

    @NotNull
    public List<Crate> getCrates() {
        return getCachedData().crates;
    }

    @Nullable
    @Override
    public RsMod getCrateRoot() {
        return getCachedData().crateRoot;
    }

    public boolean isDeeplyEnabledByCfg() {
        return getCachedData().isDeeplyEnabledByCfg;
    }

    public boolean isIncludedByIncludeMacro() {
        return getCachedData().isIncludedByIncludeMacro;
    }

    public int getMacroExpansionDepth() {
        return getCachedData().macroExpansionDepth;
    }

    @NotNull
    private CachedData getCachedData() {
        Supplier<CachedData> forced = forcedCachedData;
        if (forced != null) return forced.get();

        PsiFile originalFile = getOriginalFile();
        if (originalFile != this) {
            if (originalFile instanceof RsFile) {
                return ((RsFile) originalFile).getCachedData();
            }
            return new CachedData(null, null, null, new FakeInvalidCrate(getProject()),
                Collections.emptyList(), true, false, 0);
        }

        return CachedValuesManager.getManager(getProject()).getCachedValue(this, CACHED_DATA_KEY, () -> {
            CachedData value = doGetCachedData();
            Object modificationTracker;
            if (getVirtualFile() instanceof VirtualFileWindow) {
                modificationTracker = PsiModificationTracker.MODIFICATION_COUNT;
            } else if (value.crate.getOrigin() == PackageOrigin.WORKSPACE) {
                modificationTracker = RsPsiManagerUtil.getRustStructureModificationTracker(getProject());
            } else {
                modificationTracker = RsPsiManagerUtil.getRustPsiManager(getProject())
                    .getRustStructureModificationTrackerInDependencies();
            }
            return CachedValueProvider.Result.create(value, modificationTracker);
        }, false);
    }

    @NotNull
    private CachedData doGetCachedData() {
        VirtualFile virtualFile = getVirtualFile();
        if (virtualFile == null) {
            return new CachedData(null, null, null,
                new FakeDetachedCrate(this, -1, Collections.emptyList()),
                Collections.emptyList(), true, false, 0);
        }

        if (virtualFile.getFileSystem() instanceof MacroExpansionFileSystem) {
            Object crateId = MacroExpansionManagerUtil.getMacroExpansionManager(getProject())
                .getCrateForExpansionFile(virtualFile);
            if (crateId == null) {
                return new CachedData(null, null, null, new FakeInvalidCrate(getProject()),
                    Collections.emptyList(), true, false, 0);
            }
            Crate crate = CrateGraphService.crateGraph(getProject()).findCrateById(((Number) crateId).intValue());
            if (crate == null) {
                return new CachedData(null, null, null, new FakeInvalidCrate(getProject()),
                    Collections.emptyList(), true, false, 0);
            }
            return new CachedData(
                crate.getCargoProject(),
                crate.getCargoWorkspace(),
                crate.getRootMod(),
                crate,
                Collections.singletonList(crate),
                true, false, 0
            );
        }

        List<FileInclusionPoint> allInclusionPoints = FacadeResolve.findFileInclusionPointsFor(this);
        FileInclusionPoint inclusionPoint = pickSingleInclusionPoint(allInclusionPoints);
        if (inclusionPoint != null) {
            var crateGraph = CrateGraphService.crateGraph(getProject());
            List<Crate> crates = allInclusionPoints.stream()
                .map(p -> crateGraph.findCrateById(p.getModData().getCrate()))
                .filter(c -> c != null)
                .collect(Collectors.toList());
            Crate crate = crates.stream()
                .filter(c -> c.getId() == inclusionPoint.getModData().getCrate())
                .findFirst()
                .orElse(null);
            if (crate == null) {
                return new CachedData(null, null, null, new FakeInvalidCrate(getProject()),
                    Collections.emptyList(), true, false, 0);
            }
            return new CachedData(
                crate.getCargoProject(),
                crate.getCargoWorkspace(),
                crate.getRootMod(),
                crate,
                crates,
                inclusionPoint.getModData().isDeeplyEnabledByCfg(),
                inclusionPoint.getIncludeMacroIndex() != null,
                0
            );
        }

        RsFile injectedFromFile = getInjectedFromIfDoctestInjection(virtualFile, getProject());
        if (injectedFromFile != null) {
            CachedData cached = injectedFromFile.getCachedData();
            Crate doctestCrate = DoctestCrate.inCrate(cached.crate, this);
            return new CachedData(
                cached.cargoProject,
                cached.cargoWorkspace,
                this,
                doctestCrate,
                cached.crates,
                cached.isDeeplyEnabledByCfg,
                false,
                cached.macroExpansionDepth
            );
        }

        var stdlibCrates = CrateGraphService.crateGraph(getProject()).getTopSortedCrates().stream()
            .filter(c -> c.getOrigin() == PackageOrigin.STDLIB)
            .map(c -> new Crate.Dependency(c.getNormName(), c))
            .collect(Collectors.toList());
        Crate crate = new FakeDetachedCrate(this, DefMapService.getNextNonCargoCrateId(), stdlibCrates);

        CargoProject cargoProject = CargoProjectServiceUtil.getCargoProjects(getProject()).findProjectForFile(virtualFile);
        if (cargoProject == null) {
            return new CachedData(null, null, null, crate,
                Collections.emptyList(), true, false, 0);
        }
        CargoWorkspace workspace = cargoProject.getWorkspace();
        if (workspace == null) {
            return new CachedData(cargoProject, null, null, crate,
                Collections.emptyList(), true, false, 0);
        }
        return new CachedData(cargoProject, workspace, null, crate,
            Collections.emptyList(), true, false, 0);
    }

    public void inheritCachedDataFrom(@NotNull RsFile other, boolean isInMemoryMacroExpansion) {
        if (isInMemoryMacroExpansion) {
            CachedData otherCachedData = other.getCachedData();
            CachedData data = new CachedData(
                otherCachedData.cargoProject,
                otherCachedData.cargoWorkspace,
                otherCachedData.crateRoot,
                otherCachedData.crate,
                otherCachedData.crates,
                otherCachedData.isDeeplyEnabledByCfg,
                otherCachedData.isIncludedByIncludeMacro,
                otherCachedData.macroExpansionDepth + 1
            );
            forcedCachedData = () -> data;
        } else {
            forcedCachedData = other::getCachedData;
        }
    }

    @NotNull
    @Override
    public PsiElement setName(@NotNull String name) {
        if (getName().equals(RsConstants.MOD_RS_FILE)) return this;
        String nameWithExtension = name.indexOf('.') < 0 ? name + ".rs" : name;
        return super.setName(nameWithExtension);
    }

    @Nullable
    @Override
    public RsMod getSuper() {
        List<FileInclusionPoint> points = FacadeResolve.findFileInclusionPointsFor(this);
        FileInclusionPoint inclusionPoint = pickSingleInclusionPoint(points);
        if (inclusionPoint == null) return null;
        var modData = inclusionPoint.getModData();
        var parentModData = modData.getParent();
        if (parentModData == null) return null;
        var mods = parentModData.toRsMod(getProject());
        return mods.isEmpty() ? null : mods.get(0);
    }

    @Nullable
    @Override
    public String getModName() {
        RsModDeclItem decl = getDeclaration();
        if (decl != null) return decl.getName();
        if (!getName().equals(RsConstants.MOD_RS_FILE)) {
            return FileUtil.getNameWithoutExtension(getName());
        }
        PsiElement parent = getParent();
        return parent != null ? parent.toString() : null;
    }

    @Nullable
    @Override
    public String getPathAttribute() {
        RsModDeclItem decl = getDeclaration();
        return decl != null ? RsModDeclItemUtil.getPathAttribute(decl) : null;
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        return RsPsiImplUtil.modCrateRelativePath(this);
    }

    @Override
    public boolean getOwnsDirectory() {
        return getOwnedDirectory() != null;
    }

    @Override
    public boolean isCrateRoot() {
        PsiFile orig = getOriginalFile();
        VirtualFile file = orig.getVirtualFile();
        if (file == null) return false;
        return file instanceof VirtualFileWithId
            && CrateGraphService.crateGraph(getProject()).findCrateByRootMod(file) != null
            || RsDoctestLanguageInjector.isDoctestInjection(file, getProject());
    }

    @NotNull
    @Override
    public RsVisibility getVisibility() {
        if (isCrateRoot()) return RsVisibility.Public.INSTANCE;
        RsModDeclItem decl = getDeclaration();
        return decl != null ? RsVisibilityUtil.getVisibility(decl) : RsVisibility.Private.INSTANCE;
    }

    @Override
    public boolean isPublic() {
        if (isCrateRoot()) return true;
        RsModDeclItem decl = getDeclaration();
        return decl != null && RsVisibilityUtil.isPublic(decl);
    }

    @NotNull
    public Attributes getStdlibAttributes() {
        return getStdlibAttributes(null);
    }

    @NotNull
    public Attributes getStdlibAttributes(@Nullable Crate crate) {
        RsFileStub stub = (RsFileStub) getStub();
        if (stub != null && !stub.getMayHaveStdlibAttributes()) return Attributes.NONE;
        var attributes = RsAttrProcMacroOwnerUtil.getQueryAttributes(this, crate, stub);
        if (attributes.hasAtomAttribute("no_core")) return Attributes.NO_CORE;
        if (attributes.hasAtomAttribute("no_std")) return Attributes.NO_STD;
        return Attributes.NONE;
    }

    public boolean hasMacroUseInner(@Nullable Crate crate) {
        RsFileStub stub = (RsFileStub) getStub();
        if (stub != null && !stub.getMayHaveMacroUse()) return false;
        return RsAttrProcMacroOwnerUtil.getQueryAttributes(this, crate, stub).hasAtomAttribute("macro_use");
    }

    public int getRecursionLimit(@Nullable Crate crate) {
        RsFileStub stub = (RsFileStub) getStub();
        if (stub != null && !stub.getMayHaveRecursionLimitAttribute()) return NameResolution.DEFAULT_RECURSION_LIMIT;
        var attributes = RsAttrProcMacroOwnerUtil.getQueryAttributes(this, crate, stub);
        String recursionLimit = attributes.lookupStringValueForKey("recursion_limit");
        if (recursionLimit != null) {
            try {
                return Integer.parseInt(recursionLimit);
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return NameResolution.DEFAULT_RECURSION_LIMIT;
    }

    @Nullable
    public RsModDeclItem getDeclaration() {
        List<RsModDeclItem> decls = getDeclarations();
        return decls.isEmpty() ? null : decls.get(0);
    }

    @NotNull
    public List<RsModDeclItem> getDeclarations() {
        PsiFile originalFile = getOriginalFile();
        if (!(originalFile instanceof RsFile)) return Collections.emptyList();
        RsFile orig = (RsFile) originalFile;
        return CachedValuesManager.getManager(orig.getProject()).getCachedValue(orig, MOD_DECL_KEY, () -> {
            List<RsModDeclItem> decl;
            if (orig.isCrateRoot()) {
                decl = Collections.emptyList();
            } else {
                decl = RsModulesIndex.getDeclarationsFor(orig);
            }
            return CachedValueProvider.Result.create(decl,
                RsElementUtil.getRustStructureOrAnyPsiModificationTracker(orig));
        }, false);
    }

    public void forceSetStubTree(@NotNull PsiFileStub<?> stub) {
        assert getVirtualFile() instanceof LightVirtualFile;
        hasForcedStubTree = true;
        if (!RsPsiFileInternals.setStubTree(this, stub)) {
            hasForcedStubTree = false;
        }
    }

    @Nullable
    @Override
    public FileElement getTreeElement() {
        if (hasForcedStubTree && !isContentsLoaded()) {
            return null;
        }
        return super.getTreeElement();
    }

    // --- Attributes enum ---
    public enum Attributes {
        NO_CORE, NO_STD, NONE;

        @Nullable
        public String getAutoInjectedCrate() {
            switch (this) {
                case NONE:
                    return AutoInjectedCrates.STD;
                case NO_STD:
                    return AutoInjectedCrates.CORE;
                case NO_CORE:
                    return null;
            }
            return null;
        }

        public boolean canUseStdlibCrate(@NotNull String crateName) {
            switch (this) {
                case NONE:
                    return true;
                case NO_STD:
                    return !crateName.equals(AutoInjectedCrates.STD);
                case NO_CORE:
                    return !crateName.equals(AutoInjectedCrates.STD) && !crateName.equals(AutoInjectedCrates.CORE);
            }
            return true;
        }
    }

    // --- Static utility methods ---

    @Nullable
    private static FileInclusionPoint pickSingleInclusionPoint(@NotNull List<FileInclusionPoint> points) {
        if (points.isEmpty()) return null;
        if (points.size() == 1) return points.get(0);
        return points.stream()
            .min((a, b) -> Integer.compare(a.getModData().getCrate(), b.getModData().getCrate()))
            .orElse(null);
    }

    @Nullable
    private static RsFile getInjectedFromIfDoctestInjection(@NotNull VirtualFile file, @NotNull Project project) {
        if (!RsDoctestLanguageInjector.isDoctestInjection(file, project)) return null;
        if (!(file instanceof VirtualFileWindow)) return null;
        VirtualFile delegate = ((VirtualFileWindow) file).getDelegate();
        PsiFile psi = OpenApiUtil.toPsiFile(delegate, project);
        return psi instanceof RsFile ? (RsFile) psi : null;
    }

    @Nullable
    public static RsFile from(@NotNull PsiFile file) {
        return file instanceof RsFile ? (RsFile) file : null;
    }

    public static boolean isRustFile(@NotNull VirtualFile file) {
        return file.getFileType() == RsFileType.INSTANCE;
    }

    public static boolean isNotRustFile(@NotNull VirtualFile file) {
        return !isRustFile(file);
    }

    public static boolean shouldIndexFile(@NotNull Project project, @NotNull VirtualFile file) {
        ProjectFileIndex index = ProjectFileIndex.getInstance(project);
        return (index.isInContent(file) || index.isInLibrary(file))
            && !FileTypeManager.getInstance().isFileIgnored(file);
    }

    // --- Keys ---
    private static final Key<CachedValue<List<RsModDeclItem>>> MOD_DECL_KEY = Key.create("MOD_DECL_KEY");
    private static final Key<CachedValue<CachedData>> CACHED_DATA_KEY = Key.create("CACHED_DATA_KEY");

    // --- Internal data class ---
    private static final class CachedData {
        @Nullable final CargoProject cargoProject;
        @Nullable final CargoWorkspace cargoWorkspace;
        @Nullable final RsFile crateRoot;
        @NotNull final Crate crate;
        @NotNull final List<Crate> crates;
        final boolean isDeeplyEnabledByCfg;
        final boolean isIncludedByIncludeMacro;
        final int macroExpansionDepth;

        CachedData(
            @Nullable CargoProject cargoProject,
            @Nullable CargoWorkspace cargoWorkspace,
            @Nullable RsFile crateRoot,
            @NotNull Crate crate,
            @NotNull List<Crate> crates,
            boolean isDeeplyEnabledByCfg,
            boolean isIncludedByIncludeMacro,
            int macroExpansionDepth
        ) {
            this.cargoProject = cargoProject;
            this.cargoWorkspace = cargoWorkspace;
            this.crateRoot = crateRoot;
            this.crate = crate;
            this.crates = crates;
            this.isDeeplyEnabledByCfg = isDeeplyEnabledByCfg;
            this.isIncludedByIncludeMacro = isIncludedByIncludeMacro;
            this.macroExpansionDepth = macroExpansionDepth;
        }
    }
}
