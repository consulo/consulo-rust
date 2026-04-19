/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.completion.CompletionUtil;
import org.rust.lang.core.macros.decl.DeclMacroExpander;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve2.RsModInfo;
import org.rust.lang.core.resolve2.FacadeResolve;

import java.util.Set;

public class PathResolutionContext {
    @NotNull
    private final RsElement context;
    private final boolean isCompletion;
    private final boolean processAssocItems;
    @Nullable
    private final ImplLookup givenImplLookup;

    @Nullable
    private final RsMod crateRoot;
    private RsMod containingMod;
    private boolean containingModInitialized;
    private RsModInfo containingModInfo;
    private boolean containingModInfoInitialized;
    private ImplLookup implLookup;

    public PathResolutionContext(
        @NotNull RsElement context,
        boolean isCompletion,
        boolean processAssocItems,
        @Nullable ImplLookup givenImplLookup
    ) {
        this.context = context;
        this.isCompletion = isCompletion;
        this.processAssocItems = processAssocItems;
        this.givenImplLookup = givenImplLookup;
        this.crateRoot = context.getCrateRoot();
    }

    @NotNull
    public RsElement getContext() {
        return context;
    }

    public boolean isCompletion() {
        return isCompletion;
    }

    public boolean isProcessAssocItems() {
        return processAssocItems;
    }

    @Nullable
    public RsMod getCrateRoot() {
        return crateRoot;
    }

    @NotNull
    public RsMod getContainingMod() {
        if (!containingModInitialized) {
            containingMod = context.getContainingMod();
            containingModInitialized = true;
        }
        return containingMod;
    }

    @Nullable
    public RsModInfo getContainingModInfo() {
        if (!containingModInfoInitialized) {
            containingModInfo = FacadeResolve.getModInfo(getContainingMod());
            containingModInfoInitialized = true;
        }
        return containingModInfo;
    }

    @NotNull
    public ImplLookup getImplLookup() {
        if (implLookup == null) {
            implLookup = givenImplLookup != null ? givenImplLookup : ImplLookup.relativeTo(context);
        }
        return implLookup;
    }

    public boolean isAtLeastEdition2018() {
        CargoWorkspace.Edition edition = crateRoot != null ? RsElementExtUtil.getEdition(crateRoot) : CargoWorkspace.Edition.DEFAULT;
        return edition.compareTo(CargoWorkspace.Edition.EDITION_2018) >= 0;
    }

    @Nullable
    public RsModInfo getContainingModInfo(@NotNull RsMod knownContainingMod) {
        if (containingModInfoInitialized) {
            return containingModInfo;
        }
        RsMod original = CompletionUtil.getOriginalOrSelf(knownContainingMod);
        if (containingModInitialized) {
            assert containingMod == original;
        }
        RsModInfo modInfo = FacadeResolve.getModInfo(original);
        containingMod = original;
        containingModInitialized = true;
        containingModInfo = modInfo;
        containingModInfoInitialized = true;
        return modInfo;
    }

    @NotNull
    public RsPathResolveKind classifyPath(@NotNull RsPath path) {
        PsiElement parent = PsiElementUtil.getStubParent(path);
        if (parent instanceof RsMacroCall) {
            throw new IllegalStateException("Tried to use processPathResolveVariants for macro path. See RsMacroPathReferenceImpl");
        }
        if (parent instanceof RsAssocTypeBinding) {
            return new RsPathResolveKind.AssocTypeBindingPath((RsAssocTypeBinding) parent);
        }

        RsPath qualifier = RsPathUtil.getQualifier(path);
        RsTypeQual typeQual = path.getTypeQual();
        Set<Namespace> ns = NameResolutionUtil.allowedNamespaces(path);

        if (qualifier != null) {
            return new RsPathResolveKind.QualifiedPath(path, ns, qualifier, parent);
        }
        if (typeQual != null) {
            return new RsPathResolveKind.ExplicitTypeQualifiedPath(ns, typeQual);
        }
        return classifyUnqualifiedPath(path, ns);
    }

    @NotNull
    private RsPathResolveKind classifyUnqualifiedPath(@NotNull RsPath path, @NotNull Set<Namespace> ns) {
        boolean hasColonColon = path.getHasColonColon();
        if (!hasColonColon) {
            String referenceName = path.getReferenceName();
            if (DeclMacroExpander.MACRO_DOLLAR_CRATE_IDENTIFIER.equals(referenceName)) {
                return new RsPathResolveKind.MacroDollarCrateIdentifier(path);
            }
        }

        boolean isEdition2018 = isAtLeastEdition2018();

        boolean isCrateRelative = (!isEdition2018 && (hasColonColon || rootPathParentIsUseSpeck(path)))
            || rootPathParentIsVisRestriction(path);
        boolean isExternCrate = isEdition2018 && hasColonColon;

        if (isCrateRelative) {
            return new RsPathResolveKind.CrateRelativePath(path, ns, hasColonColon);
        }
        if (isExternCrate) {
            return RsPathResolveKind.ExternCratePath.INSTANCE;
        }
        return new RsPathResolveKind.UnqualifiedPath(ns);
    }

    private static boolean rootPathParentIsUseSpeck(@NotNull RsPath path) {
        RsPath root = path;
        while (root.getPath() != null) {
            root = root.getPath();
        }
        return root.getParent() instanceof RsUseSpeck;
    }

    private static boolean rootPathParentIsVisRestriction(@NotNull RsPath path) {
        RsPath root = path;
        while (root.getPath() != null) {
            root = root.getPath();
        }
        return root.getParent() instanceof RsVisRestriction;
    }
}
