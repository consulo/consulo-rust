/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.lang.core.psi.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.rust.lang.core.psi.ext.*;

/**
 * Extension utility methods for RsMod-related items.
 */
public final class RsModExtUtil {
    private RsModExtUtil() {}

    @Nonnull
    public static List<RsMod> getSuperMods(@Nonnull RsMod mod) {
        // For malformed programs, the chain of `super`s may be infinite because of cycles,
        // so we detect repetition and stop.
        Set<RsMod> visited = new HashSet<>();
        List<RsMod> result = new ArrayList<>();
        RsMod current = mod;
        while (current != null && visited.add(current)) {
            result.add(current);
            current = current.getSuper();
        }
        return result;
    }

    public static boolean getHasChildModules(@Nonnull RsMod mod) {
        return RsModUtil.getHasChildModules(mod);
    }

    @Nullable
    public static String getModName(@Nonnull RsMod mod) {
        return RsModUtil.getModName(mod);
    }

    @Nullable
    public static String getCrateRelativePath(@Nonnull RsMod mod) {
        return RsModUtil.getCrateRelativePath(mod);
    }

    @Nullable
    public static RsMod getContainingMod(@Nonnull PsiElement element) {
        return RsElementUtil.getContainingMod(element);
    }

    @Nullable
    public static RsMod getCrateRoot(@Nonnull RsMod mod) {
        List<RsMod> superMods = RsModExtUtil.getSuperMods(mod);
        return superMods.isEmpty() ? null : superMods.get(superMods.size() - 1);
    }

    @Nullable
    public static RsMod getSuper(@Nonnull RsMod mod) {
        return mod.getSuper();
    }

    @Nullable
    public static CargoProject getCargoProject(@Nonnull RsMod mod) {
        return RsElementExtUtil.getCargoProject(mod);
    }

    @Nullable
    public static CargoWorkspace.Target getContainingCargoTarget(@Nonnull PsiElement element) {
        return RsElementExtUtil.getContainingCargoTarget(element);
    }

    @Nonnull
    public static List<RsItemElement> exportedItems(@Nonnull RsMod mod, @Nullable RsMod containingMod) {
        return RsModUtil.exportedItems(mod, containingMod);
    }
}
