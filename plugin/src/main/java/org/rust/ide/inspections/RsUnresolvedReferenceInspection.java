/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemHighlightType;
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel;
import consulo.application.util.registry.Registry;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.ide.fixes.QualifyPathFix;
import org.rust.ide.inspections.imports.AutoImportFix;
import org.rust.ide.utils.imports.ImportCandidate;
import org.rust.lang.core.macros.proc.ProcMacroApplicationService;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import consulo.localize.LocalizeValue;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.annotation.component.ExtensionImpl;
import org.rust.lang.utils.RsDiagnostic;

@ExtensionImpl
public class RsUnresolvedReferenceInspection extends RsLocalInspectionTool {

    public boolean ignoreWithoutQuickFix = true;

@Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitPath(@Nonnull RsPath path) {
                PathInfo pathInfo = processPath(path);
                if (pathInfo == null) return;
                if (pathInfo.myIsPathUnresolved || pathInfo.myContext != null) {
                    registerProblem(holder, path, pathInfo.myContext);
                }
            }

            @Override
            public void visitMethodCall(@Nonnull RsMethodCall methodCall) {
                boolean isMethodResolved = !methodCall.getReference().multiResolve().isEmpty();
                AutoImportFix.Context context = AutoImportFix.findApplicableContext(methodCall);

                if (!isMethodResolved || context != null) {
                    registerProblem(holder, methodCall, context);
                }
            }

            @SuppressWarnings("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            @Override
            public void visitExternCrateItem2(@Nonnull RsExternCrateItem externCrate) {
                if (externCrate.getReference().multiResolve().isEmpty()
                    && externCrate.getContainingCrate().getOrigin() == PackageOrigin.WORKSPACE) {
                    new org.rust.lang.utils.RsDiagnostic.CrateNotFoundError(
                        externCrate.getReferenceNameElement(),
                        externCrate.getReferenceName()
                    ).addToHolder(holder);
                }
            }
        };
    }

    private void registerProblem(
        @Nonnull RsProblemsHolder holder,
        @Nonnull RsReferenceElement element,
        @Nullable AutoImportFix.Context context
    ) {
        List<ImportCandidate> candidates = context != null ? context.getCandidates() : null;
        boolean showError = (candidates != null && !candidates.isEmpty())
            || (!isTypeDependentPath(element) && Registry.is("org.rust.insp.unresolved.reference.type.independent", true))
            || !ignoreWithoutQuickFix;
        if (!showError) return;

        if (shouldIgnoreUnresolvedReference(element)) return;

        String referenceName = element.getReferenceName();
        String description = referenceName == null
            ? RsBundle.message("inspection.message.unresolved.reference2")
            : RsBundle.message("inspection.message.unresolved.reference", referenceName);
        List<LocalQuickFix> fixes = createQuickFixes(candidates, element, context);

        PsiElement highlightedElement = element.getReferenceNameElement();
        if (highlightedElement == null) highlightedElement = element;
        holder.registerProblem(
            highlightedElement,
            description,
            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
            fixes.toArray(LocalQuickFix.EMPTY_ARRAY)
        );
    }

    @SuppressWarnings("RedundantIf")
    private static boolean isTypeDependentPath(@Nonnull RsReferenceElement element) {
        if (!(element instanceof RsPath)) return true;
        RsPath path = (RsPath) element;

        if (path.getTypeQual() != null) return true;

        RsPath qualifier = path.getPath();
        if (qualifier == null) return false;

        PsiElement resolvedQualifier = qualifier.getReference() != null ? qualifier.getReference().resolve() : null;
        if (resolvedQualifier == null) return true;

        if (resolvedQualifier instanceof RsMod) return false;

        if (resolvedQualifier instanceof RsEnumItem) {
            String refName = path.getReferenceName();
            if (refName != null && !refName.isEmpty() && Character.isUpperCase(refName.charAt(0))) {
                return false;
            }
        }

        return true;
    }

    @Nonnull
    public JComponent createOptionsPanel() {
        MultipleCheckboxOptionsPanel panel = new MultipleCheckboxOptionsPanel(this);
        panel.addCheckbox(RsBundle.message("checkbox.ignore.unresolved.references.with.possibly.high.false.positive.rate"), "ignoreWithoutQuickFix");
        return panel;
    }

    public static class PathInfo {
        public final boolean myIsPathUnresolved;
        @Nullable
        public final AutoImportFix.Context myContext;

        public PathInfo(boolean isPathUnresolved, @Nullable AutoImportFix.Context context) {
            this.myIsPathUnresolved = isPathUnresolved;
            this.myContext = context;
        }
    }

    @Nullable
    public static PathInfo processPath(@Nonnull RsPath path) {
        if (path.getReference() == null) return null;

        RsPath rootPath = RsPathUtil.rootPath(path);
        PsiElement rootPathParent = rootPath.getParent();
        if (rootPathParent instanceof RsMetaItem) {
            RsMetaItem metaItem = (RsMetaItem) rootPathParent;
            if (!RsMetaItemUtil.isMacroCall(metaItem) || !ProcMacroApplicationService.isFullyEnabled()) return null;
        }

        if (RsPathUtil.isInsideDocLink(path)) return null;

        boolean isPathUnresolved = RsPathUtil.getResolveStatus(path) != PathResolveStatus.RESOLVED;
        RsPath qualifier = RsPathUtil.getQualifier(path);

        AutoImportFix.Context context;
        if (qualifier == null && isPathUnresolved) {
            context = AutoImportFix.findApplicableContext(path);
        } else if (qualifier != null && isPathUnresolved) {
            if (RsPathUtil.getResolveStatus(qualifier) != PathResolveStatus.RESOLVED) return null;
            if (qualifier.getReference() != null && qualifier.getReference().multiResolve().size() > 1) return null;
            context = null;
        } else if ((qualifier != null || path.getTypeQual() != null) && !isPathUnresolved) {
            context = AutoImportFix.findApplicableContextForAssocItemPath(path);
        } else {
            context = null;
        }
        return new PathInfo(isPathUnresolved, context);
    }

    @Nonnull
    private static List<LocalQuickFix> createQuickFixes(
        @Nullable List<ImportCandidate> candidates,
        @Nonnull RsReferenceElement element,
        @Nullable AutoImportFix.Context context
    ) {
        if (context == null) return java.util.Collections.emptyList();

        List<LocalQuickFix> fixes = new ArrayList<>();
        if (candidates != null && !candidates.isEmpty()) {
            fixes.add(new AutoImportFix(element, context));

            if (element instanceof RsPath && context.getType() == AutoImportFix.Type.GENERAL_PATH && candidates.size() == 1) {
                fixes.add(new QualifyPathFix((RsPath) element, candidates.get(0).getInfo()));
            }
        }
        return fixes;
    }

    public static boolean shouldIgnoreUnresolvedReference(@Nonnull RsElement element) {
        return element.getContainingCrate().getHasCyclicDevDependencies() && RsElementUtil.isUnderCfgTest(element);
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.unresolved.reference.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }
}
