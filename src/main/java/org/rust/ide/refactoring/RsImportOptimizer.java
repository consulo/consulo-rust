/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.lang.ImportOptimizer;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.inspections.lints.PathUsageMap;
import org.rust.ide.inspections.lints.RsUnusedImportInspection;
import org.rust.ide.inspections.lints.UseSpeckUsageUtil;
import org.rust.ide.inspections.lints.PathUsageUtil;
import org.rust.ide.utils.imports.ImportUtils;
import org.rust.ide.utils.imports.UseItemWrapper;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.doc.psi.RsDocComment;
import org.rust.stdext.SequenceExtUtil;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsUseSpeckUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;

public class RsImportOptimizer implements ImportOptimizer {

    @Override
    public boolean supports(@NotNull PsiFile file) {
        return file instanceof RsFile;
    }

    @NotNull
    @Override
    public Runnable processFile(@NotNull PsiFile file) {
        return () -> {
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(file.getProject());
            com.intellij.openapi.editor.Document document = documentManager.getDocument(file);
            if (document != null) {
                documentManager.commitDocument(document);
            }
            optimizeAndReorderUseItems((RsFile) file);
            reorderExternCrates((RsFile) file);
        };
    }

    private void reorderExternCrates(@NotNull RsFile file) {
        RsElement first = RsItemsOwnerUtil.getFirstItem(file);
        if (first == null) return;
        List<RsExternCrateItem> externCrateItems = RsElementUtil.childrenOfType(file, RsExternCrateItem.class);
        List<RsExternCrateItem> sorted = externCrateItems.stream()
            .sorted(Comparator.comparing(RsExternCrateItem::getReferenceName, Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());
        for (RsExternCrateItem item : sorted) {
            RsExternCrateItem copy = (RsExternCrateItem) item.copy();
            if (copy != null) {
                file.addBefore(copy, first);
            }
        }
        for (RsExternCrateItem item : externCrateItems) {
            item.delete();
        }
    }

    private void optimizeAndReorderUseItems(@NotNull RsFile file) {
        RsPsiFactory factory = new RsPsiFactory(file.getProject());
        forEachScope(file, (scope, uses, pathUsage) -> {
            if (scope instanceof RsMod) {
                replaceOrderOfUseItems((RsItemsOwner) scope, uses, pathUsage);
            } else if (scope instanceof RsBlock) {
                for (RsUseItem useItem : uses) {
                    optimizeUseItem(useItem, factory, pathUsage);
                }
            }
        });
    }

    public static void optimizeUseItems(@NotNull RsFile file) {
        RsPsiFactory factory = new RsPsiFactory(file.getProject());
        forEachScope(file, (scope, uses, pathUsage) -> {
            if (scope instanceof RsMod) {
                for (RsUseItem useItem : uses) {
                    optimizeUseItem(useItem, factory, pathUsage);
                }
            }
        });
    }

    private static void optimizeUseItem(@NotNull RsUseItem useItem, @NotNull RsPsiFactory factory, @org.jetbrains.annotations.Nullable PathUsageMap pathUsage) {
        RsUseSpeck useSpeck = useItem.getUseSpeck();
        if (useSpeck == null) return;
        boolean used = optimizeUseSpeck(useSpeck, factory, pathUsage);
        if (!used) {
            if (useItem.getNextSibling() instanceof PsiWhiteSpace) {
                useItem.getNextSibling().delete();
            }
            useItem.delete();
        }
    }

    /**
     * Returns false if useSpeck is empty and should be removed
     */
    private static boolean optimizeUseSpeck(
        @NotNull RsUseSpeck useSpeck,
        @NotNull RsPsiFactory factory,
        @org.jetbrains.annotations.Nullable PathUsageMap pathUsage
    ) {
        RsUseGroup useGroup = useSpeck.getUseGroup();
        if (useGroup == null) {
            if (pathUsage != null && !UseSpeckUsageUtil.isUsed(useSpeck, pathUsage)) {
                RsUseSpeckUtil.deleteWithSurroundingComma(useSpeck);
                return false;
            } else {
                return true;
            }
        } else {
            for (RsUseSpeck child : useGroup.getUseSpeckList()) {
                optimizeUseSpeck(child, factory, pathUsage);
            }
            if (removeUseSpeckIfEmpty(useSpeck)) return false;
            if (removeCurlyBracesIfPossible(factory, useSpeck)) return true;
            sortUseSpecks(useGroup);
            return true;
        }
    }

    public static void sortUseSpecks(@NotNull RsUseGroup useGroup) {
        List<RsUseSpeck> sortedList = useGroup.getUseSpeckList().stream()
            .sorted(ImportUtils.getCOMPARATOR_FOR_SPECKS_IN_USE_GROUP())
            .map(speck -> (RsUseSpeck) speck.copy())
            .collect(Collectors.toList());
        List<RsUseSpeck> original = useGroup.getUseSpeckList();
        for (int i = 0; i < original.size(); i++) {
            original.get(i).replace(sortedList.get(i));
        }
    }

    /**
     * Returns true if successfully removed, e.g. {@code use aaa::{bbb};} -> {@code use aaa::bbb;}
     */
    private static boolean removeCurlyBracesIfPossible(@NotNull RsPsiFactory psiFactory, @NotNull RsUseSpeck useSpeck) {
        RsUseGroup useGroup = useSpeck.getUseGroup();
        if (useGroup == null) return false;
        RsUseSpeck trivial = org.rust.lang.core.psi.ext.RsUseGroupUtil.getAsTrivial(useGroup);
        if (trivial == null) return false;
        String name = trivial.getText();
        String path = useSpeck.getPath() != null ? useSpeck.getPath().getText() : null;
        String tempPath = (path != null ? path + "::" : "") + name;
        RsUseSpeck newUseSpeck = psiFactory.createUseSpeck(tempPath);
        useSpeck.replace(newUseSpeck);
        return true;
    }

    /**
     * Returns true if useSpeck is empty and was successfully removed,
     * e.g. {@code use aaa::{bbb::{}, ccc, ddd};} -> {@code use aaa::{ccc, ddd};}
     */
    private static boolean removeUseSpeckIfEmpty(@NotNull RsUseSpeck useSpeck) {
        RsUseGroup useGroup = useSpeck.getUseGroup();
        if (useGroup == null) return false;
        if (!useGroup.getUseSpeckList().isEmpty()) return false;
        if (useSpeck.getParent() instanceof RsUseGroup) {
            RsUseSpeckUtil.deleteWithSurroundingComma(useSpeck);
        }
        return true;
    }

    private static void replaceOrderOfUseItems(
        @NotNull RsItemsOwner scope,
        @NotNull Collection<RsUseItem> uses,
        @org.jetbrains.annotations.Nullable PathUsageMap pathUsage
    ) {
        int offset = scope instanceof RsModItem ? ((RsModItem) scope).getLbrace().getTextOffset() + 1 : 0;
        List<RsElement> children = RsElementUtil.childrenOfType(scope, RsElement.class);
        RsElement first = null;
        for (RsElement child : children) {
            if (child.getTextOffset() >= offset
                && !(child instanceof RsExternCrateItem)
                && !(child instanceof RsAttr)
                && !(child instanceof RsDocComment)) {
                first = child;
                break;
            }
        }
        if (first == null) return;

        RsPsiFactory psiFactory = new RsPsiFactory(scope.getProject());
        List<UseItemWrapper> sortedUses = new ArrayList<>();
        for (RsUseItem use : uses) {
            RsUseSpeck useSpeck = use.getUseSpeck();
            if (useSpeck == null) continue;
            if (optimizeUseSpeck(useSpeck, psiFactory, pathUsage)) {
                sortedUses.add(new UseItemWrapper(use));
            }
        }
        Collections.sort(sortedUses);

        for (int i = 0; i < sortedUses.size(); i++) {
            UseItemWrapper useWrapper = sortedUses.get(i);
            UseItemWrapper nextUseWrapper = (i + 1 < sortedUses.size()) ? sortedUses.get(i + 1) : null;

            com.intellij.psi.PsiElement addedUseItem = scope.addBefore(useWrapper.getUseItem(), first);
            scope.addAfter(psiFactory.createNewline(), addedUseItem);

            boolean addNewLine = !Objects.equals(useWrapper.getPackageGroupLevel(), nextUseWrapper != null ? nextUseWrapper.getPackageGroupLevel() : null)
                && (nextUseWrapper != null || scope instanceof RsMod);
            if (addNewLine) {
                scope.addAfter(psiFactory.createNewline(), addedUseItem);
            }
        }
        for (RsUseItem use : uses) {
            if (use.getNextSibling() instanceof PsiWhiteSpace) {
                use.getNextSibling().delete();
            }
            use.delete();
        }
    }

    @FunctionalInterface
    private interface ScopeCallback {
        void accept(RsItemsOwner scope, List<RsUseItem> uses, @org.jetbrains.annotations.Nullable PathUsageMap pathUsage);
    }

    private static void forEachScope(@NotNull RsFile file, @NotNull ScopeCallback callback) {
        List<RsUseItem> allUseItems = RsElementUtil.descendantsOfType(file, RsUseItem.class);
        Map<com.intellij.psi.PsiElement, List<RsUseItem>> usesByScope = new LinkedHashMap<>();
        for (RsUseItem useItem : allUseItems) {
            if (isReexportOfLegacyMacro(useItem)) continue;
            usesByScope.computeIfAbsent(useItem.getParent(), k -> new ArrayList<>()).add(useItem);
        }
        for (Map.Entry<com.intellij.psi.PsiElement, List<RsUseItem>> entry : usesByScope.entrySet()) {
            com.intellij.psi.PsiElement scope = entry.getKey();
            if (!(scope instanceof RsMod) && !(scope instanceof RsBlock)) continue;
            PathUsageMap pathUsage = getPathUsage((RsItemsOwner) scope);
            callback.accept((RsItemsOwner) scope, entry.getValue(), pathUsage);
        }
    }

    @org.jetbrains.annotations.Nullable
    private static PathUsageMap getPathUsage(@NotNull RsItemsOwner scope) {
        if (!RsUnusedImportInspection.isEnabled(scope.getProject())) return null;
        return PathUsageUtil.getPathUsage(scope);
    }

    private static boolean isReexportOfLegacyMacro(@NotNull RsUseItem useItem) {
        RsUseSpeck useSpeck = useItem.getUseSpeck();
        if (useSpeck == null) return false;
        RsUseGroup useGroup = useSpeck.getUseGroup();
        if (useGroup == null) {
            return isUseSpeckReexportOfLegacyMacro(useSpeck);
        } else {
            if (useSpeck.getColoncolon() != null) return false;
            for (RsUseSpeck child : useGroup.getUseSpeckList()) {
                if (isUseSpeckReexportOfLegacyMacro(child)) return true;
            }
            return false;
        }
    }

    private static boolean isUseSpeckReexportOfLegacyMacro(@NotNull RsUseSpeck useSpeck) {
        RsPath path = useSpeck.getPath();
        if (path == null) return false;
        if (path.getColoncolon() != null) return false;
        com.intellij.psi.PsiElement resolved = path.getReference() != null ? path.getReference().resolve() : null;
        boolean macroOrNullAlias = resolved instanceof RsMacro
            || (resolved == null && useSpeck.getAlias() != null);
        return macroOrNullAlias && !RsUseSpeckUtil.isStarImport(useSpeck) && useSpeck.getUseGroup() == null;
    }
}
