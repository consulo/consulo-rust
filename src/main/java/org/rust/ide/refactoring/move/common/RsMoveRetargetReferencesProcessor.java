/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import org.rust.ide.refactoring.move.common.RsMoveUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.refactoring.RsImportOptimizer;
import org.rust.ide.utils.imports.ImportUtils;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.*;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsUseSpeckUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

public class RsMoveRetargetReferencesProcessor {

    @NotNull
    private final RsPsiFactory psiFactory;
    @NotNull
    private final RsCodeFragmentFactory codeFragmentFactory;
    @NotNull
    private final RsMod sourceMod;
    @NotNull
    private final RsMod targetMod;
    @NotNull
    private final Set<RsFile> filesToOptimizeImports = new HashSet<>();

    public RsMoveRetargetReferencesProcessor(
        @NotNull Project project,
        @NotNull RsMod sourceMod,
        @NotNull RsMod targetMod
    ) {
        this.psiFactory = new RsPsiFactory(project);
        this.codeFragmentFactory = new RsCodeFragmentFactory(project);
        this.sourceMod = sourceMod;
        this.targetMod = targetMod;
        filesToOptimizeImports.add((RsFile) sourceMod.getContainingFile());
        filesToOptimizeImports.add((RsFile) targetMod.getContainingFile());
    }

    public void retargetReferences(@NotNull List<RsMoveReferenceInfo> referencesAll) {
        List<RsMoveReferenceInfo> referencesDirectly = new ArrayList<>();
        List<RsMoveReferenceInfo> referencesOther = new ArrayList<>();

        for (RsMoveReferenceInfo ref : referencesAll) {
            if (ref.isInsideUseDirective() || ref.isForceReplaceDirectly()) {
                referencesDirectly.add(ref);
            } else {
                referencesOther.add(ref);
            }
        }

        for (RsMoveReferenceInfo reference : referencesDirectly) {
            retargetReferenceDirectly(reference);
        }
        for (RsMoveReferenceInfo reference : referencesOther) {
            RsPath pathOld = reference.getPathOld();
            if (RsMoveUtil.resolvesToAndAccessible(pathOld, reference.getTarget())) continue;
            boolean success = !RsMoveUtil.isAbsolute(pathOld) && tryRetargetReferenceKeepExistingStyle(reference);
            if (!success) {
                retargetReferenceDirectly(reference);
            }
        }
    }

    private void retargetReferenceDirectly(@NotNull RsMoveReferenceInfo reference) {
        RsPath pathNew = reference.getPathNew();
        if (pathNew == null) return;
        replacePathOld(reference, pathNew);
    }

    private boolean tryRetargetReferenceKeepExistingStyle(@NotNull RsMoveReferenceInfo reference) {
        RsPath pathOld = reference.getPathOld();
        RsPath pathNew = reference.getPathNew();
        if (pathNew == null) return false;

        String[] pathOldSegments = RsMoveUtil.getTextNormalized(pathOld).split("::");
        String[] pathNewSegments = RsMoveUtil.getTextNormalized(pathNew).split("::");

        int pathNewShortNumberSegments = adjustPathNewNumberSegments(reference, pathOldSegments.length);
        return doRetargetReferenceKeepExistingStyle(reference, pathNewSegments, pathNewShortNumberSegments);
    }

    private boolean doRetargetReferenceKeepExistingStyle(
        @NotNull RsMoveReferenceInfo reference,
        @NotNull String[] pathNewSegments,
        int pathNewShortNumberSegments
    ) {
        if (pathNewShortNumberSegments >= pathNewSegments.length) return false;

        StringBuilder pathNewShortBuilder = new StringBuilder();
        for (int i = pathNewSegments.length - pathNewShortNumberSegments; i < pathNewSegments.length; i++) {
            if (pathNewShortBuilder.length() > 0) pathNewShortBuilder.append("::");
            pathNewShortBuilder.append(pathNewSegments[i]);
        }
        String pathNewShortText = pathNewShortBuilder.toString();

        StringBuilder usePathBuilder = new StringBuilder();
        for (int i = 0; i <= pathNewSegments.length - pathNewShortNumberSegments; i++) {
            if (usePathBuilder.length() > 0) usePathBuilder.append("::");
            usePathBuilder.append(pathNewSegments[i]);
        }
        String usePath = usePathBuilder.toString();

        String alias = null;
        if (reference.getPathOld().getColoncolon() == null && pathNewShortNumberSegments == 1) {
            String referenceName = reference.getPathOld().getReferenceName();
            if (referenceName != null && !referenceName.equals(pathNewShortText)) {
                alias = referenceName;
            }
        }

        RsMod containingMod = reference.getPathOldOriginal().getContainingMod();
        String textToResolve = alias != null ? alias : pathNewShortText;
        RsPath pathNewShort = RsMoveUtil.toRsPath(textToResolve, codeFragmentFactory, containingMod);
        if (pathNewShort == null) return false;

        RsPath basePath = RsPathUtil.basePath(pathNewShort);
        com.intellij.psi.PsiElement baseResolved = basePath.getReference() != null ? basePath.getReference().resolve() : null;
        RsPath usePathAsPath = RsMoveUtil.toRsPath(usePath, codeFragmentFactory, containingMod);
        com.intellij.psi.PsiElement elementToImport = usePathAsPath != null && usePathAsPath.getReference() != null
            ? usePathAsPath.getReference().resolve() : null;
        boolean containingModHasSameNameInScope = baseResolved != null && baseResolved != elementToImport && elementToImport != null;

        if (containingModHasSameNameInScope) {
            return doRetargetReferenceKeepExistingStyle(reference, pathNewSegments, pathNewShortNumberSegments + 1);
        }
        addImport(reference.getPathOldOriginal(), usePath, alias);
        replacePathOld(reference, pathNewShort);
        return true;
    }

    private int adjustPathNewNumberSegments(@NotNull RsMoveReferenceInfo reference, int numberSegments) {
        RsElement pathOldOriginal = reference.getPathOldOriginal();
        RsQualifiedNamedElement target = reference.getTarget();

        if (RsMoveUtil.startsWithSuper(reference.getPathOld())) {
            return target instanceof RsFunction ? 2 : 1;
        }

        if (numberSegments != 1 || !(target instanceof RsFunction)) return numberSegments;
        boolean isReferenceBetweenElementsInSourceMod =
            (pathOldOriginal.getContainingMod() == sourceMod && RsMoveUtil.getContainingModStrict(target) == targetMod)
                || (pathOldOriginal.getContainingMod() == targetMod && RsMoveUtil.getContainingModStrict(target) == sourceMod);
        return isReferenceBetweenElementsInSourceMod ? 2 : numberSegments;
    }

    private void replacePathOld(@NotNull RsMoveReferenceInfo reference, @NotNull RsPath pathNew) {
        RsPath pathOld = reference.getPathOld();
        RsElement pathOldOriginal = reference.getPathOldOriginal();

        if (!(pathOldOriginal instanceof RsPath)) {
            replacePathOldInPatIdent((RsPatIdent) pathOldOriginal, pathNew);
            return;
        }

        if (tryReplacePathOldInUseGroup((RsPath) pathOldOriginal, pathNew)) return;

        if (RsMoveUtil.getTextNormalized(pathOld).equals(RsMoveUtil.getTextNormalized(pathNew))) return;
        if (pathOld != pathOldOriginal) {
            if (!RsMoveUtil.getTextNormalized((RsPath) pathOldOriginal).startsWith(RsMoveUtil.getTextNormalized(pathOld))) {
                RsMoveUtil.LOG.error("Expected '" + pathOldOriginal.getText() + "' to start with '" + pathOld.getText() + "'");
            } else if (replacePathOldWithTypeArguments((RsPath) pathOldOriginal, RsMoveUtil.getTextNormalized(pathNew))) {
                return;
            }
        }

        if (pathOld.getParent() instanceof RsUseSpeck && pathOld.getParent().getParent() instanceof RsUseItem && !pathNew.getHasColonColon()) {
            pathOld.getParent().getParent().delete();
            return;
        }
        pathOldOriginal.replace(pathNew);
    }

    private void replacePathOldInPatIdent(@NotNull RsPatIdent pathOldOriginal, @NotNull RsPath pathNew) {
        if (pathNew.getColoncolon() != null) {
            RsMoveUtil.LOG.error("Expected paths in patIdent to be one-segment, got: '" + pathNew.getText() + "'");
            return;
        }
        String referenceName = pathNew.getReferenceName();
        if (referenceName == null) throw new IllegalStateException("Generated paths can't be incomplete");
        com.intellij.psi.PsiElement patBindingNew = psiFactory.createIdentifier(referenceName);
        pathOldOriginal.getPatBinding().getIdentifier().replace(patBindingNew);
    }

    private boolean replacePathOldWithTypeArguments(@NotNull RsPath pathOldOriginal, @NotNull String pathNewText) {
        if (pathOldOriginal.getTypeArgumentList() == null) return false;
        RsPath pathOldCopy = (RsPath) pathOldOriginal;
        if (pathOldCopy.getPath() != null) pathOldCopy.getPath().delete();
        if (pathOldCopy.getColoncolon() != null) pathOldCopy.getColoncolon().delete();
        if (pathOldCopy.getReferenceNameElement() != null) pathOldCopy.getReferenceNameElement().delete();

        RsPath pathNew = RsMoveUtil.toRsPath(pathNewText, psiFactory);
        if (pathNew == null) return false;
        List<com.intellij.psi.PsiElement> elements = new ArrayList<>();
        if (pathNew.getPath() != null) elements.add(pathNew.getPath());
        if (pathNew.getColoncolon() != null) elements.add(pathNew.getColoncolon());
        if (pathNew.getReferenceNameElement() != null) elements.add(pathNew.getReferenceNameElement());
        for (int i = elements.size() - 1; i >= 0; i--) {
            pathOldOriginal.addAfter(elements.get(i), null);
        }
        return true;
    }

    private boolean tryReplacePathOldInUseGroup(@NotNull RsPath pathOld, @NotNull RsPath pathNew) {
        RsUseSpeck useSpeck = RsElementUtil.ancestorStrict(pathOld, RsUseSpeck.class);
        if (useSpeck == null) return false;
        if (!(useSpeck.getParent() instanceof RsUseGroup)) return false;
        RsUseItem useItem = RsElementUtil.ancestorStrict(useSpeck, RsUseItem.class);
        if (useItem == null) return false;

        pathOld.replace(pathNew);
        String useSpeckText = useSpeck.getText();
        RsUseSpeckUtil.deleteWithSurroundingComma(useSpeck);
        if (useSpeckText.contains("::")) {
            insertUseItemAndCopyAttributes(useSpeckText, useItem);
        }

        RsUseGroup useGroup = (RsUseGroup) useSpeck.getParent();
        filesToOptimizeImports.add((RsFile) RsUseSpeckUtil.getParentUseSpeck(useGroup).getContainingFile());
        return true;
    }

    private void insertUseItemAndCopyAttributes(@NotNull String useSpeckText, @NotNull RsUseItem existingUseItem) {
        RsMod containingMod = existingUseItem.getContainingMod();
        RsUseItem useItem = (RsUseItem) existingUseItem.copy();
        RsUseSpeck useSpeck = psiFactory.createUseSpeck(useSpeckText);
        if (useItem.getUseSpeck() != null) {
            useItem.getUseSpeck().replace(useSpeck);
        }
        ImportUtils.insertUseItem(containingMod, psiFactory, useItem);
        filesToOptimizeImports.add((RsFile) containingMod.getContainingFile());
    }

    private void addImport(@NotNull RsElement context, @NotNull String usePath, @Nullable String alias) {
        RsMoveUtil.addImport(psiFactory, context, usePath, alias);
        filesToOptimizeImports.add((RsFile) context.getContainingFile());
    }

    public void optimizeImports() {
        for (RsFile file : filesToOptimizeImports) {
            RsImportOptimizer.optimizeUseItems(file);
        }
    }
}
