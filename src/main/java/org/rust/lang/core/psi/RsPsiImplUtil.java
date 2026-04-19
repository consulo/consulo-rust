/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.crate.impl.FakeCrate;
import org.rust.lang.core.macros.MacroExpansionUtil;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve2.FacadeMetaInfo;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;

import java.util.List;
import org.rust.lang.core.psi.ext.RsTraitRefUtil;

/**
 * Mixin methods to implement PSI interfaces without copy pasting and
 * introducing monster base classes.
 */
public final class RsPsiImplUtil {
    public static final RsPsiImplUtil INSTANCE = new RsPsiImplUtil();

    private RsPsiImplUtil() {
    }

    @Nullable
    public static String crateRelativePath(@NotNull RsNamedElement element) {
        String name = element.getName();
        if (name == null) return null;
        String qualifier = RsModUtil.getCrateRelativePath(element.getContainingMod());
        if (qualifier == null) return null;
        return qualifier + "::" + name;
    }

    @Nullable
    public static String modCrateRelativePath(@NotNull RsMod mod) {
        List<RsMod> superMods = RsModExtUtil.getSuperMods(mod);
        if (superMods.size() <= 1) return "";
        StringBuilder sb = new StringBuilder("::");
        for (int i = superMods.size() - 2; i >= 0; i--) {
            String modName = superMods.get(i).getModName();
            if (modName == null) return null;
            if (i < superMods.size() - 2) sb.append("::");
            sb.append(modName);
        }
        return sb.toString();
    }

    @Nullable
    public static SearchScope getParameterUseScope(@NotNull RsElement element) {
        RsGenericDeclaration owner = PsiTreeUtil.getContextOfType(element, RsGenericDeclaration.class);
        if (owner != null) return localOrMacroSearchScope(owner);
        return null;
    }

    @Nullable
    public static SearchScope getDeclarationUseScope(@NotNull RsVisible element) {
        return getDeclarationUseScope(element, RsVisibility.Public.INSTANCE);
    }

    @Nullable
    private static SearchScope getDeclarationUseScope(@NotNull RsVisible element, @NotNull RsVisibility restrictedVis) {
        if (element instanceof RsEnumVariant) {
            return getDeclarationUseScope(RsEnumVariantUtil.getParentEnum((RsEnumVariant) element), restrictedVis);
        }
        if (element instanceof RsFieldDecl) {
            RsFieldsOwner fieldsOwner = PsiTreeUtil.getContextOfType(element, RsFieldsOwner.class);
            if (fieldsOwner instanceof RsVisible) {
                return getDeclarationUseScope((RsVisible) fieldsOwner, ((RsFieldDecl) element).getVisibility());
            }
        }

        PsiElement owner = PsiTreeUtil.getContextOfType(element, true, RsItemElement.class, RsMod.class);
        if (!(owner instanceof RsElement)) return null;

        if (owner instanceof RsTraitItem) {
            return getDeclarationUseScope((RsTraitItem) owner);
        } else if (owner instanceof RsImplItem) {
            RsTraitRef traitRef = ((RsImplItem) owner).getTraitRef();
            if (traitRef != null) {
                RsTraitItem trait = RsTraitRefUtil.resolveToTrait(traitRef);
                if (trait != null) return getDeclarationUseScope(trait);
                return null;
            }
            return getTopLevelDeclarationUseScope(element, ((RsImplItem) owner).getContainingMod(), restrictedVis);
        } else if (owner instanceof RsMod) {
            return getTopLevelDeclarationUseScope(element, (RsMod) owner, restrictedVis);
        } else if (owner instanceof RsForeignModItem) {
            return getTopLevelDeclarationUseScope(element, ((RsForeignModItem) owner).getContainingMod(), restrictedVis);
        } else {
            return localOrMacroSearchScope((RsElement) owner);
        }
    }

    @Nullable
    private static SearchScope getTopLevelDeclarationUseScope(
        @NotNull RsVisible element,
        @NotNull RsMod containingMod,
        @NotNull RsVisibility restrictedVis
    ) {
        RsVisibility visibility = RsVisibilityUtil.intersect(restrictedVis, element.getVisibility());
        RsMod restrictedMod;
        if (visibility instanceof RsVisibility.Public) {
            org.rust.lang.core.crate.Crate crate = RsElementUtil.getContainingCrate(containingMod);
            if (crate instanceof FakeCrate || crate.getKind() instanceof CargoWorkspace.TargetKind.Lib) return null;
            restrictedMod = crate.getRootMod();
            if (restrictedMod == null) return null;
        } else if (visibility instanceof RsVisibility.Private) {
            restrictedMod = containingMod;
        } else if (visibility instanceof RsVisibility.Restricted) {
            restrictedMod = ((RsVisibility.Restricted) visibility).getInMod();
        } else {
            return null;
        }

        LocalSearchScope restrictedModScope = localOrMacroSearchScope(restrictedMod);
        if (!RsModUtil.hasChildFiles(restrictedMod)) return restrictedModScope;

        com.intellij.openapi.vfs.VirtualFile containedDirectory =
            org.rust.lang.core.resolve2.FacadeMetaInfo.getDirectoryContainedAllChildFiles(restrictedMod);
        if (containedDirectory == null) return null;
        SearchScope containedDirectoryScope =
            GlobalSearchScopes.directoryScope(element.getProject(), containedDirectory, true);
        return containedDirectoryScope.union(restrictedModScope);
    }

    @NotNull
    public static LocalSearchScope localOrMacroSearchScope(@NotNull PsiElement scope) {
        PsiElement expanded = MacroExpansionUtil.findMacroCallExpandedFrom(scope);
        return new LocalSearchScope(expanded != null ? expanded : scope);
    }

    /**
     * Checks if the element is being used in an intention preview context.
     * Delegates to {@link PsiElementExt#isIntentionPreviewElement(PsiElement)}.
     */
    public static boolean isIntentionPreviewElement(@NotNull PsiElement element) {
        return PsiElementExt.isIntentionPreviewElement(element);
    }
}
