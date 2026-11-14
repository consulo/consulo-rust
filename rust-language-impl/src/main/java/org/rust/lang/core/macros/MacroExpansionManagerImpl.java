/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.disposer.Disposable;
import java.util.List;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.application.util.CachedValueProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.rust.lang.core.macros.errors.GetMacroExpansionError;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;
import org.rust.stdext.RsResult;
import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Inject;
import consulo.component.util.ModificationTracker;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.crate.impl.FakeCrate;
import org.rust.lang.core.macros.errors.MacroExpansionError;
import org.rust.lang.core.macros.errors.ResolveMacroWithoutPsiError;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.impl.PsiElementUtil;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.psi.ext.impl.RsPossibleMacroCallUtil;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.impl.RsPsiManagerUtil;
import org.rust.lang.core.resolve2.FacadeResolve;
import org.rust.lang.core.psi.ext.impl.RsMacroCallUtil;
import org.rust.lang.core.macros.generated.RsGeneratedCode;
import org.rust.lang.core.macros.generated.RsGeneratedIncludes;
import org.rust.lang.core.psi.light.RsLightItems;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.language.psi.PsiManager;

/**
 * The main implementation of {@link MacroExpansionManager}.
 * <p>
 * A function-like macro call is expanded into an in-memory {@link RsFile} whose context is the call site, so the
 * expanded elements resolve as if written there. The expansion is cached on the call until the call or the
 * crate structure changes.
 */
@State(name = "MacroExpansionManager", storages = {
    @Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED),
    @Storage(value = "misc.xml", roamingType = RoamingType.DISABLED, deprecated = true)
})
@ServiceImpl
public class MacroExpansionManagerImpl
    implements MacroExpansionManager,
    PersistentStateComponent<MacroExpansionManagerImpl.PersistentState>,
    Disposable {

    @Nonnull
    private final Project myProject;

    /**
     * The default {@code #![recursion_limit]}: deeper nested expansions are not expanded.
     */
    private static final int RECURSION_LIMIT = 128;

    private static final Key<RsPossibleMacroCall> RS_EXPANSION_MACRO_CALL =
        Key.create("org.rust.lang.core.psi.RS_EXPANSION_MACRO_CALL");

    private volatile boolean isDisposed = false;

    @Inject

    public MacroExpansionManagerImpl(@Nonnull Project project) {
        myProject = project;
    }

    // --- PersistentState ---

    public static class PersistentState {
        @Nullable
        public String directoryName;

        public PersistentState() {
            this(null);
        }

        public PersistentState(@Nullable String directoryName) {
            this.directoryName = directoryName;
        }
    }

    @Nullable
    @Override
    public PersistentState getState() {
        return new PersistentState();
    }

    @Override
    public void loadState(@Nonnull PersistentState state) {
        // TODO: restore the expansion directory from the persisted state
    }

    public void noStateLoaded() {
        loadState(new PersistentState(null));
    }

    // --- MacroExpansionManager interface ---

    @Nullable
    @Override
    public VirtualFile getIndexableDirectory() {
        return null;
    }

    @Nonnull
    @Override
    public CachedValueProvider.Result<RsResult<MacroExpansion, GetMacroExpansionError>> getExpansionFor(@Nonnull RsPossibleMacroCall call) {
        if (!(call instanceof RsMacroCall macroCall)) {
            return CachedValueProvider.Result.create(
                new RsResult.Err<>(GetMacroExpansionError.MemExpAttrMacro),
                ModificationTracker.EVER_CHANGED
            );
        }
        if ("include".equals(RsMacroCallUtil.getMacroName(macroCall))) {
            return CachedValueProvider.Result.create(
                expandIncludeMacroCall(macroCall),
                RsPsiManagerUtil.getRustStructureOrAnyPsiModificationTracker(macroCall)
            );
        }
        // the call is resolved through the DefMap of its crate, which is not built while indexing
        if (DumbService.isDumb(myProject)) {
            return CachedValueProvider.Result.create(
                new RsResult.Err<>(GetMacroExpansionError.MacroExpansionEngineIsNotReady),
                ModificationTracker.EVER_CHANGED
            );
        }
        return CachedValueProvider.Result.create(expandToMemoryFile(macroCall), getModificationTrackers(macroCall));
    }

    /**
     * `include!("x.rs")` expands to the items of the file it names. A file under `OUT_DIR` that a provider generates
     * expands to the generated code instead, and is never read from the output directory of the build script.
     */
    @Nonnull
    private static RsResult<MacroExpansion, GetMacroExpansionError> expandIncludeMacroCall(@Nonnull RsMacroCall call) {
        String path = RsMacroCallUtil.getIncludeMacroArgumentValue(call, null);
        if (path == null) return new RsResult.Err<>(GetMacroExpansionError.IncludingFileNotFound);
        String outDirRelativePath = RsMacroCallUtil.getOutDirRelativePath(path);
        if (outDirRelativePath != null) {
            Crate crate = RsElementUtil.getContainingCrate(call);
            RsGeneratedCode generated = crate instanceof FakeCrate
                ? null
                : RsGeneratedIncludes.getInstance(call.getProject()).find(crate, outDirRelativePath);
            if (generated != null) return expandGeneratedCode(call, generated);
            path = RsMacroCallUtil.resolveOutDir(path, crate);
            if (path == null) return new RsResult.Err<>(GetMacroExpansionError.IncludingFileNotFound);
        }
        RsFile includingFile = findIncludingFile(call, path);
        if (includingFile == null) return new RsResult.Err<>(GetMacroExpansionError.IncludingFileNotFound);
        List<RsExpandedElement> items = PsiElementUtil.stubChildrenOfType(includingFile, RsExpandedElement.class);
        return new RsResult.Ok<>(new MacroExpansion.Items(includingFile, items));
    }

    /** The generated code, parsed in memory as the expansion of the call; its names navigate to their sources. */
    @Nonnull
    private static RsResult<MacroExpansion, GetMacroExpansionError> expandGeneratedCode(
        @Nonnull RsMacroCall call,
        @Nonnull RsGeneratedCode generated
    ) {
        MacroExpansion expansion = RsExpandedElementUtil.parseExpandedTextWithContext(
            MacroExpansionContext.ITEM,
            new RsPsiFactory(call.getProject(), false),
            generated.getText()
        );
        if (expansion == null) {
            return new RsResult.Err<>(new GetMacroExpansionError.MemExpParsingError(generated.getText(), MacroExpansionContext.ITEM));
        }
        expansion.getFile().putUserData(RsGeneratedCode.KEY, generated);
        PsiElement callContext = RsPossibleMacroCallUtil.getContextToSetForExpansion(call);
        for (RsExpandedElement element : expansion.getElements()) {
            element.putUserData(RS_EXPANSION_MACRO_CALL, call);
            if (callContext instanceof RsElement rsContext) {
                RsExpandedElementUtil.setExpandedElementContext(element, rsContext);
            }
        }
        if (callContext instanceof RsElement rsContext) {
            RsExpandedElementUtil.setRsFileContext(expansion.getFile(), rsContext, true);
        }
        // the items generated from a source element stand for it: light elements that navigate to and rename it.
        // They take the place of the parsed items, which keep the context set above for the code inside them.
        expansion = new MacroExpansion.Items(expansion.getFile(), RsLightItems.lighten(expansion.getElements(), generated));
        for (RsExpandedElement element : expansion.getElements()) {
            element.putUserData(RS_EXPANSION_MACRO_CALL, call);
        }
        return new RsResult.Ok<>(expansion);
    }

    /** The file an `include!` names, relative to the file the call is written in. */
    @Nullable
    private static RsFile findIncludingFile(@Nonnull RsMacroCall call, @Nonnull String path) {
        RsPossibleMacroCall expandedFrom = MacroExpansionUtil.findMacroCallExpandedFrom(call);
        PsiElement source = expandedFrom != null ? expandedFrom : call;
        PsiFile containingFile = source.getContainingFile();
        VirtualFile file = containingFile != null ? containingFile.getOriginalFile().getVirtualFile() : null;
        VirtualFile parent = file != null ? file.getParent() : null;
        if (parent == null) return null;
        VirtualFile included;
        try {
            // an absolute path - the output directory of the build script - is a path of the same machine as the file
            included = VirtualFileManager.getInstance().findFileByNioPath(parent.toNioPath().resolve(path));
        }
        catch (UnsupportedOperationException | java.nio.file.InvalidPathException e) {
            included = parent.findFileByRelativePath(path);
        }
        if (included == null) return null;
        return PsiManager.getInstance(call.getProject()).findFile(included) instanceof RsFile rsFile ? rsFile : null;
    }

    @Nonnull
    private static RsResult<MacroExpansion, GetMacroExpansionError> expandToMemoryFile(@Nonnull RsMacroCall call) {
        Crate crate = RsElementUtil.getContainingCrate(call);
        if (crate instanceof FakeCrate) {
            return new RsResult.Err<>(GetMacroExpansionError.Unresolved);
        }
        if (!RsElementUtil.isEnabledByCfg(call, crate)) {
            return new RsResult.Err<>(GetMacroExpansionError.CfgDisabled);
        }
        RsFile containingFile = PsiElementUtil.getContainingRsFileSkippingCodeFragments(call);
        if (containingFile != null && containingFile.getMacroExpansionDepth() >= RECURSION_LIMIT) {
            return new RsResult.Err<>(GetMacroExpansionError.TooDeepExpansion);
        }

        RsResult<RsMacroDataWithHash<?>, ResolveMacroWithoutPsiError> def = FacadeResolve.resolveToMacroWithoutPsi(call);
        if (def.isErr()) {
            return new RsResult.Err<>(toExpansionError(def.err()));
        }
        RsMacroCallData callData = RsMacroCallData.fromPsi(call);
        if (callData == null) {
            return new RsResult.Err<>(GetMacroExpansionError.MacroCallSyntax);
        }
        RsResult<Pair<CharSequence, RangeMap>, MacroExpansionError> expanded =
            FunctionLikeMacroExpander.forCrate(crate).expandMacroAsTextWithErr(def.ok().getData(), callData);
        if (expanded.isErr()) {
            return new RsResult.Err<>(new GetMacroExpansionError.ExpansionError(expanded.err()));
        }
        CharSequence text = expanded.ok().getFirst();

        MacroExpansionContext context = MacroExpansionUtil.getExpansionContext(call);
        MacroExpansion expansion = RsExpandedElementUtil.parseExpandedTextWithContext(
            context,
            new RsPsiFactory(call.getProject(), false),
            text
        );
        if (expansion == null) {
            return new RsResult.Err<>(new GetMacroExpansionError.MemExpParsingError(text, context));
        }
        expansion.getFile().putUserData(MacroExpansionUtil.MACRO_RANGE_MAP_KEY, expanded.ok().getSecond());

        PsiElement callContext = RsPossibleMacroCallUtil.getContextToSetForExpansion(call);
        for (RsExpandedElement element : expansion.getElements()) {
            element.putUserData(RS_EXPANSION_MACRO_CALL, call);
            if (callContext instanceof RsElement rsContext) {
                RsExpandedElementUtil.setExpandedElementContext(element, rsContext);
            }
        }
        if (callContext instanceof RsElement rsContext) {
            RsExpandedElementUtil.setRsFileContext(expansion.getFile(), rsContext, true);
        }
        return new RsResult.Ok<>(expansion);
    }

    /**
     * The expansion is stale once the call itself or the crate structure changes. A non-physical call has no PSI
     * events, so its file's modification stamp is tracked instead.
     */
    @Nonnull
    private static Object[] getModificationTrackers(@Nonnull RsMacroCall call) {
        ModificationTracker structure = RsPsiManagerUtil.getRustStructureOrAnyPsiModificationTracker(call);
        if (!call.isPhysical()) {
            return new Object[]{structure, call.getContainingFile()};
        }
        return new Object[]{structure, call.getModificationTracker()};
    }

    @Nonnull
    private static GetMacroExpansionError toExpansionError(@Nonnull ResolveMacroWithoutPsiError error) {
        if (error == ResolveMacroWithoutPsiError.NoProcMacroArtifact) {
            return GetMacroExpansionError.NoProcMacroArtifact;
        }
        if (error == ResolveMacroWithoutPsiError.HardcodedProcMacroAttribute) {
            return GetMacroExpansionError.Skipped;
        }
        if (error instanceof ResolveMacroWithoutPsiError.UnmatchedProcMacroKind unmatched) {
            return new GetMacroExpansionError.UnmatchedProcMacroKind(unmatched.getCallKind(), unmatched.getDefKind());
        }
        return GetMacroExpansionError.Unresolved;
    }

    @Nullable
    @Override
    public RsPossibleMacroCall getExpandedFrom(@Nonnull RsExpandedElement element) {
        return element.getUserData(RS_EXPANSION_MACRO_CALL);
    }

    @Nullable
    @Override
    public RsMacroCall getIncludedFrom(@Nonnull RsFile file) {
        return null;
    }

    @Nullable
    @Override
    public PsiElement getContextOfMacroCallExpandedFrom(@Nonnull RsFile stubParent) {
        return null;
    }

    @Override
    public boolean isExpansionFileOfCurrentProject(@Nonnull VirtualFile file) {
        return false;
    }

    @Nullable
    @Override
    public Integer getCrateForExpansionFile(@Nonnull VirtualFile file) {
        return null;
    }

    @Override
    public void reexpand() {
    }

    @Nonnull
    @Override
    public MacroExpansionMode getMacroExpansionMode() {
        return MacroExpansionMode.OLD;
    }

    
    @Nonnull
    @Override
    public Disposable setUnitTestExpansionModeAndDirectory(
        @Nonnull MacroExpansionScope mode,
        @Nonnull String cacheDirectory,
        boolean clearCacheBeforeDispose
    ) {
        return () -> {};
    }

    
    @Override
    public void updateInUnitTestMode() {
    }

    
    @Override
    public void setMacroExpansionEnabled(boolean enabled) {
    }

    @Override
    public void dispose() {
        isDisposed = true;
    }
}
