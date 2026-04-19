/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.codeInspection.ContainerBasedSuppressQuickFix;
import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.ArrayList;
import java.util.List;

public class RsSuppressQuickFix extends LocalQuickFixOnPsiElement implements ContainerBasedSuppressQuickFix {

    private final RsLint myLint;
    private final String myTarget;

    public RsSuppressQuickFix(
        @NotNull RsDocAndAttributeOwner suppressAt,
        @NotNull RsLint lint,
        @NotNull String target
    ) {
        super(suppressAt);
        myLint = lint;
        myTarget = target;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.suppress.warnings");
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.suppress.for", myLint.getId(), myTarget);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @NotNull PsiElement context) {
        return context.isValid();
    }

    @Override
    public boolean isSuppressAll() {
        return false;
    }

    @Nullable
    @Override
    public PsiElement getContainer(@Nullable PsiElement context) {
        PsiElement startElement = getStartElement();
        if (startElement != null && !(startElement instanceof RsFile)) {
            return startElement;
        }
        return null;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile file, @NotNull PsiElement suppressAt, @NotNull PsiElement endElement) {
        Attribute attr = new Attribute("allow", myLint.getId());
        if (suppressAt instanceof RsOuterAttributeOwner) {
            RsOuterAttributeOwner outerOwner = (RsOuterAttributeOwner) suppressAt;
            List<RsOuterAttr> outerAttrs = outerOwner.getOuterAttrList();
            PsiElement anchor = !outerAttrs.isEmpty() ? outerAttrs.get(0) : suppressAt.getFirstChild();
            RsOuterAttributeOwnerUtil.addOuterAttribute(outerOwner, attr, anchor);
        } else if (suppressAt instanceof RsInnerAttributeOwner) {
            RsInnerAttributeOwner innerOwner = (RsInnerAttributeOwner) suppressAt;
            PsiElement anchor = null;
            for (PsiElement child : suppressAt.getChildren()) {
                if (!(child instanceof RsInnerAttr) && !(child instanceof PsiComment)) {
                    anchor = child;
                    break;
                }
            }
            if (anchor == null) return;
            RsOuterAttributeOwnerUtil.addInnerAttribute(innerOwner, attr, anchor);
        }
    }

    @NotNull
    public static RsSuppressQuickFix[] createSuppressFixes(@NotNull PsiElement element, @NotNull RsLint lint) {
        if (lint.levelFor(element) != RsLintLevel.WARN) return new RsSuppressQuickFix[0];
        List<RsSuppressQuickFix> fixes = new ArrayList<>();
        PsiElement ancestor = element;
        while (ancestor != null) {
            RsSuppressQuickFix action = createFixForAncestor(ancestor, lint);
            if (action != null) {
                fixes.add(action);
            }
            ancestor = ancestor.getParent();
        }
        return fixes.toArray(new RsSuppressQuickFix[0]);
    }

    @Nullable
    private static RsSuppressQuickFix createFixForAncestor(@NotNull PsiElement ancestor, @NotNull RsLint lint) {
        if (ancestor instanceof RsLetDecl || ancestor instanceof RsFieldDecl
            || ancestor instanceof RsEnumVariant || ancestor instanceof RsItemElement
            || ancestor instanceof RsFile) {
            String target = getTargetName(ancestor);
            if (target == null) return null;
            if (ancestor instanceof PsiNamedElement) {
                String name = ((PsiNamedElement) ancestor).getName();
                if (name != null) {
                    target += " " + name;
                }
            }
            return new RsSuppressQuickFix((RsDocAndAttributeOwner) ancestor, lint, target);
        } else if (ancestor instanceof RsExprStmt) {
            RsExpr expr = ((RsExprStmt) ancestor).getExpr();
            if (expr instanceof RsOuterAttributeOwner) {
                return new RsSuppressQuickFix((RsDocAndAttributeOwner) expr, lint, "statement");
            }
        }
        return null;
    }

    @Nullable
    private static String getTargetName(@NotNull PsiElement ancestor) {
        if (ancestor instanceof RsLetDecl) return "statement";
        if (ancestor instanceof RsFieldDecl) return "field";
        if (ancestor instanceof RsEnumVariant) return "enum variant";
        if (ancestor instanceof RsStructItem) return "struct";
        if (ancestor instanceof RsEnumItem) return "enum";
        if (ancestor instanceof RsFunction) return "fn";
        if (ancestor instanceof RsTypeAlias) return "type";
        if (ancestor instanceof RsConstant) return "const";
        if (ancestor instanceof RsModItem) return "mod";
        if (ancestor instanceof RsImplItem) return "impl";
        if (ancestor instanceof RsTraitItem) return "trait";
        if (ancestor instanceof RsUseItem) return "use";
        if (ancestor instanceof RsFile) return "file";
        return null;
    }
}
