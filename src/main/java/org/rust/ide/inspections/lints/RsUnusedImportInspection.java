/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel;
import com.intellij.openapi.project.Project;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UsageSearchContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RemoveImportFix;
import org.rust.ide.injected.DoctestUtils;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.toml.CrateExt;
import org.rust.lang.core.crate.impl.DoctestCrate;
import org.rust.lang.core.macros.MacroExpansionExtUtil;
import org.rust.lang.core.macros.proc.ProcMacroApplicationService;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve2.*;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.*;
import java.util.*;
import org.rust.lang.core.psi.ext.RsUseSpeckUtil;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.ide.injected.RsDoctestLanguageInjector;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.psi.ext.RsTraitItemUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsReferenceElementBase;
import org.rust.lang.core.psi.ext.RsMod;

public class RsUnusedImportInspection extends RsLintInspection {

    public boolean ignoreDoctest = true;
    public boolean enableOnlyIfProcMacrosEnabled = true;

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.UnusedImports;
    }

    @NotNull
    @Override
    public String getShortName() {
        return SHORT_NAME;
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @SuppressWarnings("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            @Override
            public void visitUseItem2(@NotNull RsUseItem item) {
                if (!isApplicableForUseItem(item)) return;

                // It's common to include more imports than needed in doctest sample code
                if (ignoreDoctest && RsElementUtil.getContainingCrate(item) instanceof DoctestCrate) return;
                if (enableOnlyIfProcMacrosEnabled && !ProcMacroApplicationService.isFullyEnabled() && !OpenApiUtil.isUnitTestMode()) return;

                RsItemsOwner owner = item.getContext() instanceof RsItemsOwner ? (RsItemsOwner) item.getContext() : null;
                if (owner == null) return;
                PathUsageMap usage = RsPathUsageAnalysis.getPathUsage(owner);

                RsUseSpeck speck = item.getUseSpeck();
                if (speck == null) return;

                Set<RsUseSpeck> unusedSpeckSet = new HashSet<>();
                storeUseSpeckUsage(speck, usage, unusedSpeckSet);
                markUnusedSpecks(speck, unusedSpeckSet, holder);
            }
        };
    }

    /**
     * Traverse all use specks recursively and store information about their usage.
     */
    private void storeUseSpeckUsage(
        @NotNull RsUseSpeck useSpeck,
        @NotNull PathUsageMap usage,
        @NotNull Set<RsUseSpeck> unusedSpeckSet
    ) {
        RsUseGroup group = useSpeck.getUseGroup();
        boolean isUsed;
        if (group == null) {
            isUsed = isUseSpeckUsed(useSpeck, usage);
        } else {
            for (RsUseSpeck child : group.getUseSpeckList()) {
                storeUseSpeckUsage(child, usage, unusedSpeckSet);
            }
            isUsed = false;
            for (RsUseSpeck child : group.getUseSpeckList()) {
                if (!unusedSpeckSet.contains(child)) {
                    isUsed = true;
                    break;
                }
            }
        }
        if (!isUsed) {
            unusedSpeckSet.add(useSpeck);
        }
    }

    private void markUnusedSpecks(
        @NotNull RsUseSpeck useSpeck,
        @NotNull Set<RsUseSpeck> unusedSpeckSet,
        @NotNull RsProblemsHolder holder
    ) {
        boolean used = !unusedSpeckSet.contains(useSpeck);
        if (!used) {
            markAsUnused(useSpeck, holder);
        } else {
            RsUseGroup group = useSpeck.getUseGroup();
            if (group != null) {
                for (RsUseSpeck child : group.getUseSpeckList()) {
                    markUnusedSpecks(child, unusedSpeckSet, holder);
                }
            }
        }
    }

    private void markAsUnused(@NotNull RsUseSpeck useSpeck, @NotNull RsProblemsHolder holder) {
        PsiElement element = getHighlightElement(useSpeck);
        // https://github.com/intellij-rust/intellij-rust/issues/7565
        List<LocalQuickFix> fixes = RsDoctestLanguageInjector.isDoctestInjection(useSpeck)
            ? Collections.emptyList()
            : Collections.singletonList(new RemoveImportFix(element));
        registerLintProblem(
            holder,
            element,
            RsBundle.message("inspection.message.unused.import", useSpeck.getText()),
            RsLintHighlightingType.UNUSED_SYMBOL,
            fixes
        );
    }

    @Nullable
    @Override
    public JComponent createOptionsPanel() {
        MultipleCheckboxOptionsPanel panel = new MultipleCheckboxOptionsPanel(this);
        panel.addCheckbox(RsBundle.message("checkbox.ignore.unused.imports.in.doctests"), "ignoreDoctest");
        panel.addCheckbox(RsBundle.message("checkbox.enable.inspection.only.if.procedural.macros.are.enabled"), "enableOnlyIfProcMacrosEnabled");
        return panel;
    }

    @NotNull
    private static PsiElement getHighlightElement(@NotNull RsUseSpeck useSpeck) {
        if (useSpeck.getParent() instanceof RsUseItem) {
            return useSpeck.getParent();
        }
        return useSpeck;
    }

    private static boolean isApplicableForUseItem(@NotNull RsUseItem item) {
        org.rust.lang.core.crate.Crate crate = RsElementUtil.getContainingCrate(item);
        if (item.getVisibility() == RsVisibility.Public.INSTANCE && crate.getKind().isLib()) return false;
        if (!CfgUtils.existsAfterExpansion(item, crate)) return false;
        return true;
    }

    /**
     * Returns true if this use speck has at least one usage in its containing owner.
     */
    public static boolean isUseSpeckUsed(@NotNull RsUseSpeck useSpeck, @NotNull PathUsageMap pathUsage) {
        RsUseItem useItem = RsElementUtil.ancestorStrict(useSpeck, RsUseItem.class);
        if (useItem == null) return true;
        if (!isApplicableForUseItem(useItem)) return true;
        return isUseSpeckUsedInternal(useSpeck, pathUsage)
            || RsLint.UnusedImports.explicitLevel(useItem) == RsLintLevel.ALLOW;
    }

    private static boolean isUseSpeckUsedInternal(@NotNull RsUseSpeck useSpeck, @NotNull PathUsageMap usage) {
        RsPath path = useSpeck.getPath();
        if (path != null && RsPathUtil.getResolveStatus(path) != PathResolveStatus.RESOLVED) return true;

        List<NamedItem> items;
        if (RsUseSpeckUtil.isStarImport(useSpeck)) {
            RsMod module = path != null && path.getReference() != null
                ? (path.getReference().resolve() instanceof RsMod ? (RsMod) path.getReference().resolve() : null)
                : null;
            if (module == null) return true;
            items = new ArrayList<>();
            for (RsItemElement exportedItem : RsModUtil.exportedItems(module, RsElementUtil.getContainingMod(useSpeck))) {
                if (exportedItem instanceof RsNamedElement) {
                    String name = ((RsNamedElement) exportedItem).getName();
                    if (name != null) {
                        items.add(new NamedItem(name, (RsNamedElement) exportedItem));
                    }
                }
            }
        } else {
            if (path == null) return true;
            List<RsElement> resolvedItems = path.getReference() != null ? path.getReference().multiResolve() : null;
            if (resolvedItems == null) return true;
            String name = RsUseSpeckUtil.itemName(useSpeck, true);
            if (name == null) return true;
            items = new ArrayList<>();
            for (RsElement item : resolvedItems) {
                if (item instanceof RsNamedElement) {
                    items.add(new NamedItem(name, (RsNamedElement) item));
                }
            }
        }

        for (NamedItem namedItem : items) {
            if (isItemUsedInSameMod(namedItem.myItem, namedItem.myImportName, usage)
                || isItemUsedInOtherMods(namedItem.myItem, namedItem.myImportName, useSpeck)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isItemUsedInSameMod(@NotNull RsNamedElement item, @NotNull String importName, @NotNull PathUsageMap usage) {
        Set<RsElement> pathUsages = usage.getPathUsages().get(importName);
        boolean used = (pathUsages != null && pathUsages.contains(item))
            || (item instanceof RsTraitItem && usage.getTraitUsages().contains(item));
        boolean probablyUsed = usage.getUnresolvedPaths().contains(importName);
        if (!probablyUsed && item instanceof RsTraitItem) {
            List<? extends RsAbstractable> expandedMembers = RsMembersUtil.getExpandedMembers((RsTraitItem) item);
            for (RsAbstractable member : expandedMembers) {
                if (member.getName() != null && usage.getUnresolvedMethods().contains(member.getName())) {
                    probablyUsed = true;
                    break;
                }
            }
        }
        return used || probablyUsed;
    }

    private static boolean isItemUsedInOtherMods(@NotNull RsNamedElement item, @NotNull String importName, @NotNull RsUseSpeck useSpeck) {
        RsUseItem useItem = RsElementUtil.ancestorStrict(useSpeck, RsUseItem.class);
        if (useItem == null) return true;
        RsMod importMod = RsElementUtil.getContainingMod(useItem);
        RsVisibility useItemVisibility = useItem.getVisibility();
        if (useItemVisibility == RsVisibility.Private.INSTANCE) {
            if (!RsModUtil.getHasChildModules(importMod)) return false;
            useItemVisibility = new RsVisibility.Restricted(importMod);
        } else if (useItemVisibility instanceof RsVisibility.Restricted) {
            // keep as is
        } else if (useItemVisibility == RsVisibility.Public.INSTANCE) {
            RsMod crateRoot = RsModUtil.getCrateRoot(importMod);
            if (crateRoot == null) return true;
            useItemVisibility = new RsVisibility.Restricted(crateRoot);
        }
        if (item instanceof RsTraitItem) {
            // TODO we should search usages for all methods of the trait
            return true;
        }

        SearchScope searchScope = RsPsiImplUtil.getDeclarationUseScope(useItem);
        RsVisibility.Restricted finalVisibility = (RsVisibility.Restricted) useItemVisibility;
        return !processReferencesWithAliases((RsElement) item, searchScope, importName, element -> {
            return !isImportNeededForReference(element, finalVisibility, importMod);
        });
    }

    private static boolean isImportNeededForReference(
        @NotNull RsReferenceElement reference,
        @NotNull RsVisibility.Restricted importVisibility,
        @NotNull RsMod importMod
    ) {
        if (RsElementUtil.getContainingMod(reference) == importMod) {
            return false;
        }
        if (!RsModExtUtil.getSuperMods(RsElementUtil.getContainingMod(reference)).contains(importVisibility.getInMod())) {
            return false;
        }

        if (reference instanceof RsPath) {
            RsPath qualifier = RsPathUtil.getQualifier((RsPath) reference);
            if (qualifier == null) {
                return hasTransitiveGlobImportTo(RsElementUtil.getContainingMod(reference), importMod);
            } else {
                PsiElement qualifierTarget = qualifier.getReference() != null ? qualifier.getReference().resolve() : null;
                if (!(qualifierTarget instanceof RsMod)) return false;
                return hasTransitiveGlobImportTo((RsMod) qualifierTarget, importMod);
            }
        } else if (reference instanceof RsPatBinding) {
            return hasTransitiveGlobImportTo(RsElementUtil.getContainingMod(reference), importMod);
        }
        return false;
    }

    private static boolean hasTransitiveGlobImportTo(@NotNull RsMod self, @NotNull RsMod target) {
        if (self == target) return true;
        org.rust.lang.core.crate.Crate notFakeCrate = org.rust.lang.core.crate.Crate.asNotFake(RsElementUtil.getContainingCrate(self));
        Integer crateId = notFakeCrate != null
            ? notFakeCrate.getId()
            : null;
        if (crateId == null) return false;
        org.rust.lang.core.crate.Crate targetCrate = RsElementUtil.getContainingCrate(target);
        if (targetCrate == null || !crateId.equals(targetCrate.getId())) {
            return false;
        }
        DefMapService defMapService = self.getProject().getService(DefMapService.class);
        CrateDefMap defMap = FacadeUpdateDefMap.getOrUpdateIfNeeded(defMapService, crateId);
        if (defMap == null) return false;
        return FacadeMetaInfo.hasTransitiveGlobImport(defMap, self, target);
    }

    /**
     * Like searchReferences, but takes into account reexports with aliases.
     * Returns true if originalProcessor returns true for all references.
     */
    public static boolean processReferencesWithAliases(
        @NotNull RsElement item,
        @Nullable SearchScope searchScope,
        @NotNull String identifier,
        @NotNull ReferenceProcessor originalProcessor
    ) {
        SearchScope effectiveScope = searchScope != null ? searchScope : GlobalSearchScope.allScope(item.getProject());
        return PsiSearchHelper.getInstance(item.getProject()).processElementsWithWord(
            (element, offset) -> processElement(element, item, identifier, originalProcessor),
            effectiveScope,
            identifier,
            UsageSearchContext.IN_CODE,
            true
        );
    }

    private static boolean processElement(
        @NotNull PsiElement element,
        @NotNull RsElement item,
        @NotNull String identifier,
        @NotNull ReferenceProcessor originalProcessor
    ) {
        if (element instanceof LeafPsiElement) {
            List<PsiElement> expansionElements = MacroExpansionExtUtil.findExpansionElements(element);
            if (expansionElements != null) {
                for (PsiElement expansionElement : expansionElements) {
                    PsiElement ancestor = expansionElement;
                    while (ancestor != null) {
                        if (!processElement(ancestor, item, identifier, originalProcessor)) {
                            return false;
                        }
                        ancestor = ancestor.getParent();
                    }
                }
                return true;
            }
        }
        if (!(element instanceof RsReferenceElementBase) || !identifier.equals(((RsReferenceElementBase) element).getReferenceName())) {
            return true;
        }
        if (element instanceof RsReferenceElement) {
            RsReferenceElement refElement = (RsReferenceElement) element;
            if (refElement.getReference() != null && refElement.getReference().isReferenceTo(item)) {
                return originalProcessor.process(refElement);
            }
        }
        return true;
    }

    public static boolean isEnabled(@NotNull Project project) {
        InspectionProfileImpl profile = InspectionProjectProfileManager.getInstance(project).getCurrentProfile();
        boolean enabled = profile.isToolEnabled(HighlightDisplayKey.find(SHORT_NAME));
        return enabled && checkProcMacrosMatch(profile, project);
    }

    private static boolean checkProcMacrosMatch(@NotNull InspectionProfileImpl profile, @NotNull Project project) {
        if (OpenApiUtil.isUnitTestMode()) return true;
        com.intellij.codeInspection.ex.InspectionToolWrapper<?, ?> toolWrapper = profile.getInspectionTool(SHORT_NAME, project);
        if (toolWrapper == null) return true;
        com.intellij.codeInspection.InspectionProfileEntry tool = toolWrapper.getTool();
        if (!(tool instanceof RsUnusedImportInspection)) return true;
        RsUnusedImportInspection inspection = (RsUnusedImportInspection) tool;
        if (!inspection.enableOnlyIfProcMacrosEnabled) return true;
        return ProcMacroApplicationService.isFullyEnabled();
    }

    public static final String SHORT_NAME = "RsUnusedImport";

    @FunctionalInterface
    public interface ReferenceProcessor {
        boolean process(@NotNull RsReferenceElement element);
    }

    private static class NamedItem {
        final String myImportName;
        final RsNamedElement myItem;

        NamedItem(@NotNull String importName, @NotNull RsNamedElement item) {
            myImportName = importName;
            myItem = item;
        }
    }
}
