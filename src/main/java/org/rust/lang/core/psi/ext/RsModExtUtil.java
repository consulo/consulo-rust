/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extension utility methods for RsMod-related items.
 */
public final class RsModExtUtil {
    private RsModExtUtil() {}

    @NotNull
    public static List<RsMod> getSuperMods(@NotNull RsMod mod) {
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

    public static boolean getHasChildModules(@NotNull RsMod mod) {
        return RsModUtil.getHasChildModules(mod);
    }

    @Nullable
    public static String getModName(@NotNull RsMod mod) {
        return RsModUtil.getModName(mod);
    }

    @Nullable
    public static String getCrateRelativePath(@NotNull RsMod mod) {
        return RsModUtil.getCrateRelativePath(mod);
    }

    @Nullable
    public static RsMod getContainingMod(@NotNull PsiElement element) {
        return RsElementUtil.getContainingMod(element);
    }

    @Nullable
    public static RsMod getCrateRoot(@NotNull RsMod mod) {
        List<RsMod> superMods = RsModExtUtil.getSuperMods(mod);
        return superMods.isEmpty() ? null : superMods.get(superMods.size() - 1);
    }

    @Nullable
    public static RsMod getSuper(@NotNull RsMod mod) {
        return mod.getSuper();
    }

    @Nullable
    public static CargoProject getCargoProject(@NotNull RsMod mod) {
        return RsElementExtUtil.getCargoProject(mod);
    }

    @Nullable
    public static CargoWorkspace.Target getContainingCargoTarget(@NotNull PsiElement element) {
        return RsElementExtUtil.getContainingCargoTarget(element);
    }

    @NotNull
    public static List<RsItemElement> exportedItems(@NotNull RsMod mod, @Nullable RsMod containingMod) {
        return RsModUtil.exportedItems(mod, containingMod);
    }
}
