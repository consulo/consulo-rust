/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.imports;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.util.AutoInjectedCrates;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.Comparator;
import java.util.List;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;

/**
 *
 * (Java reserved keyword). This Java class in the 'imports' (plural) package
 * via bridge classes where needed.
 *
 * Key functions:
 * - importCandidate: inserts a use declaration for an ImportCandidate
 * - insertExternCrateIfNeeded: inserts extern crate item if needed
 * - insertUseItem: inserts a use item at the correct location
 * - stdlibAttributes: gets stdlib attributes for an element
 * - isStd / isCore: checks crate origin
 * - Testmarks: test marker objects
 * - createVirtualImportContext: creates a fake mod for completion
 * - COMPARATOR_FOR_SPECKS_IN_USE_GROUP: comparator for sorting use specks
 */
public final class ImportUtils {
    private ImportUtils() {
    }

    /**
     * Returns true if the crate is std from stdlib.
     */
    public static boolean isStd(@NotNull Crate crate) {
        return crate.getOrigin() == PackageOrigin.STDLIB
            && AutoInjectedCrates.STD.equals(crate.getNormName());
    }

    /**
     * Returns true if the crate is core from stdlib.
     */
    public static boolean isCore(@NotNull Crate crate) {
        return crate.getOrigin() == PackageOrigin.STDLIB
            && AutoInjectedCrates.CORE.equals(crate.getNormName());
    }

    /**
     * Gets the stdlib attributes for an RsElement.
     */
    @NotNull
    public static RsFile.Attributes getStdlibAttributes(@NotNull RsElement element) {
        return ImportBridge.getStdlibAttributes(element);
    }

    /**
     * Returns the last element of a list of RsElements by text offset.
     */
    @Nullable
    public static <T extends RsElement> T lastElement(@NotNull List<T> list) {
        if (list.isEmpty()) return null;
        T result = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            T item = list.get(i);
            if (item.getTextOffset() > result.getTextOffset()) {
                result = item;
            }
        }
        return result;
    }

    /**
     * Inserts a use declaration for the given ImportCandidate at the location of the context element.
     * Delegates to ImportBridge.importCandidate.
     *
     * @param candidate an ImportCandidate instance
     * @param context   the PSI element where the import should be added
     */
    public static void importCandidate(@NotNull ImportCandidate candidate, @NotNull RsElement context) {
        ImportBridge.importCandidate(candidate, context);
    }

    /**
     * Creates a fake mod that can be used for completion in code fragments to avoid importing
     * types into the real mod of the element.
     *
     * @param element the element whose containing mod is used as context
     * @return a synthetic RsMod with a wildcard use item for the containing mod's path
     */
    @NotNull
    public static RsMod createVirtualImportContext(@NotNull RsElement element) {
        RsPsiFactory factory = new RsPsiFactory(element.getProject());
        RsFile sourceContext = (RsFile) element.getContainingFile();
        RsMod containingMod = RsElementUtil.getContainingMod(element);
        String qualifiedPath = containingMod != null
            ? RsQualifiedNamedElementUtil.qualifiedNameInCrate(containingMod, element)
            : null;
        String defaultUseItem = qualifiedPath != null
            ? "use " + qualifiedPath + "::*;"
            : "";
        RsModItem module = factory.createModItem("_virtual_import_context_", defaultUseItem);
        RsExpandedElementUtil.setContext(module, sourceContext);
        return module;
    }

    /**
     * Inserts a use item into the given module at the correct location.
     *
     * {@code RsItemsOwner.insertUseItem(psiFactory: RsPsiFactory, useItem: RsUseItem)}.
     *
     * @param mod        the module to insert the use item into
     * @param psiFactory a PSI factory for creating whitespace elements
     * @param useItem    the use item to insert
     */
    public static void insertUseItem(@NotNull RsMod mod, @NotNull RsPsiFactory psiFactory, @NotNull RsUseItem useItem) {
        if (tryInsertUseItemAtCorrectLocation(mod, useItem)) return;

        // Handle case when mod is empty or has no uses / extern crates
        com.intellij.psi.PsiElement firstItem = RsModUtil.getFirstItem(mod);
        if (mod instanceof RsModItem && !RsItemsOwnerUtil.getItemsAndMacros((RsItemsOwner) mod).iterator().hasNext()) {
            mod.addBefore(useItem, ((RsModItem) mod).getRbrace());
        } else {
            mod.addBefore(useItem, firstItem);
        }
        mod.addAfter(psiFactory.createNewline(), RsModUtil.getFirstItem(mod));
    }

    private static boolean tryInsertUseItemAtCorrectLocation(@NotNull RsMod mod, @NotNull RsUseItem useItem) {
        RsPsiFactory newline = new RsPsiFactory(mod.getProject());
        List<RsUseItem> uses = RsElementUtil.childrenOfType(mod, RsUseItem.class);
        if (uses.isEmpty()) {
            List<RsExternCrateItem> externCrates = RsElementUtil.childrenOfType(mod, RsExternCrateItem.class);
            if (externCrates.isEmpty()) return false;
            com.intellij.psi.PsiElement anchor = externCrates.get(externCrates.size() - 1);
            mod.addBefore(newline.createNewline(), mod.addAfter(useItem, anchor));
            return true;
        }

        UseItemWrapper useWrapper = new UseItemWrapper(useItem);
        UseItemWrapper anchorBefore = null;
        UseItemWrapper anchorAfter = null;
        for (RsUseItem u : uses) {
            UseItemWrapper w = new UseItemWrapper(u);
            if (w.compareTo(useWrapper) < 0) {
                if (anchorBefore == null || w.compareTo(anchorBefore) > 0) {
                    anchorBefore = w;
                }
            } else {
                if (anchorAfter == null || w.compareTo(anchorAfter) < 0) {
                    anchorAfter = w;
                }
            }
        }

        if (anchorBefore != null) {
            com.intellij.psi.PsiElement addedItem = mod.addAfter(useItem, anchorBefore.getUseItem());
            mod.addBefore(newline.createNewline(), addedItem);
        } else if (anchorAfter != null) {
            com.intellij.psi.PsiElement addedItem = mod.addBefore(useItem, anchorAfter.getUseItem());
            mod.addAfter(newline.createNewline(), addedItem);
        } else {
            return false;
        }
        return true;
    }

    /**
     * Comparator for sorting use specks within a use group.
     * Items with 'self' come last; otherwise sorted by path text (lowercase).
     */
    @NotNull
    public static Comparator<RsUseSpeck> getCOMPARATOR_FOR_SPECKS_IN_USE_GROUP() {
        return COMPARATOR_FOR_SPECKS_IN_USE_GROUP;
    }

    private static final Comparator<RsUseSpeck> COMPARATOR_FOR_SPECKS_IN_USE_GROUP =
        Comparator.<RsUseSpeck, Boolean>comparing(speck -> speck.getPath() == null || speck.getPath().getSelf() == null)
            .thenComparing(speck -> {
                RsPath path = speck.getPath();
                return path != null && path.getText() != null ? path.getText().toLowerCase() : "";
            });
}
