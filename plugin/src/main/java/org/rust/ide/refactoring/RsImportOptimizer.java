/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import consulo.language.editor.refactoring.ImportOptimizer;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import jakarta.annotation.Nonnull;
import org.rust.ide.inspections.lints.PathUsageMap;
import org.rust.ide.inspections.lints.RsUnusedImportInspection;
import org.rust.ide.inspections.lints.UseSpeckUsageUtil;
import org.rust.ide.inspections.lints.PathUsageUtil;
import org.rust.lang.core.imports.ImportUtils;
import org.rust.lang.core.imports.UseItemWrapper;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.doc.psi.RsDocComment;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.impl.RsUseSpeckUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.ext.impl.RsUseGroupUtil;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.ext.impl.*;

@ExtensionImpl
public class RsImportOptimizer implements ImportOptimizer {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


    @Override
    public boolean supports(@Nonnull PsiFile file) {
        return file instanceof RsFile;
    }

    @Nonnull
    @Override
    public Runnable processFile(@Nonnull PsiFile file) {
        return () -> {
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(file.getProject());
            consulo.document.Document document = documentManager.getDocument(file);
            if (document != null) {
                documentManager.commitDocument(document);
            }
            optimizeAndReorderUseItems((RsFile) file);
            reorderExternCrates((RsFile) file);
        };
    }

    private void reorderExternCrates(@Nonnull RsFile file) {
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

    private void optimizeAndReorderUseItems(@Nonnull RsFile file) {
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

    public static void optimizeUseItems(@Nonnull RsFile file) {
        RsPsiFactory factory = new RsPsiFactory(file.getProject());
        forEachScope(file, (scope, uses, pathUsage) -> {
            if (scope instanceof RsMod) {
                for (RsUseItem useItem : uses) {
                    optimizeUseItem(useItem, factory, pathUsage);
                }
            }
        });
    }

    private static void optimizeUseItem(@Nonnull RsUseItem useItem, @Nonnull RsPsiFactory factory, @org.jetbrains.annotations.Nullable PathUsageMap pathUsage) {
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
        @Nonnull RsUseSpeck useSpeck,
        @Nonnull RsPsiFactory factory,
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

    public static void sortUseSpecks(@Nonnull RsUseGroup useGroup) {
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
    private static boolean removeCurlyBracesIfPossible(@Nonnull RsPsiFactory psiFactory, @Nonnull RsUseSpeck useSpeck) {
        RsUseGroup useGroup = useSpeck.getUseGroup();
        if (useGroup == null) return false;
        RsUseSpeck trivial = org.rust.lang.core.psi.ext.impl.RsUseGroupUtil.getAsTrivial(useGroup);
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
    private static boolean removeUseSpeckIfEmpty(@Nonnull RsUseSpeck useSpeck) {
        RsUseGroup useGroup = useSpeck.getUseGroup();
        if (useGroup == null) return false;
        if (!useGroup.getUseSpeckList().isEmpty()) return false;
        if (useSpeck.getParent() instanceof RsUseGroup) {
            RsUseSpeckUtil.deleteWithSurroundingComma(useSpeck);
        }
        return true;
    }

    private static void replaceOrderOfUseItems(
        @Nonnull RsItemsOwner scope,
        @Nonnull Collection<RsUseItem> uses,
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

            consulo.language.psi.PsiElement addedUseItem = scope.addBefore(useWrapper.getUseItem(), first);
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

    private static void forEachScope(@Nonnull RsFile file, @Nonnull ScopeCallback callback) {
        List<RsUseItem> allUseItems = RsElementUtil.descendantsOfType(file, RsUseItem.class);
        Map<consulo.language.psi.PsiElement, List<RsUseItem>> usesByScope = new LinkedHashMap<>();
        for (RsUseItem useItem : allUseItems) {
            if (isReexportOfLegacyMacro(useItem)) continue;
            usesByScope.computeIfAbsent(useItem.getParent(), k -> new ArrayList<>()).add(useItem);
        }
        for (Map.Entry<consulo.language.psi.PsiElement, List<RsUseItem>> entry : usesByScope.entrySet()) {
            consulo.language.psi.PsiElement scope = entry.getKey();
            if (!(scope instanceof RsMod) && !(scope instanceof RsBlock)) continue;
            PathUsageMap pathUsage = getPathUsage((RsItemsOwner) scope);
            callback.accept((RsItemsOwner) scope, entry.getValue(), pathUsage);
        }
    }

    @org.jetbrains.annotations.Nullable
    private static PathUsageMap getPathUsage(@Nonnull RsItemsOwner scope) {
        if (!RsUnusedImportInspection.isEnabled(scope.getProject())) return null;
        return PathUsageUtil.getPathUsage(scope);
    }

    private static boolean isReexportOfLegacyMacro(@Nonnull RsUseItem useItem) {
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

    private static boolean isUseSpeckReexportOfLegacyMacro(@Nonnull RsUseSpeck useSpeck) {
        RsPath path = useSpeck.getPath();
        if (path == null) return false;
        if (path.getColoncolon() != null) return false;
        consulo.language.psi.PsiElement resolved = path.getReference() != null ? path.getReference().resolve() : null;
        boolean macroOrNullAlias = resolved instanceof RsMacro
            || (resolved == null && useSpeck.getAlias() != null);
        return macroOrNullAlias && !RsUseSpeckUtil.isStarImport(useSpeck) && useSpeck.getUseGroup() == null;
    }
}
