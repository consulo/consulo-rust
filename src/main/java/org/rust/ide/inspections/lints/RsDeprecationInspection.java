/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.psi.PsiElement;
import io.github.z4kn4fein.semver.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsMetaItemArgs;
import io.github.z4kn4fein.semver.StringExtensionsKt;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.*;

import java.util.List;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil;
import org.rust.lang.core.psi.ext.RsElement;

public class RsDeprecationInspection extends RsLintInspection {

    private static final String DEPRECATED_ATTR_NAME = "deprecated";
    private static final String SINCE_PARAM_NAME = "since";
    private static final String NOTE_PARAM_NAME = "note";
    private static final String REASON_PARAM_NAME = "reason";

    @NotNull
    @Override
    public String getDisplayName() {
        return RsBundle.message("deprecated.item");
    }

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.Deprecated;
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitElement(@NotNull RsElement ref) {
                // item is non-inline module declaration or not reference element
                if (ref instanceof RsModDeclItem || !(ref instanceof RsReferenceElement)) return;

                RsReferenceElement refElement = (RsReferenceElement) ref;
                PsiElement original = refElement.getReference() != null ? refElement.getReference().resolve() : null;
                if (original == null) return;
                PsiElement identifier = refElement.getReferenceNameElement();
                if (identifier == null) return;

                // ignore `Self` identifier
                if (identifier.getNode().getElementType() == RsElementTypes.CSELF) return;

                PsiElement targetElement;
                if (original instanceof RsFile) {
                    targetElement = ((RsFile) original).getDeclaration();
                } else if (original instanceof RsAbstractable) {
                    RsAbstractable abstractable = (RsAbstractable) original;
                    targetElement = abstractable.getOwner().isTraitImpl() ? abstractable.getSuperItem() : original;
                } else {
                    targetElement = original;
                }
                if (targetElement == null) return;

                checkAndRegisterAsDeprecated(identifier, targetElement, holder);
            }
        };
    }

    private void checkAndRegisterAsDeprecated(
        @NotNull PsiElement identifier,
        @NotNull PsiElement original,
        @NotNull RsProblemsHolder holder
    ) {
        if (original instanceof RsOuterAttributeOwner) {
            RsMetaItem attr = RsDocAndAttributeOwnerUtil.getQueryAttributes((RsDocAndAttributeOwner) original)
                .getDeprecatedAttribute();
            if (attr == null) return;
            DeprecatedMessage result = extractDeprecatedMessage(attr, identifier.getText());
            registerLintProblem(holder, identifier, result.myMessage, result.myHighlightType);
        }
    }

    @NotNull
    private DeprecatedMessage extractDeprecatedMessage(@NotNull RsMetaItem metaItem, @NotNull String item) {
        String noteParamName;
        if (DEPRECATED_ATTR_NAME.equals(metaItem.getName())) {
            noteParamName = NOTE_PARAM_NAME;
        } else {
            noteParamName = REASON_PARAM_NAME;
        }
        DeprecatedAttribute depAttr = extract(metaItem, noteParamName, SINCE_PARAM_NAME);

        if (isPresentlyDeprecated(metaItem, depAttr.mySince)) {
            StringBuilder sb = new StringBuilder();
            sb.append("`").append(item).append("` is deprecated");
            if (depAttr.mySince != null) sb.append(" since ").append(depAttr.mySince);
            if (depAttr.myNote != null) sb.append(": ").append(depAttr.myNote);
            return new DeprecatedMessage(sb.toString(), RsLintHighlightingType.DEPRECATED);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("`").append(item).append("` will be deprecated from ").append(depAttr.mySince);
            if (depAttr.myNote != null) sb.append(": ").append(depAttr.myNote);
            return new DeprecatedMessage(sb.toString(), RsLintHighlightingType.WEAK_WARNING);
        }
    }

    // Presently as in not in the future; in the current version
    private boolean isPresentlyDeprecated(@NotNull RsMetaItem metaItem, @Nullable String since) {
        // In case we can't check if the `sinceVersion` is at least the `currentVersion` just assume it is
        if (since == null) return true;
        Version sinceVersion = StringExtensionsKt.toVersionOrNull(since, false);
        if (sinceVersion == null) return true;
        org.rust.cargo.project.workspace.CargoWorkspace.Package pkg = RsElementUtil.getContainingCargoPackage(metaItem);
        if (pkg == null) return true;
        Version currentVersion = StringExtensionsKt.toVersionOrNull(pkg.getVersion(), true);
        if (currentVersion == null) return true;
        return currentVersion.compareTo(sinceVersion) >= 0;
    }

    @NotNull
    private DeprecatedAttribute extract(@NotNull RsMetaItem metaItem, @NotNull String noteParamName, @NotNull String sinceParamName) {
        RsMetaItemArgs args = metaItem.getMetaItemArgs();
        List<RsMetaItem> params = args != null ? args.getMetaItemList() : null;
        String note = params != null ? getByName(params, noteParamName) : null;
        String since = params != null ? getByName(params, sinceParamName) : null;
        return new DeprecatedAttribute(note, since);
    }

    @Nullable
    private String getByName(@NotNull List<RsMetaItem> items, @NotNull String name) {
        for (RsMetaItem item : items) {
            if (name.equals(item.getName())) {
                return item.getValue();
            }
        }
        return null;
    }

    private static class DeprecatedAttribute {
        final String myNote;
        final String mySince;

        DeprecatedAttribute(@Nullable String note, @Nullable String since) {
            myNote = note;
            mySince = since;
        }
    }

    private static class DeprecatedMessage {
        final String myMessage;
        final RsLintHighlightingType myHighlightType;

        DeprecatedMessage(@NotNull String message, @NotNull RsLintHighlightingType highlightType) {
            myMessage = message;
            myHighlightType = highlightType;
        }
    }
}
