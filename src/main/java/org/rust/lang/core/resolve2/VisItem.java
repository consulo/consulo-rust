/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.resolve.Namespace;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The item which can be visible in the module (either directly declared or imported).
 * Could be RsEnumVariant (because it can be imported).
 */
public final class VisItem {

    public static final VisItem[] EMPTY_ARRAY = new VisItem[0];

    @NotNull
    private final ModPath path;
    @NotNull
    private final Visibility visibility;
    private final boolean isModOrEnum;
    private final boolean isTrait;
    /**
     * Records whether this item was added to mod scope with named or glob import.
     * Needed to determine whether we can override it (usual imports overrides glob-imports).
     * Used only in DefCollector, but stored here as an optimization.
     */
    private final boolean isFromNamedImport;

    public VisItem(@NotNull ModPath path, @NotNull Visibility visibility) {
        this(path, visibility, false, false, true);
    }

    public VisItem(@NotNull ModPath path, @NotNull Visibility visibility, boolean isModOrEnum) {
        this(path, visibility, isModOrEnum, false, true);
    }

    public VisItem(@NotNull ModPath path, @NotNull Visibility visibility, boolean isModOrEnum, boolean isTrait) {
        this(path, visibility, isModOrEnum, isTrait, true);
    }

    public VisItem(@NotNull ModPath path, @NotNull Visibility visibility, boolean isModOrEnum, boolean isTrait, boolean isFromNamedImport) {
        this.path = path;
        this.visibility = visibility;
        this.isModOrEnum = isModOrEnum;
        this.isTrait = isTrait;
        this.isFromNamedImport = isFromNamedImport;
        if (!isModOrEnum && path.getSegments().length == 0) {
            throw new IllegalStateException("VisItem check failed: isModOrEnum || path.segments.isNotEmpty()");
        }
    }

    @NotNull
    public ModPath getPath() {
        return path;
    }

    @NotNull
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
    @NotNull
    public ModPath getContainingMod() {
        return path.getParent();
    }

    @NotNull
    public String getName() {
        return path.getName();
    }

    public int getCrate() {
        return path.getCrate();
    }

    public boolean isCrateRoot() {
        return path.getSegments().length == 0;
    }

    @NotNull
    public VisItem adjust(@NotNull Visibility visibilityNew, boolean isFromNamedImport) {
        return new VisItem(
            path,
            visibilityNew.intersect(visibility),
            isModOrEnum,
            isTrait,
            isFromNamedImport
        );
    }

    @NotNull
    public VisItem copy(@NotNull Visibility visibility, boolean isFromNamedImport) {
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
    @NotNull
    public List<RsNamedElement> toPsi(@NotNull RsModInfo info, @NotNull Namespace namespace) {
        if (isModOrEnum()) {
            return pathToRsModOrEnum(info);
        }
        // Regular item: find the containing module/enum and pick items with matching name.
        List<org.rust.lang.core.psi.ext.RsElement> scopes = containingModToScope(info);
        List<RsNamedElement> result = new java.util.ArrayList<>();
        for (org.rust.lang.core.psi.ext.RsElement scope : scopes) {
            if (scope instanceof org.rust.lang.core.psi.RsEnumItem) {
                org.rust.lang.core.psi.RsEnumBody body = ((org.rust.lang.core.psi.RsEnumItem) scope).getEnumBody();
                if (body == null) continue;
                for (org.rust.lang.core.psi.RsEnumVariant variant : body.getEnumVariantList()) {
                    if (getName().equals(variant.getName())) result.add(variant);
                }
            } else if (scope instanceof org.rust.lang.core.psi.ext.RsItemsOwner) {
                for (org.rust.lang.core.psi.ext.RsItemElement item :
                    org.rust.lang.core.psi.ext.RsItemsOwnerUtil.getExpandedItemsExceptImplsAndUses(
                        (org.rust.lang.core.psi.ext.RsItemsOwner) scope)) {
                    if (!(item instanceof RsNamedElement)) continue;
                    if (!getName().equals(((RsNamedElement) item).getName())) continue;
                    result.add((RsNamedElement) item);
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
    public RsNamedElement scopedMacroToPsi(@NotNull RsModInfo info) {
        List<org.rust.lang.core.psi.ext.RsElement> scopes = containingModToScope(info);
        if (scopes.size() != 1) return null;
        org.rust.lang.core.psi.ext.RsElement scope = scopes.get(0);
        if (!(scope instanceof org.rust.lang.core.psi.ext.RsItemsOwner)) return null;
        for (org.rust.lang.core.psi.ext.RsItemElement item :
            org.rust.lang.core.psi.ext.RsItemsOwnerUtil.getExpandedItemsExceptImplsAndUses(
                (org.rust.lang.core.psi.ext.RsItemsOwner) scope)) {
            if (!(item instanceof RsNamedElement)) continue;
            if (!getName().equals(((RsNamedElement) item).getName())) continue;
            if (item instanceof org.rust.lang.core.psi.RsMacro
                || item instanceof org.rust.lang.core.psi.RsMacro2) {
                return (RsNamedElement) item;
            }
            if (item instanceof org.rust.lang.core.psi.RsFunction
                && org.rust.lang.core.psi.ext.RsFunctionUtil.isProcMacroDef((org.rust.lang.core.psi.RsFunction) item)) {
                return (RsNamedElement) item;
            }
        }
        return null;
    }

    /** Walk from the crate root down the path, returning any {@link org.rust.lang.core.psi.ext.RsMod} or {@link org.rust.lang.core.psi.RsEnumItem}. */
    @NotNull
    private List<RsNamedElement> pathToRsModOrEnum(@NotNull RsModInfo info) {
        org.rust.lang.core.crate.CrateGraphService graph =
            org.rust.lang.core.crate.CrateGraphService.crateGraph(info.getProject());
        org.rust.lang.core.crate.Crate target = graph.findCrateById(path.getCrate());
        if (target == null) return Collections.emptyList();
        org.rust.lang.core.psi.RsFile crateRoot = target.getRootMod();
        if (crateRoot == null) return Collections.emptyList();
        org.rust.lang.core.psi.ext.RsItemsOwner current = crateRoot;
        String[] segments = path.getSegments();
        if (segments.length == 0) {
            return Collections.singletonList(crateRoot);
        }
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            boolean isLast = i == segments.length - 1;
            org.rust.lang.core.psi.ext.RsItemsOwner next = null;
            for (org.rust.lang.core.psi.ext.RsItemElement item :
                org.rust.lang.core.psi.ext.RsItemsOwnerUtil.getExpandedItemsExceptImplsAndUses(current)) {
                if (!(item instanceof RsNamedElement) || !segment.equals(((RsNamedElement) item).getName())) continue;
                if (item instanceof org.rust.lang.core.psi.ext.RsMod) {
                    next = (org.rust.lang.core.psi.ext.RsMod) item;
                    break;
                }
                if (isLast && item instanceof org.rust.lang.core.psi.RsEnumItem) {
                    return Collections.singletonList((RsNamedElement) item);
                }
            }
            if (next == null) return Collections.emptyList();
            current = next;
        }
        return current instanceof RsNamedElement
            ? Collections.singletonList((RsNamedElement) current)
            : Collections.emptyList();
    }

    /**
     * Returns the list of scopes corresponding to {@link #getContainingMod}. For the crate root,
     * returns the crate-root file. For a nested {@code mod foo}, returns the
     * {@link org.rust.lang.core.psi.RsModItem} found by walking the path. For enum variants
     * whose containing path is an enum, returns the {@link org.rust.lang.core.psi.RsEnumItem}.
     */
    @NotNull
    private List<org.rust.lang.core.psi.ext.RsElement> containingModToScope(@NotNull RsModInfo info) {
        ModPath containingPath = getContainingMod();
        org.rust.lang.core.crate.CrateGraphService graph =
            org.rust.lang.core.crate.CrateGraphService.crateGraph(info.getProject());
        org.rust.lang.core.crate.Crate target = graph.findCrateById(containingPath.getCrate());
        if (target == null) return Collections.emptyList();
        org.rust.lang.core.psi.RsFile crateRoot = target.getRootMod();
        if (crateRoot == null) return Collections.emptyList();
        org.rust.lang.core.psi.ext.RsElement current = crateRoot;
        for (String segment : containingPath.getSegments()) {
            if (!(current instanceof org.rust.lang.core.psi.ext.RsItemsOwner)) return Collections.emptyList();
            org.rust.lang.core.psi.ext.RsElement next = null;
            for (org.rust.lang.core.psi.ext.RsItemElement item :
                org.rust.lang.core.psi.ext.RsItemsOwnerUtil.getExpandedItemsExceptImplsAndUses(
                    (org.rust.lang.core.psi.ext.RsItemsOwner) current)) {
                if (!(item instanceof RsNamedElement) || !segment.equals(((RsNamedElement) item).getName())) continue;
                if (item instanceof org.rust.lang.core.psi.ext.RsMod
                    || item instanceof org.rust.lang.core.psi.RsEnumItem) {
                    next = item;
                    break;
                }
            }
            if (next == null) return Collections.emptyList();
            current = next;
        }
        return Collections.singletonList(current);
    }

    @Override
    @NotNull
    public String toString() {
        return visibility + " " + path;
    }
}
