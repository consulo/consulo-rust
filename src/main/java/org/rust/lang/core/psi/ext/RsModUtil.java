/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.psi.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class RsModUtil {
    private RsModUtil() {
    }

    @NotNull
    public static List<RsMod> getSuperMods(@NotNull RsMod mod) {
        Set<RsMod> visited = new HashSet<>();
        List<RsMod> result = new ArrayList<>();
        RsMod current = mod;
        while (current != null && visited.add(current)) {
            result.add(current);
            current = current.getSuper();
        }
        return result;
    }

    public static boolean getHasChildModules(@NotNull RsMod mod) {
        for (RsItemElement item : RsItemsOwnerUtil.getExpandedItemsExceptImplsAndUses(mod)) {
            if (item instanceof RsModItem || item instanceof RsModDeclItem) return true;
        }
        return false;
    }

    /**
     * Alias for {@link #getHasChildModules} used by some callers.
     */
    public static boolean hasChildFiles(@NotNull RsMod mod) {
        return getHasChildModules(mod);
    }

    @NotNull
    public static List<RsMod> getChildModules(@NotNull RsMod mod) {
        List<RsMod> result = new ArrayList<>();
        for (RsItemElement item : RsItemsOwnerUtil.getExpandedItemsExceptImplsAndUses(mod)) {
            if (item instanceof RsModDeclItem) {
                PsiElement resolved = ((RsModDeclItem) item).getReference().resolve();
                if (resolved instanceof RsMod) result.add((RsMod) resolved);
            } else if (item instanceof RsModItem) {
                result.add((RsModItem) item);
            }
        }
        return result;
    }

    @Nullable
    public static RsMod getChildModule(@NotNull RsItemsOwner owner, @NotNull String name) {
        List<RsItemElement> elements = RsItemsOwnerUtil.getExpandedItemsCached(owner)
            .getNamedElementsIfCfgEnabled(name);
        if (elements == null) return null;
        for (RsItemElement element : elements) {
            if (element instanceof RsMod) return (RsMod) element;
        }
        return null;
    }

    @Nullable
    public static RsMod commonParentMod(@NotNull RsMod mod1, @NotNull RsMod mod2) {
        List<RsMod> superMods1 = getSuperMods(mod1);
        List<RsMod> superMods2 = getSuperMods(mod2);
        // Reverse them for comparison from crate root
        java.util.Collections.reverse(superMods1);
        java.util.Collections.reverse(superMods2);
        RsMod result = null;
        int minSize = Math.min(superMods1.size(), superMods2.size());
        for (int i = 0; i < minSize; i++) {
            if (superMods1.get(i).equals(superMods2.get(i))) {
                result = superMods1.get(i);
            }
        }
        return result;
    }

    // Delegating convenience methods from RsMod interface / RsElement:

    public static boolean isCrateRoot(@NotNull RsMod mod) {
        return mod.isCrateRoot();
    }

    @Nullable
    public static String getModName(@NotNull RsMod mod) {
        return mod.getModName();
    }

    @Nullable
    public static String getModName(@NotNull RsModItem modItem) {
        return modItem.getModName();
    }

    @Nullable
    public static PsiDirectory getOwnedDirectory(@NotNull RsMod mod) {
        return mod.getOwnedDirectory(false);
    }

    @Nullable
    public static PsiDirectory getOwnedDirectory(@NotNull RsMod mod, boolean createIfNotExists) {
        return mod.getOwnedDirectory(createIfNotExists);
    }

    @Nullable
    public static String getCrateRelativePath(@NotNull RsMod mod) {
        return mod instanceof RsQualifiedNamedElement ? ((RsQualifiedNamedElement) mod).getCrateRelativePath() : null;
    }

    @Nullable
    public static CargoWorkspace getCargoWorkspace(@NotNull RsMod mod) {
        return RsElementUtil.getCargoWorkspace(mod);
    }

    @NotNull
    public static List<RsItemElement> getExpandedItemsExceptImplsAndUses(@NotNull RsMod mod) {
        return RsItemsOwnerUtil.getExpandedItemsExceptImplsAndUses(mod);
    }

    public static boolean processExpandedItemsExceptImplsAndUses(@NotNull RsMod mod, @NotNull Function<RsItemElement, Boolean> processor) {
        for (RsItemElement item : RsItemsOwnerUtil.getExpandedItemsExceptImplsAndUses(mod)) {
            if (processor.apply(item)) return true;
        }
        return false;
    }

    @Nullable
    public static PsiElement getFirstItem(@NotNull RsMod mod) {
        return RsItemsOwnerUtil.getFirstItem(mod);
    }

    /**
     * If {@code this} is {@code crate::inner1::inner2::foo} and {@code context} is {@code crate::inner1},
     * then returns {@code inner2::foo}.
     */
    @Nullable
    public static String qualifiedNameRelativeTo(@NotNull RsQualifiedNamedElement element, @NotNull RsMod context) {
        return RsQualifiedNamedElementUtil.qualifiedNameRelativeTo(element, context);
    }

    @Nullable
    public static RsMod getCrateRoot(@NotNull RsMod mod) {
        List<RsMod> superMods = getSuperMods(mod);
        return superMods.isEmpty() ? mod : superMods.get(superMods.size() - 1);
    }

    @Nullable
    public static org.rust.cargo.project.model.CargoProject getCargoProject(@NotNull RsMod mod) {
        return org.rust.cargo.project.model.CargoProjectServiceUtil.getCargoProjects(mod.getProject())
            .findProjectForFile(mod.getContainingFile().getVirtualFile());
    }

    @Nullable
    public static CargoWorkspace.Target getContainingCargoTarget(@NotNull RsMod mod) {
        org.rust.lang.core.crate.Crate crate = RsElementUtil.getContainingCrate(mod);
        if (crate == null) return null;
        return crate.getCargoTarget();
    }

    @NotNull
    public static List<RsItemElement> exportedItems(@NotNull RsMod mod, @Nullable RsMod context) {
        List<RsItemElement> result = new ArrayList<>();
        for (RsItemElement item : RsItemsOwnerUtil.getExpandedItemsExceptImplsAndUses(mod)) {
            if (item instanceof RsVisible && context != null) {
                if (RsVisibilityUtil.isVisibleFrom((RsVisible) item, context)) {
                    result.add(item);
                }
            } else {
                result.add(item);
            }
        }
        return result;
    }
}
