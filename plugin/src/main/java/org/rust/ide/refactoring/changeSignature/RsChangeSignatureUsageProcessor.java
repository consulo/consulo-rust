/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.changeSignature;

import consulo.project.Project;
import consulo.util.lang.ref.SimpleReference;
import consulo.language.psi.PsiElement;
import consulo.language.editor.refactoring.changeSignature.ChangeInfo;
import consulo.language.editor.refactoring.changeSignature.ChangeSignatureUsageProcessor;
import consulo.language.editor.refactoring.ResolveSnapshotProvider;
import consulo.usage.UsageInfo;
import consulo.util.collection.MultiMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.presentation.PresentationUtils;
import org.rust.ide.presentation.TypeRendering;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.Namespace;
import org.rust.stdext.CollectionExtUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.rust.lang.core.psi.ext.RsTraitItemUtil;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwner;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil;
import org.rust.ide.refactoring.changeSignature.ChangeSignatureImpl;
import org.rust.lang.core.psi.ext.RsMod;

public class RsChangeSignatureUsageProcessor implements ChangeSignatureUsageProcessor {

    @Nonnull
    @Override
    public UsageInfo[] findUsages(@Nullable ChangeInfo changeInfo) {
        if (!(changeInfo instanceof RsSignatureChangeInfo)) return new UsageInfo[0];
        RsChangeFunctionSignatureConfig config = ((RsSignatureChangeInfo) changeInfo).getConfig();
        RsFunction function = config.getFunction();
        List<RsFunctionUsage> usages = new ArrayList<>(ChangeSignatureImpl.findFunctionUsages(function));

        RsAbstractableOwner owner = RsAbstractableUtil.getOwner(function);
        if (owner instanceof RsAbstractableOwner.Trait) {
            for (RsAbstractable impl : RsAbstractableUtil.searchForImplementations(function)) {
                if (impl instanceof RsFunction) {
                    RsFunction method = (RsFunction) impl;
                    usages.add(new RsFunctionUsage.MethodImplementation(method));
                    usages.addAll(ChangeSignatureImpl.findFunctionUsages(method));
                }
            }
        }

        return usages.toArray(new UsageInfo[0]);
    }

    @Nonnull
    @Override
    public MultiMap<PsiElement, consulo.localize.LocalizeValue> findConflicts(@Nullable ChangeInfo changeInfo, @Nonnull SimpleReference<UsageInfo[]> refUsages) {
        if (!(changeInfo instanceof RsSignatureChangeInfo)) return MultiMap.empty();
        MultiMap<PsiElement, consulo.localize.LocalizeValue> map = new MultiMap<>();
        RsChangeFunctionSignatureConfig config = ((RsSignatureChangeInfo) changeInfo).getConfig();
        RsFunction function = config.getFunction();

        findNameConflicts(function, config, map);
        findVisibilityConflicts(function, config, refUsages.get(), map);

        return map;
    }

    @Override
    public boolean processUsage(
        @Nullable ChangeInfo changeInfo,
        @Nullable UsageInfo usageInfo,
        boolean beforeMethodChange,
        @Nullable UsageInfo[] usages
    ) {
        if (beforeMethodChange) return false;
        if (!(changeInfo instanceof RsSignatureChangeInfo)) return false;
        if (!(usageInfo instanceof RsFunctionUsage)) return false;
        RsChangeFunctionSignatureConfig config = ((RsSignatureChangeInfo) changeInfo).getConfig();
        if (usageInfo instanceof RsFunctionUsage.MethodImplementation) {
            ChangeSignatureImpl.processFunction(
                config.getFunction().getProject(),
                config,
                ((RsFunctionUsage.MethodImplementation) usageInfo).getOverriddenMethod(),
                true
            );
        } else {
            ChangeSignatureImpl.processFunctionUsage(config, (RsFunctionUsage) usageInfo);
        }
        return true;
    }

    @Override
    public boolean processPrimaryMethod(@Nullable ChangeInfo changeInfo) {
        if (!(changeInfo instanceof RsSignatureChangeInfo)) return false;
        RsSignatureChangeInfo rsChangeInfo = (RsSignatureChangeInfo) changeInfo;
        RsChangeFunctionSignatureConfig config = rsChangeInfo.getConfig();
        RsFunction function = config.getFunction();
        Project project = function.getProject();

        ChangeSignatureImpl.processFunction(project, config, function, rsChangeInfo.isChangeSignature());
        return true;
    }

    @Override
    public boolean shouldPreviewUsages(@Nullable ChangeInfo changeInfo, @Nullable UsageInfo[] usages) {
        return false;
    }

    public boolean setupDefaultValues(
        @Nullable ChangeInfo changeInfo,
        @Nullable SimpleReference<UsageInfo[]> refUsages,
        @Nullable Project project
    ) {
        return true;
    }

    @Override
    public void registerConflictResolvers(
        List<ResolveSnapshotProvider.ResolveSnapshot> snapshots,
        ResolveSnapshotProvider resolveSnapshotProvider,
        UsageInfo[] usages,
        ChangeInfo changeInfo
    ) {
    }

    private static void findVisibilityConflicts(
        @Nonnull RsFunction function,
        @Nonnull RsChangeFunctionSignatureConfig config,
        @Nonnull UsageInfo[] usages,
        @Nonnull MultiMap<PsiElement, consulo.localize.LocalizeValue> map
    ) {
        RsFunction clone = (RsFunction) function.copy();
        ChangeSignatureImpl.changeVisibility(clone, config);

        for (UsageInfo usage : usages) {
            if (!(usage instanceof RsFunctionUsage)) continue;
            RsFunctionUsage funcUsage = (RsFunctionUsage) usage;
            RsMod sourceModule = RsElementUtil.getContainingMod(funcUsage.getUsageElement());
            if (!RsVisibilityUtil.isVisibleFrom(clone, sourceModule)) {
                String moduleName = sourceModule.getQualifiedName() != null ? sourceModule.getQualifiedName() : "";
                map.putValue(funcUsage.getUsageElement(),
                    consulo.localize.LocalizeValue.of(RsBundle.message("refactoring.change.signature.visibility.conflict", moduleName)));
            }
        }
    }

    private static void findNameConflicts(
        @Nonnull RsFunction function,
        @Nonnull RsChangeFunctionSignatureConfig config,
        @Nonnull MultiMap<PsiElement, consulo.localize.LocalizeValue> map
    ) {
        RsAbstractableOwner owner = RsAbstractableUtil.getOwner(function);
        PsiElement ownerElement;
        Collection<? extends RsAbstractable> items;
        if (owner instanceof RsAbstractableOwner.Impl) {
            ownerElement = ((RsAbstractableOwner.Impl) owner).getImpl();
            items = RsTraitOrImplUtil.getExpandedMembers(((RsAbstractableOwner.Impl) owner).getImpl());
        } else if (owner instanceof RsAbstractableOwner.Trait) {
            ownerElement = ((RsAbstractableOwner.Trait) owner).getTrait();
            items = RsTraitOrImplUtil.getExpandedMembers(((RsAbstractableOwner.Trait) owner).getTrait());
        } else {
            RsItemsOwner parent = RsElementUtil.contextStrict(function, RsItemsOwner.class);
            if (parent == null) return;
            List<? extends RsItemElement> namedElements = RsItemsOwnerUtil.getExpandedItemsCached(parent).getNamedElementsIfCfgEnabled(config.getName());
            if (namedElements == null) return;
            ownerElement = parent;
            List<RsAbstractable> abstractableItems = new ArrayList<>();
            for (RsItemElement namedElement : namedElements) {
                if (namedElement instanceof RsAbstractable) {
                    abstractableItems.add((RsAbstractable) namedElement);
                }
            }
            items = abstractableItems;
        }

        for (RsAbstractable item : items) {
            if (item == function) continue;
            if (item instanceof RsDocAndAttributeOwner && !RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf((RsDocAndAttributeOwner) item)) continue;
            if (!(item instanceof RsNamedElement)) continue;
            RsNamedElement namedItem = (RsNamedElement) item;

            if (!CollectionExtUtil.intersects(Namespace.getNamespaces(function), Namespace.getNamespaces(namedItem))) continue;
            if (config.getName().equals(namedItem.getName())) {
                consulo.navigation.ItemPresentation presentation = PresentationUtils.getPresentation((RsElement) ownerElement);
                String prefix = ownerElement instanceof RsImplItem ? "impl " : "";
                String ownerName = prefix
                    + (presentation.getPresentableText() != null ? presentation.getPresentableText() : "")
                    + " "
                    + (presentation.getLocationString() != null ? presentation.getLocationString() : "");
                map.putValue(namedItem,
                    consulo.localize.LocalizeValue.of(RsBundle.message("refactoring.change.signature.name.conflict", config.getName(), ownerName)));
            }
        }
    }
}
