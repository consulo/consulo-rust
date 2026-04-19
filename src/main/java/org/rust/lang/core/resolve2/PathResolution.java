/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.crate.CratePersistentId;
import org.rust.lang.core.macros.decl.DeclMacroConstantsUtil;
import org.rust.lang.core.psi.RsMacro;
import org.rust.lang.core.psi.ext.RsMacroUtil;

import java.util.Arrays;
import java.util.List;

/**
 * Provides path resolution logic for CrateDefMap.
 */
public final class PathResolution {

    private PathResolution() {}

    /**
     * Resolves a path within a CrateDefMap.
     * Returns {@code reachedFixedPoint=true} if additions to ModData.visibleItems wouldn't change the result.
     */
    @NotNull
    public static ResolvePathResult resolvePathFp(
        @NotNull CrateDefMap defMap,
        @NotNull ModData containingMod,
        @NotNull String[] path,
        @NotNull ResolveMode mode,
        boolean withInvisibleItems
    ) {
        PathInfo pathInfo = getPathKind(path);
        PathKind pathKind = pathInfo.kind;
        int firstSegmentIndex = pathInfo.segmentsToSkip;

        PerNs firstSegmentPerNs;
        if (pathKind instanceof PathKind.DollarCrate) {
            int crateId = ((PathKind.DollarCrate) pathKind).crateId;
            CrateDefMap depDefMap = defMap.getDefMap(crateId);
            if (depDefMap == null) {
                throw new IllegalStateException("Can't find DefMap for path " + String.join("::", path));
            }
            firstSegmentPerNs = depDefMap.getRootAsPerNs();
        } else if (pathKind == PathKind.CRATE) {
            firstSegmentPerNs = defMap.getRootAsPerNs();
        } else if (pathKind instanceof PathKind.Super) {
            int level = ((PathKind.Super) pathKind).level;
            ModData modData = containingMod.getNthParent(level);
            if (modData == null) {
                return ResolvePathResult.empty(true);
            }
            firstSegmentPerNs = modData.isCrateRoot() ? defMap.getRootAsPerNs() : modData.asPerNs();
        } else if (defMap.getMetaData().getEdition() == CargoWorkspace.Edition.EDITION_2015
            && (pathKind == PathKind.ABSOLUTE || (pathKind == PathKind.PLAIN && mode == ResolveMode.IMPORT))) {
            String firstSegment = path[firstSegmentIndex++];
            firstSegmentPerNs = resolveNameInCrateRootOrExternPrelude(defMap, firstSegment);
        } else if (pathKind == PathKind.ABSOLUTE) {
            String crateName = path[firstSegmentIndex++];
            CrateDefMap externDefMap = defMap.getExternPrelude().get(crateName);
            if (externDefMap == null) {
                return ResolvePathResult.empty(false);
            }
            firstSegmentPerNs = externDefMap.getRootAsPerNs();
        } else if (pathKind == PathKind.PLAIN) {
            String firstSegment = path[firstSegmentIndex++];
            boolean withLegacyMacros = mode == ResolveMode.IMPORT && path.length == 1;
            firstSegmentPerNs = resolveNameInModule(defMap, containingMod, firstSegment, withLegacyMacros);
        } else {
            throw new IllegalStateException("unreachable");
        }

        PerNs currentPerNs = firstSegmentPerNs;
        boolean visitedOtherCrate = false;
        for (int segmentIndex = firstSegmentIndex; segmentIndex < path.length; segmentIndex++) {
            VisItem currentModAsVisItem = null;
            for (VisItem vi : currentPerNs.getTypes()) {
                if (withInvisibleItems || !vi.getVisibility().isInvisible()) {
                    currentModAsVisItem = vi;
                    break;
                }
            }
            if (currentModAsVisItem == null) {
                return ResolvePathResult.empty(false);
            }
            ModData currentModData = defMap.tryCastToModData(currentModAsVisItem);
            if (currentModData == null) {
                return ResolvePathResult.empty(true);
            }
            if (currentModData.getCrate() != defMap.getCrate()) {
                visitedOtherCrate = true;
            }
            String segment = path[segmentIndex];
            currentPerNs = currentModData.getVisibleItem(segment);
        }
        PerNs resultPerNs = withInvisibleItems ? currentPerNs : currentPerNs.filterVisibility(v -> !v.isInvisible());
        return new ResolvePathResult(resultPerNs, true, visitedOtherCrate);
    }

    /**
     * Resolves a macro call to its MacroDefInfo.
     */
    @Nullable
    public static MacroDefInfo resolveMacroCallToMacroDefInfo(
        @NotNull CrateDefMap defMap,
        @NotNull ModData containingMod,
        @NotNull String[] macroPath,
        @NotNull MacroIndex macroIndex
    ) {
        if (macroPath.length == 1) {
            String name = macroPath[0];
            MacroDefInfo legacyResult = resolveMacroCallToLegacyMacroDefInfo(containingMod, name, macroIndex);
            if (legacyResult != null) return legacyResult;
        }

        ResolvePathResult perNs = resolvePathFp(defMap, containingMod, macroPath, ResolveMode.OTHER, false);
        VisItem[] macros = perNs.getResolvedDef().getMacros();
        if (macros.length != 1) return null;
        return defMap.getMacroInfo(macros[0]);
    }

    @Nullable
    private static MacroDefInfo resolveMacroCallToLegacyMacroDefInfo(
        @NotNull ModData modData,
        @NotNull String name,
        @NotNull MacroIndex macroIndex
    ) {
        List<MacroDefInfo> macros = modData.getLegacyMacros().get(name);
        if (macros != null) {
            MacroDefInfo result = getLastBefore(macros, macroIndex);
            if (result != null) return result;
        }
        ModData context = modData.getContext();
        if (context != null) {
            return resolveMacroCallToLegacyMacroDefInfo(context, name, macroIndex);
        }
        return null;
    }

    /**
     * Gets the last MacroDefInfo before the given macroIndex.
     */
    @Nullable
    public static MacroDefInfo getLastBefore(@NotNull List<MacroDefInfo> macroDefInfos, @NotNull MacroIndex macroIndex) {
        MacroDefInfo best = null;
        MacroIndex bestIndex = null;
        for (MacroDefInfo info : macroDefInfos) {
            if (info instanceof DeclMacroDefInfo) {
                MacroIndex infoIndex = ((DeclMacroDefInfo) info).getMacroIndex();
                if (infoIndex.compareTo(macroIndex) >= 0) continue;
                if (best == null || (bestIndex != null && infoIndex.compareTo(bestIndex) > 0)) {
                    best = info;
                    bestIndex = infoIndex;
                }
            } else {
                // Non-DeclMacroDefInfo always qualifies, with index 0
                MacroIndex zeroIndex = new MacroIndex(new int[]{0});
                if (best == null || (bestIndex != null && zeroIndex.compareTo(bestIndex) > 0)) {
                    best = info;
                    bestIndex = zeroIndex;
                }
            }
        }
        return best;
    }

    @NotNull
    private static PerNs resolveNameInExternPrelude(@NotNull CrateDefMap defMap, @NotNull String name) {
        CrateDefMap externDefMap = defMap.getExternPrelude().get(name);
        if (externDefMap == null) return PerNs.EMPTY;
        return externDefMap.getRootAsPerNs();
    }

    /**
     * Resolves an extern crate name to its DefMap.
     */
    @Nullable
    public static CrateDefMap resolveExternCrateAsDefMap(@NotNull CrateDefMap defMap, @NotNull String name) {
        if ("self".equals(name)) return defMap;
        return defMap.getDirectDependenciesDefMaps().get(name);
    }

    /**
     * Resolves a name in a module, considering legacy macros, current scope, extern prelude, and std prelude.
     */
    @NotNull
    public static PerNs resolveNameInModule(
        @NotNull CrateDefMap defMap,
        @NotNull ModData modData,
        @NotNull String name,
        boolean withLegacyMacros
    ) {
        PerNs result = doResolveNameInModule(defMap, modData, name, withLegacyMacros);
        ModData context = modData.getContext();
        if (context == null) return result;
        PerNs resultFromContext = resolveNameInModule(defMap, context, name, withLegacyMacros);
        return result.or(resultFromContext);
    }

    @NotNull
    private static PerNs doResolveNameInModule(
        @NotNull CrateDefMap defMap,
        @NotNull ModData modData,
        @NotNull String name,
        boolean withLegacyMacros
    ) {
        PerNs fromLegacyMacro = withLegacyMacros ? getFirstLegacyMacro(modData, name) : PerNs.EMPTY;
        PerNs fromScope = modData.getVisibleItem(name);
        PerNs fromExternPrelude = resolveNameInExternPrelude(defMap, name);
        PerNs fromPrelude = resolveNameInPrelude(defMap, name);
        return fromLegacyMacro.or(fromScope).or(fromExternPrelude).or(fromPrelude);
    }

    @NotNull
    private static PerNs getFirstLegacyMacro(@NotNull ModData modData, @NotNull String name) {
        List<MacroDefInfo> macros = modData.getLegacyMacros().get(name);
        if (macros == null || macros.isEmpty()) return PerNs.EMPTY;
        MacroDefInfo def = macros.get(0);
        Visibility visibility;
        if (!(def instanceof DeclMacroDefInfo)) {
            visibility = Visibility.PUBLIC;
        } else if (((DeclMacroDefInfo) def).isHasMacroExport()) {
            visibility = Visibility.PUBLIC;
        } else {
            visibility = modData.getRootModData().getVisibilityInSelf();
        }
        VisItem visItem = new VisItem(def.getPath(), visibility);
        return PerNs.macros(visItem);
    }

    /**
     * Returns the first (or single public) DeclMacroDefInfo from a list.
     */
    @NotNull
    public static DeclMacroDefInfo singlePublicOrFirst(@NotNull List<DeclMacroDefInfo> list) {
        for (DeclMacroDefInfo info : list) {
            if (info.isHasMacroExport()) return info;
        }
        return list.get(0);
    }

    /**
     * Returns the first (or single public) RsMacro from a list.
     */
    @NotNull
    public static RsMacro singlePublicOrFirstMacro(@NotNull List<RsMacro> list) {
        for (RsMacro macro : list) {
            if (RsMacroUtil.getHasMacroExport(macro)) return macro;
        }
        return list.get(0);
    }

    @NotNull
    private static PerNs resolveNameInCrateRootOrExternPrelude(@NotNull CrateDefMap defMap, @NotNull String name) {
        PerNs fromCrateRoot = defMap.getRoot().getVisibleItem(name);
        PerNs fromExternPrelude = resolveNameInExternPrelude(defMap, name);
        return fromCrateRoot.or(fromExternPrelude);
    }

    @NotNull
    private static PerNs resolveNameInPrelude(@NotNull CrateDefMap defMap, @NotNull String name) {
        ModData prelude = defMap.getPrelude();
        if (prelude == null) return PerNs.EMPTY;
        return prelude.getVisibleItem(name);
    }

    /**
     * Sealed class PathKind - represented as abstract class with static subclasses in Java.
     */
    static abstract class PathKind {
        static final PathKind PLAIN = new Plain();
        static final PathKind CRATE = new Crate();
        static final PathKind ABSOLUTE = new Absolute();

        private PathKind() {}

        static class Plain extends PathKind {}
        static class Super extends PathKind {
            final int level;
            Super(int level) { this.level = level; }
        }
        static class Crate extends PathKind {}
        static class Absolute extends PathKind {}
        static class DollarCrate extends PathKind {
            final int crateId;
            DollarCrate(int crateId) { this.crateId = crateId; }
        }
    }

    /**
     * Path information: the kind and number of segments to skip.
     */
    static class PathInfo {
        @NotNull final PathKind kind;
        final int segmentsToSkip;

        PathInfo(@NotNull PathKind kind, int segmentsToSkip) {
            this.kind = kind;
            this.segmentsToSkip = segmentsToSkip;
        }
    }

    /**
     * Determines the kind of a path.
     */
    @NotNull
    static PathInfo getPathKind(@NotNull String[] path) {
        String first = path[0];
        if (DeclMacroConstantsUtil.MACRO_DOLLAR_CRATE_IDENTIFIER.equals(first)) {
            String crateIdStr = path.length > 1 ? path[1] : null;
            Integer crateId = null;
            if (crateIdStr != null) {
                try {
                    crateId = Integer.parseInt(crateIdStr);
                } catch (NumberFormatException ignored) {
                }
            }
            if (crateId != null) {
                return new PathInfo(new PathKind.DollarCrate(crateId), 2);
            } else {
                CrateDefMap.RESOLVE_LOG.warn("Invalid path starting with dollar crate: '" + Arrays.toString(path) + "'");
                return new PathInfo(PathKind.PLAIN, 0);
            }
        }
        if ("crate".equals(first)) {
            return new PathInfo(PathKind.CRATE, 1);
        }
        if ("super".equals(first)) {
            int level = 0;
            while (level < path.length && "super".equals(path[level])) level++;
            return new PathInfo(new PathKind.Super(level), level);
        }
        if ("self".equals(first)) {
            if (path.length > 1 && "super".equals(path[1])) {
                PathInfo inner = getPathKind(Arrays.copyOfRange(path, 1, path.length));
                return new PathInfo(inner.kind, inner.segmentsToSkip + 1);
            }
            return new PathInfo(new PathKind.Super(0), 1);
        }
        if ("".equals(first)) {
            return new PathInfo(PathKind.ABSOLUTE, 1);
        }
        return new PathInfo(PathKind.PLAIN, 0);
    }

    /**
     * Result of path resolution.
     */
    public static final class ResolvePathResult {
        @NotNull private final PerNs resolvedDef;
        private final boolean reachedFixedPoint;
        private final boolean visitedOtherCrate;

        public ResolvePathResult(@NotNull PerNs resolvedDef, boolean reachedFixedPoint, boolean visitedOtherCrate) {
            this.resolvedDef = resolvedDef;
            this.reachedFixedPoint = reachedFixedPoint;
            this.visitedOtherCrate = visitedOtherCrate;
        }

        @NotNull
        public PerNs getResolvedDef() { return resolvedDef; }

        public boolean isReachedFixedPoint() { return reachedFixedPoint; }

        public boolean isVisitedOtherCrate() { return visitedOtherCrate; }

        @NotNull
        public static ResolvePathResult empty(boolean reachedFixedPoint) {
            return new ResolvePathResult(PerNs.EMPTY, reachedFixedPoint, false);
        }
    }
}
