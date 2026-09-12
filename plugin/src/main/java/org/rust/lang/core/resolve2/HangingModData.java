/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.macros.MacroExpansionUtil;
import org.rust.lang.core.macros.decl.DeclMacroConstantsUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve2.util.DollarCrateHelper;
import org.rust.openapiext.OpenApiUtil;

import java.lang.ref.SoftReference;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import consulo.application.progress.ProgressIndicator;
import consulo.util.lang.Pair;

/**
 * Provides functions for creating hanging (local scope) ModData for blocks and code fragments.
 */
public final class HangingModData {

    private HangingModData() {}

    private static final Key<SoftReference<consulo.util.lang.Pair<ModData, List<Long>>>> HANGING_MOD_DATA_KEY = Key.create("HANGING_MOD_DATA_KEY");

    /**
     * Gets hanging mod info for a scope (RsBlock or RsCodeFragment).
     */
    @Nullable
    public static RsModInfo getHangingModInfo(@Nonnull RsItemsOwner scope) {
        if (!(scope instanceof RsBlock) && !(scope instanceof RsCodeFragment)) {
            throw new IllegalArgumentException("scope must be RsBlock or RsCodeFragment");
        }
        if (!shouldCreateHangingModInfo(scope)) return null;

        RsModInfo contextInfo = getContextModInfo(scope);
        if (contextInfo == null) return null;
        return getHangingModInfo(scope, contextInfo);
    }

    /**
     * Gets tmp mod info for a temporary module.
     */
    @Nullable
    public static RsModInfo getTmpModInfo(@Nonnull RsModItem scope) {
        RsMod context = scope.getContext() instanceof RsMod ? (RsMod) scope.getContext() : null;
        if (context == null) return null;
        RsModInfo contextInfo = FacadeResolve.getModInfo(context);
        if (contextInfo == null) return null;
        return getHangingModInfo(scope, contextInfo);
    }

    /**
     * Gets hanging mod info for a scope with a known context info.
     */
    @Nonnull
    public static RsModInfo getHangingModInfo(@Nonnull RsItemsOwner scope, @Nonnull RsModInfo contextInfo) {
        Project project = contextInfo.getProject();
        CrateDefMap defMap = contextInfo.getDefMap();
        ModData contextData = contextInfo.getModData();
        long modificationStamp;
        RsFunction func = RsElementUtil.stubAncestorStrict(scope, RsFunction.class);
        if (func != null) {
            modificationStamp = func.getModificationTracker().getModificationCount();
        } else {
            modificationStamp = scope.getContainingFile().getModificationStamp();
        }
        List<Long> dependencies = Arrays.asList(defMap.getTimestamp(), contextData.getTimestamp(), modificationStamp);
        ModData hangingModData = OpenApiUtil.getCachedOrCompute(scope, HANGING_MOD_DATA_KEY, dependencies, () ->
            createHangingModData(scope, contextInfo)
        );
        DataPsiHelper dataPsiHelper = new LocalScopeDataPsiHelper(scope, hangingModData, contextInfo.getDataPsiHelper());
        return new RsModInfo(project, defMap, hangingModData, contextInfo.getCrate(), dataPsiHelper);
    }

    /**
     * Gets local mod info for a locally defined module.
     */
    @Nullable
    public static RsModInfo getLocalModInfo(@Nonnull RsMod scope) {
        Object context = scope.getContext();
        if (!(context instanceof RsBlock)) return null;
        RsModInfo contextInfo = getHangingModInfo((RsBlock) context);
        if (contextInfo == null) return null;
        Project project = contextInfo.getProject();
        CrateDefMap defMap = contextInfo.getDefMap();
        Crate crate = contextInfo.getCrate();

        String name = scope instanceof RsModItem ? ((RsModItem) scope).getModName() : null;
        ModData modData = contextInfo.getModData().getChildModules().get(name);
        if (modData == null) return null;
        DataPsiHelper dataPsiHelper = new LocalScopeDataPsiHelper(scope, modData, contextInfo.getDataPsiHelper());
        return new RsModInfo(project, defMap, modData, crate, dataPsiHelper);
    }

    /**
     * Gets the nearest ancestor ModInfo for a scope.
     */
    @Nullable
    public static RsModInfo getNearestAncestorModInfo(@Nonnull RsItemsOwner scope) {
        if (!(scope instanceof RsBlock)) return FacadeResolve.getModInfo(scope);
        RsModInfo hanging = getHangingModInfo(scope);
        if (hanging != null) return hanging;
        return getContextModInfo(scope);
    }

    @Nonnull
    private static ModData createHangingModData(@Nonnull RsItemsOwner scope, @Nonnull RsModInfo contextInfo) {
        Project project = contextInfo.getProject();
        CrateDefMap defMap = contextInfo.getDefMap();
        ModData contextData = contextInfo.getModData();
        Crate crate = contextInfo.getCrate();

        String pathSegment = scope instanceof RsModItem ? "local#" + ((RsModItem) scope).getModName() : "#block";
        ModData hangingModData = new ModData(
            contextData.getParent(),
            contextData.getCrate(),
            contextData.getPath().append(pathSegment),
            contextData.getMacroIndex().append(Integer.MAX_VALUE),
            contextData.isDeeplyEnabledByCfgOuter(),
            RsElementUtil.isEnabledByCfg((PsiElement) scope, crate),
            null, // fileId
            "",   // fileRelativePath
            null, // ownedDirectoryId
            false, // hasPathAttribute
            false, // hasMacroUse
            false, // isEnum
            false, // isNormalCrate
            contextData, // context
            scope instanceof RsBlock, // isBlock
            pathSegment + " in " + contextData.getCrateDescription()
        );

        CollectorContext collectorContext = new CollectorContext(crate, project, hangingModData);
        ModCollectorContext modCollectorContext = new ModCollectorContext(defMap, collectorContext);
        DollarCrateHelper dollarCrateHelper = createDollarCrateHelper(scope);
        ModCollector.collectScope(scope, hangingModData, modCollectorContext,
            hangingModData.getMacroIndex(), dollarCrateHelper, null, false);

        consulo.application.progress.ProgressIndicator indicator =
            ProgressManager.getGlobalProgressIndicator() != null
                ? ProgressManager.getGlobalProgressIndicator()
                : new EmptyProgressIndicator();
        DefCollector defCollector = new DefCollector(project, defMap, collectorContext, null, indicator);
        defCollector.collect();
        return hangingModData;
    }

    @Nullable
    private static RsModInfo getContextModInfo(@Nonnull PsiElement element) {
        @SuppressWarnings("unchecked")
        RsItemsOwner context = (RsItemsOwner) RsElementUtil.contextStrict(element, (Class) RsItemsOwner.class);
        if (context == null) return null;
        if (context instanceof RsMod) {
            return FacadeResolve.getModInfo(context);
        }
        if (context instanceof RsBlock && shouldCreateHangingModInfo(context)) {
            return getHangingModInfo(context);
        }
        return getContextModInfo(context);
    }

    private static boolean shouldCreateHangingModInfo(@Nonnull RsItemsOwner scope) {
        for (RsElement child : RsItemsOwnerUtil.getItemsAndMacros(scope)) {
            if (child instanceof RsItemElement || child instanceof RsMacro || child instanceof RsMacroCall) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static DollarCrateHelper createDollarCrateHelper(@Nonnull RsItemsOwner scope) {
        RsPossibleMacroCall expanded = MacroExpansionUtil.findMacroCallExpandedFromNonRecursive((PsiElement) scope);
        if (!(expanded instanceof RsMacroCall)) return null;
        RsMacroCall call = (RsMacroCall) expanded;
        Object expansion = RsMacroCallUtil.getExpansion(call);
        if (expansion == null) return null;
        // Simplified implementation - full implementation would check for $crate in expansion
        return null;
    }

    /**
     * Finds hanging ModData by path.
     */
    @Nullable
    public static ModData findHangingModData(@Nonnull ModPath path, @Nonnull ModData hangingModData) {
        if (path.equals(hangingModData.getPath())) return hangingModData;
        String[] relativePath = getRelativePathTo(path, hangingModData.getPath());
        if (relativePath != null) {
            return hangingModData.getChildModData(relativePath);
        }
        return null;
    }

    @Nullable
    private static String[] getRelativePathTo(@Nonnull ModPath path, @Nonnull ModPath parent) {
        if (parent.isSubPathOf(path)) {
            return Arrays.copyOfRange(path.getSegments(), parent.getSegments().length, path.getSegments().length);
        }
        return null;
    }

    /**
     * DataPsiHelper implementation for local scopes.
     */
    private static class LocalScopeDataPsiHelper implements DataPsiHelper {
        @Nonnull private final RsItemsOwner scope;
        @Nonnull private final ModData modData;
        @Nullable private final DataPsiHelper delegate;

        LocalScopeDataPsiHelper(
            @Nonnull RsItemsOwner scope,
            @Nonnull ModData modData,
            @Nullable DataPsiHelper delegate
        ) {
            this.scope = scope;
            this.modData = modData;
            this.delegate = delegate;
        }

        @Override
        @Nullable
        public ModData psiToData(@Nonnull RsItemsOwner scope) {
            if (scope == this.scope) return modData;
            return delegate != null ? delegate.psiToData(scope) : null;
        }

        @Override
        @Nullable
        public RsMod dataToPsi(@Nonnull ModData data) {
            if (data == modData && scope instanceof RsMod) return (RsMod) scope;
            String[] relativePath = getRelativePathTo(data.getPath(), modData.getPath());
            if (relativePath != null) {
                RsMod current = scope instanceof RsMod ? (RsMod) scope : null;
                for (String segment : relativePath) {
                    if (current == null) return null;
                    current = RsModUtil.getChildModule(current, segment);
                    if (current == null) return null;
                }
                return current;
            }
            return delegate != null ? delegate.dataToPsi(data) : null;
        }

        @Override
        @Nullable
        public ModData findModData(@Nonnull ModPath path) {
            ModData found = findHangingModData(path, modData);
            if (found != null) return found;
            return delegate != null ? delegate.findModData(path) : null;
        }
    }
}
