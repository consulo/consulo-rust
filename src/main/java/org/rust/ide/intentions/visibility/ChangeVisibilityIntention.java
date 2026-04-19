/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.visibility;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.ide.intentions.RsElementBaseIntentionAction;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.PsiElementExtUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.PsiElementUtil;
import com.intellij.psi.util.PsiTreeUtil;

public abstract class ChangeVisibilityIntention extends RsElementBaseIntentionAction<ChangeVisibilityIntention.Context> {
    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.change.item.visibility");
    }

    @NotNull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @NotNull
    public abstract String getVisibility();

    public abstract boolean isApplicable(@NotNull RsVisibilityOwner element);

    public static class Context {
        private final RsVisibilityOwner myElement;

        public Context(@NotNull RsVisibilityOwner element) {
            this.myElement = element;
        }

        @NotNull
        public RsVisibilityOwner getElement() {
            return myElement;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        RsVisibilityOwner visibleElement = PsiElementExtUtil.ancestorStrict(element, RsVisibilityOwner.class);
        if (visibleElement == null) return null;
        if (!isValidVisibilityOwner(visibleElement)) return null;
        if (!isValidPlace(visibleElement, element)) return null;
        if (!isApplicable(visibleElement)) return null;

        String name = "";
        if (visibleElement instanceof RsNamedElement) {
            String n = ((RsNamedElement) visibleElement).getName();
            if (n != null) {
                name = " `" + n + "`";
            }
        }

        setText(RsBundle.message("intention.name.make", name, getVisibility()));

        return new Context(visibleElement);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
        if (ctx.getElement().isPublic()) {
            makePrivate(ctx.getElement());
        } else {
            makePublic(ctx.getElement(), true);
        }
    }

    public static boolean isValidVisibilityOwner(@NotNull RsVisibilityOwner visible) {
        if (visible instanceof RsEnumVariant) return false;
        if (RsElementUtil.getContainingCrate((RsElement) visible).getOrigin() != PackageOrigin.WORKSPACE) return false;

        if (visible instanceof RsAbstractable) {
            RsAbstractableOwner owner = RsAbstractableUtil.getOwner((RsAbstractable) visible);
            if (owner instanceof RsAbstractableOwner.Trait) return false;
            if (owner != null && RsAbstractableOwnerUtil.isTraitImpl(owner)) return false;
        }

        return true;
    }

    public static void makePrivate(@NotNull RsVisibilityOwner element) {
        RsVis vis = element.getVis();
        if (vis != null) {
            vis.delete();
        }
    }

    @Nullable
    public static PsiElement findInsertionAnchor(@NotNull RsVisibilityOwner element) {
        PsiElement anchor = getAnchor((PsiElement) element);
        while (true) {
            PsiElement prevNonCommentSibling = PsiElementUtil.getPrevNonCommentSibling(anchor);
            if (!(prevNonCommentSibling instanceof RsOuterAttr || prevNonCommentSibling == null)) {
                anchor = prevNonCommentSibling;
            } else {
                break;
            }
        }
        return anchor;
    }

    public static void makePublic(@NotNull RsVisibilityOwner element, boolean crateRestricted) {
        Project project = ((PsiElement) element).getProject();
        PsiElement anchor = findInsertionAnchor(element);

        RsPsiFactory factory = new RsPsiFactory(project);
        RsVis newVis = crateRestricted ? factory.createPubCrateRestricted() : factory.createPub();

        RsVis currentVis = element.getVis();
        if (currentVis != null) {
            currentVis.replace(newVis);
        } else {
            ((PsiElement) element).addBefore(newVis, anchor);

            if (crateRestricted) {
                if (anchor instanceof RsExternAbi || (anchor != null && anchor.getParent() instanceof RsNamedFieldDecl)) {
                    ((PsiElement) element).addBefore(new RsPsiFactory(project).createPubCrateRestricted().getNextSibling(), anchor);
                }
            }
        }
    }

    @Nullable
    private static PsiElement getAnchor(@NotNull PsiElement element) {
        if (element instanceof RsNameIdentifierOwner) {
            return ((RsNameIdentifierOwner) element).getNameIdentifier();
        } else if (element instanceof RsTupleFieldDecl) {
            return ((RsTupleFieldDecl) element).getTypeReference();
        } else if (element instanceof RsUseItem) {
            return ((RsUseItem) element).getUse();
        } else {
            return null;
        }
    }

    private static boolean isValidPlace(@NotNull RsVisibilityOwner visibleElement, @NotNull PsiElement element) {
        PsiElement anchor;
        if (visibleElement instanceof RsConstant) {
            anchor = ((RsConstant) visibleElement).getConst();
        } else if (visibleElement instanceof RsEnumItem) {
            anchor = ((RsEnumItem) visibleElement).getEnum();
        } else if (visibleElement instanceof RsFieldDecl) {
            anchor = element;
        } else if (visibleElement instanceof RsFunction) {
            anchor = ((RsFunction) visibleElement).getFn();
        } else if (visibleElement instanceof RsMacro2) {
            anchor = ((RsMacro2) visibleElement).getMacroKw();
        } else if (visibleElement instanceof RsModDeclItem) {
            anchor = ((RsModDeclItem) visibleElement).getMod();
        } else if (visibleElement instanceof RsModItem) {
            anchor = ((RsModItem) visibleElement).getMod();
        } else if (visibleElement instanceof RsStructItem) {
            anchor = ((RsStructItem) visibleElement).getStruct();
        } else if (visibleElement instanceof RsTraitAlias) {
            anchor = ((RsTraitAlias) visibleElement).getTrait();
        } else if (visibleElement instanceof RsTraitItem) {
            anchor = ((RsTraitItem) visibleElement).getTrait();
        } else if (visibleElement instanceof RsTypeAlias) {
            anchor = ((RsTypeAlias) visibleElement).getTypeKw();
        } else if (visibleElement instanceof RsUseItem) {
            anchor = element;
        } else {
            anchor = null;
        }

        if (anchor == null) return false;

        if (element == anchor) return true;
        for (PsiElement sibling : PsiElementUtil.getLeftSiblings(anchor)) {
            if (sibling.equals(element) || PsiTreeUtil.isAncestor(sibling, element, false)) {
                return true;
            }
        }
        return false;
    }
}
