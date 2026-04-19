/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import org.rust.ide.refactoring.move.common.RsMoveUtil;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.DummyHolder;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.fixes.MakePublicFix;
import org.rust.ide.utils.imports.RsImportHelper;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.intellij.refactoring.RefactoringBundle.message;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import com.intellij.psi.PsiFile;

public class RsMoveCommonProcessor {

    private static final Key<RsElement> RS_PATH_OLD_BEFORE_MOVE_KEY = Key.create("RS_PATH_OLD_BEFORE_MOVE_KEY");
    private static final Key<RsQualifiedNamedElement> RS_TARGET_BEFORE_MOVE_KEY = Key.create("RS_TARGET_BEFORE_MOVE_KEY");

    @NotNull
    private final Project project;
    @NotNull
    private List<ElementToMove> elementsToMove;
    @NotNull
    private final RsMod targetMod;
    @NotNull
    private final RsPsiFactory psiFactory;
    @NotNull
    private final RsCodeFragmentFactory codeFragmentFactory;
    @NotNull
    private final RsMod sourceMod;
    @NotNull
    private final RsMovePathHelper pathHelper;
    @NotNull
    private final RsMoveTraitMethodsProcessor traitMethodsProcessor;

    private RsMoveConflictsDetector conflictsDetector;
    private List<RsMoveReferenceInfo> outsideReferences;

    public RsMoveCommonProcessor(
        @NotNull Project project,
        @NotNull List<ElementToMove> elementsToMove,
        @NotNull RsMod targetMod
    ) {
        this.project = project;
        this.elementsToMove = elementsToMove;
        this.targetMod = targetMod;
        this.psiFactory = new RsPsiFactory(project);
        this.codeFragmentFactory = new RsCodeFragmentFactory(project);

        if (elementsToMove.isEmpty()) {
            throw new IncorrectOperationException("No items to move");
        }

        Set<RsMod> sourceMods = elementsToMove.stream()
            .map(e -> RsMoveUtil.getContainingModStrict(e.getElement()))
            .collect(Collectors.toSet());
        if (sourceMods.size() != 1) {
            throw new IllegalStateException("Elements to move must belong to single parent mod");
        }
        this.sourceMod = sourceMods.iterator().next();

        if (targetMod == sourceMod) {
            throw new IncorrectOperationException("Source and destination modules should be different");
        }

        this.pathHelper = new RsMovePathHelper(project, targetMod);
        this.traitMethodsProcessor = new RsMoveTraitMethodsProcessor(psiFactory, sourceMod, targetMod, pathHelper);
    }

    @NotNull
    public RsMod getTargetMod() {
        return targetMod;
    }

    @NotNull
    public RsMoveUsageInfo[] findUsages() {
        List<PsiReference> referencesDirect = findDirectInsideReferences();
        List<PsiReference> referencesIndirect = findIndirectInsideReferences();
        List<PsiReference> all = new ArrayList<>(referencesDirect);
        all.addAll(referencesIndirect);

        return all.stream()
            .map(this::createMoveUsageInfo)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(
                u -> u.getRsElement().getContainingMod().getCrateRelativePath() + ":" + u.getRsElement().getStartOffsetInParent()
            ))
            .toArray(RsMoveUsageInfo[]::new);
    }

    @NotNull
    private List<PsiReference> findDirectInsideReferences() {
        List<PsiReference> result = new ArrayList<>();
        for (ElementToMove element : elementsToMove) {
            result.addAll(ReferencesSearch.search(element.getElement(), GlobalSearchScope.projectScope(project)).findAll());
        }
        return result;
    }

    @NotNull
    private List<PsiReference> findIndirectInsideReferences() {
        List<RsPath> paths = RsMoveUtil.movedElementsShallowDescendantsOfType(elementsToMove, RsPath.class, false);
        return paths.stream()
            .filter(path -> {
                if (RsElementUtil.ancestorStrict(path, RsUseGroup.class) != null || path.getPath() != null) return false;
                PsiElement target = path.getReference() != null ? path.getReference().resolve() : null;
                if (target == null) return false;
                if (elementsToMove.stream().anyMatch(e -> e instanceof ItemToMove && ((ItemToMove) e).getItem() == target)) return false;
                return RsMoveUtil.isInsideMovedElements(target, elementsToMove);
            })
            .map(path -> (PsiReference) path.getReference())
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Nullable
    private RsMoveUsageInfo createMoveUsageInfo(@NotNull PsiReference reference) {
        PsiElement element = reference.getElement();
        PsiElement target = reference.resolve();
        if (target == null) return null;

        if (element instanceof RsModDeclItem && target instanceof RsFile) {
            return new RsModDeclUsageInfo((RsModDeclItem) element, (RsFile) target);
        }
        if (element instanceof RsPath && target instanceof RsQualifiedNamedElement) {
            return new RsPathUsageInfo((RsPath) element, reference, (RsQualifiedNamedElement) target);
        }
        return null;
    }

    public boolean preprocessUsages(
        @NotNull UsageInfo[] usages,
        @NotNull MultiMap<PsiElement, @DialogMessage String> conflicts
    ) {
        String title = message("refactoring.preprocess.usages.progress");
        try {
            org.rust.openapiext.OpenApiUtil.computeWithCancelableProgress(project, title, () -> {
                ReadAction.run(() -> {
                    outsideReferences = collectOutsideReferences();
                    List<RsMoveReferenceInfo> insideReferences = preprocessInsideReferences(usages);
                    traitMethodsProcessor.preprocessOutsideReferences(conflicts, elementsToMove);
                    traitMethodsProcessor.preprocessInsideReferences(conflicts, elementsToMove);

                    if (!org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
                        ProgressManager.getInstance().getProgressIndicator().setText(message("detecting.possible.conflicts"));
                    }

                    conflictsDetector = new RsMoveConflictsDetector(conflicts, elementsToMove, sourceMod, targetMod);
                    conflictsDetector.detectOutsideReferencesVisibilityProblems(outsideReferences);
                    conflictsDetector.detectInsideReferencesVisibilityProblems(insideReferences);
                    conflictsDetector.checkImpls();
                });
                return null;
            });
            return true;
        } catch (ProcessCanceledException e) {
            return false;
        }
    }

    @NotNull
    private List<RsMoveReferenceInfo> collectOutsideReferences() {
        List<RsMoveReferenceInfo> references = new ArrayList<>();
        for (RsPath path : RsMoveUtil.movedElementsDeepDescendantsOfType(elementsToMove, RsPath.class)) {
            if (path.getContainingFile() == sourceMod.getContainingFile()) {
                path.putCopyableUserData(RS_PATH_OLD_BEFORE_MOVE_KEY, path);
            }

            if (path.getParent() instanceof RsVisRestriction) continue;
            if (path.getContainingMod() != sourceMod
                && !RsMoveUtil.isAbsolute(path)
                && !RsMoveUtil.startsWithSuper(path)
            ) continue;
            if (!RsMoveUtil.isSimplePath(path)) continue;
            if (!checkMacroCallPath(path)) continue;
            if (RsElementUtil.ancestorStrict(path, RsUseGroup.class) != null) continue;

            PsiElement resolved = path.getReference() != null ? path.getReference().resolve() : null;
            if (!(resolved instanceof RsQualifiedNamedElement)) continue;
            RsQualifiedNamedElement target = (RsQualifiedNamedElement) resolved;

            if (RsMoveUtil.isInsideMovedElements(target, elementsToMove)) continue;

            RsMoveReferenceInfo reference = createOutsideReferenceInfo(path, target);
            if (reference != null) {
                references.add(reference);
            }
        }
        for (RsPatIdent patIdent : RsMoveUtil.movedElementsShallowDescendantsOfType(elementsToMove, RsPatIdent.class, true)) {
            patIdent.putCopyableUserData(RS_PATH_OLD_BEFORE_MOVE_KEY, patIdent);
            PsiElement resolved = patIdent.getPatBinding().getReference().resolve();
            if (!(resolved instanceof RsQualifiedNamedElement)) continue;
            RsQualifiedNamedElement target = (RsQualifiedNamedElement) resolved;
            if (!(target instanceof RsStructItem) && !(target instanceof RsEnumVariant) && !(target instanceof RsConstant)) continue;
            RsMoveReferenceInfo reference = createOutsideReferenceInfo(patIdent, target);
            if (reference != null) {
                references.add(reference);
            }
        }
        return references;
    }

    private boolean checkMacroCallPath(@NotNull RsPath path) {
        if (!(path.getParent() instanceof RsMacroCall)) return true;
        PsiElement resolved = path.getReference() != null ? path.getReference().resolve() : null;
        if (!(resolved instanceof RsMacroDefinitionBase)) return false;
        Object targetCrate = ((RsMacroDefinitionBase) resolved).getContainingCrate();
        return targetCrate != sourceMod.getContainingCrate() && resolved != targetMod.getContainingCrate();
    }

    @Nullable
    private RsMoveReferenceInfo createOutsideReferenceInfo(@NotNull RsElement pathOriginal, @NotNull RsQualifiedNamedElement target) {
        RsPath path = RsMoveUtil.convertFromPathOriginal(pathOriginal, codeFragmentFactory);

        if (path.getContainingMod() == sourceMod && RsMoveUtil.getContainingModStrict(target) == targetMod) {
            String name = target.getName();
            if (name != null) {
                RsPath pathNew = RsMoveUtil.toRsPath(name, psiFactory);
                if (pathNew != null) {
                    return new RsMoveReferenceInfo(path, pathOriginal, pathNew, pathNew, target, true);
                }
            }
        }

        if (RsMoveUtil.isAbsolute(path)) {
            PsiElement basePathTarget = RsPathUtil.basePath(path).getReference() != null ? RsPathUtil.basePath(path).getReference().resolve() : null;
            if (basePathTarget instanceof RsMod) {
                RsMod basePathMod = (RsMod) basePathTarget;
                if (basePathMod.getCrateRoot() != sourceMod.getCrateRoot() && basePathMod.getCrateRoot() != targetMod.getCrateRoot()) {
                    return null;
                }
            }

            RsPath pathNew = RsMoveUtil.toRsPath(RsMoveUtil.getTextNormalized(path), codeFragmentFactory, targetMod);
            if (pathNew != null && RsMoveUtil.resolvesToAndAccessible(pathNew, target)) return null;
        }

        RsPath pathNewFallback;
        if (path.getContainingMod() == sourceMod) {
            String qualifiedName = target.getQualifiedNameRelativeTo(targetMod);
            pathNewFallback = qualifiedName != null ? RsMoveUtil.toRsPath(qualifiedName, codeFragmentFactory, targetMod) : null;
        } else {
            String qualifiedName = target.getQualifiedNameInCrate(targetMod);
            pathNewFallback = qualifiedName != null ? RsMoveUtil.toRsPathInEmptyTmpMod(qualifiedName, codeFragmentFactory, psiFactory, targetMod) : null;
        }
        String importPath = RsImportHelper.findPath(targetMod, target);
        RsPath pathNewAccessible = importPath != null
            ? RsMoveUtil.toRsPathInEmptyTmpMod(importPath, codeFragmentFactory, psiFactory, targetMod) : null;

        return new RsMoveReferenceInfo(path, pathOriginal, pathNewAccessible, pathNewFallback, target);
    }

    @NotNull
    private List<RsMoveReferenceInfo> preprocessInsideReferences(@NotNull UsageInfo[] usages) {
        List<RsPathUsageInfo> pathUsages = Arrays.stream(usages)
            .filter(u -> u instanceof RsPathUsageInfo)
            .map(u -> (RsPathUsageInfo) u)
            .collect(Collectors.toList());

        for (RsPathUsageInfo usage : pathUsages) {
            usage.setReferenceInfo(createInsideReferenceInfo(usage.getElement(), usage.getTarget()));
        }

        List<RsMoveReferenceInfo> originalReferences = pathUsages.stream().map(RsPathUsageInfo::getReferenceInfo).collect(Collectors.toList());

        for (RsPathUsageInfo usage : pathUsages) {
            RsMoveReferenceInfo converted = convertToFullReference(usage.getReferenceInfo());
            if (converted != null) {
                usage.setReferenceInfo(converted);
            }

            RsQualifiedNamedElement target = usage.getReferenceInfo().getTarget();
            target.putCopyableUserData(RS_TARGET_BEFORE_MOVE_KEY, target);
        }
        return originalReferences;
    }

    @NotNull
    private RsMoveReferenceInfo createInsideReferenceInfo(@NotNull RsPath pathOriginal, @NotNull RsQualifiedNamedElement target) {
        RsPath path = RsMoveUtil.convertFromPathOriginal(pathOriginal, codeFragmentFactory);

        boolean isSelfReference = RsMoveUtil.isInsideMovedElements(pathOriginal, elementsToMove);
        if (isSelfReference && path.getContainingMod() == sourceMod) {
            if (RsMoveUtil.getContainingModStrict(target) == sourceMod) {
                String name = target.getName();
                if (name != null) {
                    RsPath pathNew = RsMoveUtil.toRsPath(name, codeFragmentFactory, targetMod);
                    if (pathNew != null) return new RsMoveReferenceInfo(path, pathOriginal, pathNew, pathNew, target);
                }
            } else {
                String pathOldAbsolute = RsImportHelper.findPath(path, target);
                if (pathOldAbsolute != null) {
                    String sourceModPrefix = "crate" + sourceMod.getCrateRelativePath() + "::";
                    if (pathOldAbsolute.startsWith(sourceModPrefix)) {
                        String pathRelativeToSourceMod = pathOldAbsolute.substring(sourceModPrefix.length());
                        RsPath pathNew = RsMoveUtil.toRsPath(pathRelativeToSourceMod, codeFragmentFactory, targetMod);
                        return new RsMoveReferenceInfo(path, pathOriginal, pathNew, pathNew, target);
                    }
                }
            }
        }

        RsPath pathNewAccessible = pathHelper.findPathAfterMove(path, target);
        RsPath pathNewFallback = null;
        String targetModPath = targetMod.getQualifiedNameRelativeTo(path.getContainingMod());
        if (targetModPath != null) {
            String targetName = target.getName();
            if (targetName != null) {
                String pathNewFallbackText = targetModPath + "::" + targetName;
                RsElement context = path.getContext() instanceof RsElement ? (RsElement) path.getContext() : path;
                pathNewFallback = RsMoveUtil.toRsPath(pathNewFallbackText, codeFragmentFactory, context);
            }
        }
        return new RsMoveReferenceInfo(path, pathOriginal, pathNewAccessible, pathNewFallback, target);
    }

    @Nullable
    private RsMoveReferenceInfo convertToFullReference(@NotNull RsMoveReferenceInfo reference) {
        if (RsMoveUtil.isSimplePath(reference.getPathOld()) || reference.isInsideUseDirective()) return null;

        PsiElement current = reference.getPathOldOriginal();
        RsPath pathOldOriginal = null;
        while (current instanceof RsPath) {
            if (RsMoveUtil.isSimplePath((RsPath) current)) {
                pathOldOriginal = (RsPath) current;
                break;
            }
            current = current.getParent();
        }
        if (pathOldOriginal == null) return null;

        RsPath pathOld = RsMoveUtil.convertFromPathOriginal(pathOldOriginal, codeFragmentFactory);
        if (!RsMoveUtil.getTextNormalized(pathOld).startsWith(RsMoveUtil.getTextNormalized(reference.getPathOld()))) {
            RsMoveUtil.LOG.error("Expected '" + pathOld.getText() + "' to start with '" + reference.getPathOld().getText() + "'");
            return null;
        }

        if (pathOld.getContainingFile() instanceof DummyHolder) {
            RsMoveUtil.LOG.error("Path '" + pathOld.getText() + "' is inside dummy holder");
        }
        PsiElement resolved = pathOld.getReference() != null ? pathOld.getReference().resolve() : null;
        if (!(resolved instanceof RsQualifiedNamedElement)) return null;
        RsQualifiedNamedElement target = (RsQualifiedNamedElement) resolved;

        RsPath pathNewAccessible = reference.getPathNewAccessible() != null ? convertPathToFull(pathOld, reference, reference.getPathNewAccessible()) : null;
        RsPath pathNewFallback = reference.getPathNewFallback() != null ? convertPathToFull(pathOld, reference, reference.getPathNewFallback()) : null;

        return new RsMoveReferenceInfo(pathOld, pathOldOriginal, pathNewAccessible, pathNewFallback, target);
    }

    @Nullable
    private RsPath convertPathToFull(@NotNull RsPath pathOld, @NotNull RsMoveReferenceInfo reference, @NotNull RsPath path) {
        String pathFullText = RsMoveUtil.getTextNormalized(pathOld)
            .replaceFirst(RsMoveUtil.getTextNormalized(reference.getPathOld()), RsMoveUtil.getTextNormalized(path));
        RsPath pathFull = RsMoveUtil.toRsPath(pathFullText, codeFragmentFactory, path);
        if (pathFull != null && pathFull.getContainingFile() instanceof DummyHolder) {
            RsMoveUtil.LOG.error("Path '" + pathFull.getText() + "' is inside dummy holder");
        }
        return pathFull;
    }

    public void performRefactoring(@NotNull UsageInfo[] usages, @NotNull Supplier<List<ElementToMove>> moveElements) {
        updateOutsideReferencesInVisRestrictions();

        elementsToMove = moveElements.get();
        Map<RsElement, RsElement> pathMapping = createMapping(RS_PATH_OLD_BEFORE_MOVE_KEY, RsElement.class);

        RsMoveRetargetReferencesProcessor retargetProcessor = new RsMoveRetargetReferencesProcessor(project, sourceMod, targetMod);
        restoreOutsideReferenceInfosAfterMove(pathMapping);
        retargetProcessor.retargetReferences(outsideReferences);

        traitMethodsProcessor.addTraitImportsForOutsideReferences(elementsToMove);
        traitMethodsProcessor.addTraitImportsForInsideReferences();

        List<RsMoveReferenceInfo> insideReferences = Arrays.stream(usages)
            .filter(u -> u instanceof RsPathUsageInfo)
            .map(u -> ((RsPathUsageInfo) u).getReferenceInfo())
            .collect(Collectors.toList());
        updateInsideReferenceInfosIfNeeded(insideReferences, pathMapping);
        retargetProcessor.retargetReferences(insideReferences);
        retargetProcessor.optimizeImports();
    }

    private void updateOutsideReferencesInVisRestrictions() {
        for (RsVisRestriction visRestriction : RsMoveUtil.movedElementsDeepDescendantsOfType(elementsToMove, RsVisRestriction.class)) {
            RsMoveUtil.updateScopeIfNecessary(visRestriction, psiFactory, targetMod);
        }
    }

    private void restoreOutsideReferenceInfosAfterMove(@NotNull Map<RsElement, RsElement> pathMapping) {
        for (RsMoveReferenceInfo reference : outsideReferences) {
            restorePathOldAfterMove(reference, pathMapping);
        }
    }

    private void updateInsideReferenceInfosIfNeeded(
        @NotNull List<RsMoveReferenceInfo> references,
        @NotNull Map<RsElement, RsElement> pathMapping
    ) {
        Map<RsQualifiedNamedElement, RsQualifiedNamedElement> targetMapping = createMapping(RS_TARGET_BEFORE_MOVE_KEY, RsQualifiedNamedElement.class);
        for (RsMoveReferenceInfo reference : references) {
            restorePathOldAfterMove(reference, pathMapping);

            RsQualifiedNamedElement targetRestored = targetMapping.get(reference.getTarget());
            if (targetRestored != null) {
                reference.setTarget(targetRestored);
            } else if (reference.getTarget().getContainingFile() instanceof DummyHolder) {
                RsMoveUtil.LOG.error("Can't restore target " + reference.getTarget()
                    + " for reference '" + reference.getPathOldOriginal().getText() + "' after move");
            }
        }
    }

    @NotNull
    private <T extends RsElement> Map<T, T> createMapping(@NotNull Key<T> key, @NotNull Class<T> aClass) {
        Map<T, T> result = new HashMap<>();
        for (T element : RsMoveUtil.movedElementsShallowDescendantsOfType(elementsToMove, aClass, true)) {
            T elementOld = element.getCopyableUserData(key);
            if (elementOld == null) continue;
            element.putCopyableUserData(key, null);
            result.put(elementOld, element);
        }
        return result;
    }

    private void restorePathOldAfterMove(@NotNull RsMoveReferenceInfo reference, @NotNull Map<RsElement, RsElement> pathMapping) {
        RsElement restored = pathMapping.get(reference.getPathOldOriginal());
        if (restored == null) return;
        reference.setPathOldOriginal(restored);
        reference.setPathOld(RsMoveUtil.convertFromPathOriginal(restored, codeFragmentFactory));
    }

    public void updateMovedItemVisibility(@NotNull RsItemElement item) {
        RsVisibility visibility = ((RsVisibilityOwner) item).getVisibility();
        if (visibility instanceof RsVisibility.Private) {
            if (conflictsDetector != null && conflictsDetector.getItemsToMakePublic().contains(item)) {
                String itemName = item.getName();
                com.intellij.psi.PsiFile containingFile = item.getContainingFile();
                if (!(item instanceof RsVisibilityOwner)) {
                    RsMoveUtil.LOG.error("Unexpected item to make public: " + item);
                    return;
                }
                var fix = MakePublicFix.createIfCompatible((RsVisibilityOwner) item, itemName, false);
                if (fix != null) {
                    fix.invoke(project, null, containingFile);
                }
            }
        } else if (visibility instanceof RsVisibility.Restricted) {
            RsVis vis = ((RsVisibilityOwner) item).getVis();
            if (vis != null && vis.getVisRestriction() != null) {
                RsMoveUtil.updateScopeIfNecessary(vis.getVisRestriction(), psiFactory, targetMod);
            }
        }
        // RsVisibility.Public -> already public, keep as is
    }
}
