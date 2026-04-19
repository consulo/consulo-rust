/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.RsElementUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsFile;

import java.util.List;

public final class RsQualifiedNamedElementUtil {
    private RsQualifiedNamedElementUtil() {
    }

    /**
     * Always starts with crate root name.
     */
    @Nullable
    public static String getQualifiedName(@NotNull RsQualifiedNamedElement element) {
        String inCratePath = element.getCrateRelativePath();
        if (inCratePath == null) return null;
        Crate crate = Crate.asNotFake(RsElementUtil.getContainingCrate(element));
        if (crate == null) return null;
        String cargoTarget = crate.getNormName();
        return cargoTarget + inCratePath;
    }

    /**
     * Starts with 'crate' instead of crate root name if context is in same crate.
     */
    @Nullable
    public static String qualifiedNameInCrate(@NotNull RsQualifiedNamedElement element, @NotNull RsElement context) {
        String crateRelativePath = element.getCrateRelativePath();
        if (RsElementUtil.getCrateRoot(context) != RsElementUtil.getCrateRoot(element) || crateRelativePath == null) {
            return getQualifiedName(element);
        }
        return "crate" + crateRelativePath;
    }

    /**
     * If this is crate::inner1::inner2::foo and context is crate::inner1, then returns inner2::foo.
     */
    @Nullable
    public static String qualifiedNameRelativeTo(@NotNull RsQualifiedNamedElement element, @NotNull RsMod context) {
        String absolutePath = qualifiedNameInCrate(element, context);
        if (absolutePath == null) return null;
        List<RsMod> superMods = RsModExtUtil.getSuperMods(element.getContainingMod());
        if (!superMods.contains(context)) return absolutePath;
        return convertPathToRelativeIfPossible(context, absolutePath);
    }

    @NotNull
    public static String convertPathToRelativeIfPossible(@NotNull RsMod context, @NotNull String absolutePath) {
        String contextModPath = null;
        if (context instanceof RsQualifiedNamedElement) {
            contextModPath = ((RsQualifiedNamedElement) context).getCrateRelativePath();
        }
        if (contextModPath == null) return absolutePath;
        String contextModPathPrefix = "crate" + contextModPath + "::";
        if (!absolutePath.startsWith(contextModPathPrefix)) return absolutePath;
        String relativePath = absolutePath.substring(contextModPathPrefix.length());

        CargoWorkspace cargoWorkspace = RsElementUtil.getCargoWorkspace(context);
        if (cargoWorkspace != null) {
            for (CargoWorkspace.Package pkg : cargoWorkspace.getPackages()) {
                if (relativePath.startsWith(pkg.getNormName() + "::")) {
                    return "self::" + relativePath;
                }
            }
        }
        return relativePath;
    }
}
