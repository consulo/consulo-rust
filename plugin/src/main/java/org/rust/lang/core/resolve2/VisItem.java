/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.resolve.Namespace;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.crate.CrateGraphService;
import org.rust.lang.core.psi.RsEnumBody;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.RsEnumVariant;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsMacro;
import org.rust.lang.core.psi.RsMacro2;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsItemElement;
import org.rust.lang.core.psi.ext.RsItemsOwner;
import org.rust.lang.core.psi.ext.RsItemsOwnerUtil;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.psi.ext.RsCachedItems;
import org.rust.lang.core.psi.ext.RsElementUtil;

/**
 * The item which can be visible in the module (either directly declared or imported).
 * Could be RsEnumVariant (because it can be imported).
 */
public final class VisItem {

    public static final VisItem[] EMPTY_ARRAY = new VisItem[0];

    @Nonnull
    private final ModPath path;
    @Nonnull
    private final Visibility visibility;
    private final boolean isModOrEnum;
    private final boolean isTrait;
    /**
     * Records whether this item was added to mod scope with named or glob import.
     * Needed to determine whether we can override it (usual imports overrides glob-imports).
     * Used only in DefCollector, but stored here as an optimization.
     */
    private final boolean isFromNamedImport;

    public VisItem(@Nonnull ModPath path, @Nonnull Visibility visibility) {
        this(path, visibility, false, false, true);
    }

    public VisItem(@Nonnull ModPath path, @Nonnull Visibility visibility, boolean isModOrEnum) {
        this(path, visibility, isModOrEnum, false, true);
    }

    public VisItem(@Nonnull ModPath path, @Nonnull Visibility visibility, boolean isModOrEnum, boolean isTrait) {
        this(path, visibility, isModOrEnum, isTrait, true);
    }

    public VisItem(@Nonnull ModPath path, @Nonnull Visibility visibility, boolean isModOrEnum, boolean isTrait, boolean isFromNamedImport) {
        this.path = path;
        this.visibility = visibility;
        this.isModOrEnum = isModOrEnum;
        this.isTrait = isTrait;
        this.isFromNamedImport = isFromNamedImport;
        if (!isModOrEnum && path.getSegments().length == 0) {
            throw new IllegalStateException("VisItem check failed: isModOrEnum || path.segments.isNotEmpty()");
        }
    }

    @Nonnull
    public ModPath getPath() {
        return path;
    }

    @Nonnull
    public Visibility getVisibility() {
        return visibility;
    }

    public boolean isModOrEnum() {
        return isModOrEnum;
    }

    public boolean isTrait() {
        return isTrait;
    }

    public boolean isFromNamedImport() {
        return isFromNamedImport;
    }

    /** Mod where item is explicitly declared */
    @Nonnull
    public ModPath getContainingMod() {
        return path.getParent();
    }

    @Nonnull
    public String getName() {
        return path.getName();
    }

    public int getCrate() {
        return path.getCrate();
    }

    public boolean isCrateRoot() {
        return path.getSegments().length == 0;
    }

    @Nonnull
    public VisItem adjust(@Nonnull Visibility visibilityNew, boolean isFromNamedImport) {
        return new VisItem(
            path,
            visibilityNew.intersect(visibility),
            isModOrEnum,
            isTrait,
            isFromNamedImport
        );
    }

    @Nonnull
    public VisItem copy(@Nonnull Visibility visibility, boolean isFromNamedImport) {
        return new VisItem(path, visibility, isModOrEnum, isTrait, isFromNamedImport);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VisItem visItem = (VisItem) o;
        return isModOrEnum == visItem.isModOrEnum
            && isTrait == visItem.isTrait
            && isFromNamedImport == visItem.isFromNamedImport
            && Objects.equals(path, visItem.path)
            && Objects.equals(visibility, visItem.visibility);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, visibility, isModOrEnum, isTrait, isFromNamedImport);
    }

    /**
     * Resolves this VisItem to PSI elements in the given namespace. Walks down the crate's
     * item tree from the crate root, resolving each {@link ModPath} segment either as a child
     * module or (for enum variants) as a declared variant of the containing enum.
     */
    @Nonnull
    public List<RsNamedElement> toPsi(@Nonnull RsModInfo info, @Nonnull Namespace namespace) {
        if (isModOrEnum()) {
            return pathToRsModOrEnum(info);
        }
        // Regular item: find the containing module/enum and pick items with matching name.
        List<RsElement> scopes = containingModToScope(info);
        List<RsNamedElement> result = new java.util.ArrayList<>();
        for (RsElement scope : scopes) {
            if (scope instanceof RsEnumItem) {
                RsEnumBody body = ((RsEnumItem) scope).getEnumBody();
                if (body == null) continue;
                for (RsEnumVariant variant : body.getEnumVariantList()) {
                    if (getName().equals(variant.getName())) result.add(variant);
                }
            } else if (scope instanceof RsItemsOwner) {
                for (RsItemElement item :
                    namedItems((RsItemsOwner) scope, getName())) {
                    if (item instanceof RsNamedElement) result.add((RsNamedElement) item);
                }
            }
        }
        return result;
    }

    /**
     * Resolves this VisItem as a scoped macro to a PSI element — looks up legacy macros,
     * macro2 items, and proc-macro functions in the containing module.
     */
    @Nullable
    public RsNamedElement scopedMacroToPsi(@Nonnull RsModInfo info) {
        List<RsElement> scopes = containingModToScope(info);
        if (scopes.size() != 1) return null;
        RsElement scope = scopes.get(0);
        if (!(scope instanceof RsItemsOwner)) return null;

        RsCachedItems items =
            RsItemsOwnerUtil.getExpandedItemsCached(
                (RsItemsOwner) scope);

        // `macro_rules!` definitions are kept apart from the named items, so they have to be looked
        // up in their own list - the named map only ever holds macro 2.0 and proc-macro functions.
        List<RsMacro> legacyMacros = new java.util.ArrayList<>();
        for (RsMacro macro : items.getLegacyMacros()) {
            if (getName().equals(macro.getName()) && matchesIsEnabledByCfg(macro)) {
                legacyMacros.add(macro);
            }
        }
        if (!legacyMacros.isEmpty()) {
            return PathResolution.singlePublicOrFirstMacro(legacyMacros);
        }

        List<RsItemElement> named = items.getNamed().get(getName());
        if (named != null) {
            RsMacro2 single = null;
            for (RsItemElement item : named) {
                if (item instanceof RsMacro2 && matchesIsEnabledByCfg(item)) {
                    if (single != null) {
                        single = null;
                        break;
                    }
                    single = (RsMacro2) item;
                }
            }
            if (single != null) return single;
        }

        RsFunction procMacro = null;
        for (List<RsItemElement> group : items.getNamed().values()) {
            for (RsItemElement item : group) {
                if (!(item instanceof RsFunction)) continue;
                RsFunction fn = (RsFunction) item;
                if (!RsFunctionUtil.isProcMacroDef(fn)) continue;
                if (!getName().equals(RsFunctionUtil.getProcMacroName(fn))) continue;
                if (!matchesIsEnabledByCfg(fn)) continue;
                if (procMacro != null) return null;
                procMacro = fn;
            }
        }
        return procMacro;
    }

    /**
     * An import of a cfg-enabled item made from inside a cfg-disabled module is recorded with
     * cfg-disabled visibility, so in that case the PSI item's own cfg state must not be re-checked.
     */
    private boolean matchesIsEnabledByCfg(@Nonnull RsElement itemPsi) {
        if (visibility == Visibility.CFG_DISABLED) return true;
        return RsElementUtil.isEnabledByCfg(itemPsi);
    }

    /** The module or enum this item's own path names, resolved through the def map. */
    @Nonnull
    private List<RsNamedElement> pathToRsModOrEnum(@Nonnull RsModInfo info) {
        ModData data = findModData(info, path);
        if (data == null) return Collections.emptyList();

        List<RsNamedElement> result = new java.util.ArrayList<>();
        if (data.isEnum()) {
            ModData parent = data.getParent();
            if (parent == null) return Collections.emptyList();
            for (RsElement parentScope : modDataToScope(info, parent)) {
                if (!(parentScope instanceof RsItemsOwner)) continue;
                for (RsItemElement item :
                    namedItems((RsItemsOwner) parentScope, data.getName())) {
                    if (item instanceof RsEnumItem) result.add((RsNamedElement) item);
                }
            }
            return result;
        }

        for (RsElement scope : modDataToScope(info, data)) {
            if (scope instanceof RsNamedElement) result.add((RsNamedElement) scope);
        }
        return result;
    }

    /**
     * Returns the list of scopes corresponding to {@link #getContainingMod}. For the crate root,
     * returns the crate-root file. For a nested {@code mod foo}, returns the
     * {@link RsModItem} found by walking the path. For enum variants
     * whose containing path is an enum, returns the {@link RsEnumItem}.
     */
    /**
     * The modules (or enum) that hold this item, found through the def map rather than by walking the
     * PSI tree by name. The def map records the file each module lives in, so a module reached through
     * a {@code #[path]} attribute, a macro, or a deeply nested standard library layout still resolves -
     * a name walk from the crate root does not.
     */
    @Nonnull
    private List<RsElement> containingModToScope(@Nonnull RsModInfo info) {
        ModData containingModData = findModData(info, getContainingMod());
        if (containingModData == null) return Collections.emptyList();

        if (containingModData.isEnum()) {
            ModData parent = containingModData.getParent();
            if (parent == null) return Collections.emptyList();
            List<RsElement> enums = new java.util.ArrayList<>();
            for (RsElement parentScope : modDataToScope(info, parent)) {
                if (!(parentScope instanceof RsItemsOwner)) continue;
                for (RsItemElement item :
                    namedItems((RsItemsOwner) parentScope, containingModData.getName())) {
                    if (item instanceof RsEnumItem) enums.add(item);
                }
            }
            return enums;
        }

        return modDataToScope(info, containingModData);
    }

    /**
     * The items of {@code scope} carrying {@code name}, taken from the cached name map rather than by
     * walking every item in the scope. The standard library modules hold thousands of items, and a
     * resolve asks for exactly one name.
     */
    @Nonnull
    private static List<RsItemElement> namedItems(
        @Nonnull RsItemsOwner scope,
        @Nullable String name
    ) {
        if (name == null) return Collections.emptyList();
        List<RsItemElement> named =
            RsItemsOwnerUtil.getExpandedItemsCached(scope).getNamed().get(name);
        return named == null ? Collections.emptyList() : named;
    }

    @Nullable
    private static ModData findModData(@Nonnull RsModInfo info, @Nonnull ModPath path) {
        DataPsiHelper helper = info.getDataPsiHelper();
        if (helper != null) {
            ModData fromHelper = helper.findModData(path);
            if (fromHelper != null) return fromHelper;
        }
        return info.getDefMap().getModData(path);
    }

    @Nonnull
    private static List<RsElement> modDataToScope(
        @Nonnull RsModInfo info,
        @Nonnull ModData data
    ) {
        DataPsiHelper helper = info.getDataPsiHelper();
        if (helper != null) {
            RsMod fromHelper = helper.dataToPsi(data);
            if (fromHelper != null) return Collections.singletonList(fromHelper);
        }
        return new java.util.ArrayList<>(data.toRsMod(info.getProject()));
    }

    @Override
    @Nonnull
    public String toString() {
        return visibility + " " + path;
    }
}
