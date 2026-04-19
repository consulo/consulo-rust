/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.navigation.goto_;

import com.intellij.codeInsight.TargetElementEvaluatorEx2;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ref.*;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.ty.Ty;

import java.util.List;
import java.util.Map;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElementUtil;

public class RsTargetElementEvaluator extends TargetElementEvaluatorEx2 {

    /**
     * Allows to intercept platform calls to {@link PsiReference#resolve()}
     * <p>
     * Note that if this method returns null, it means
     * "use default logic", i.e. call {@code ref.resolve()}
     */
    @Override
    @Nullable
    public PsiElement getElementByReference(@NotNull PsiReference ref, int flags) {
        if (!(ref instanceof RsReference)) return null;

        // prefer pattern binding to its target if element name is accepted
        if (ref instanceof RsPatBindingReferenceImpl && BitUtil.isSet(flags, TargetElementUtil.ELEMENT_NAME_ACCEPTED)) {
            return ref.getElement();
        }

        if (ref instanceof RsPathReference) {
            PsiElement resolved = tryResolveTypeAliasToImpl((RsPathReference) ref);
            if (resolved != null) {
                return resolved;
            }
        }

        // Filter invocations from CtrlMouseHandler (see RsQuickNavigationInfoTest)
        // and leave invocations from GotoDeclarationAction only.
        if (!RsGoToDeclarationRunningService.getInstance().isGoToDeclarationAction()) return null;

        return tryResolveToDeriveMetaItem(ref);
    }

    @Nullable
    private PsiElement tryResolveTypeAliasToImpl(@NotNull RsPathReference ref) {
        return RsPathReferenceImpl.tryResolveTypeAliasToImpl(ref);
    }

    @Nullable
    private PsiElement tryResolveToDeriveMetaItem(@NotNull PsiReference ref) {
        PsiElement resolved = ref.resolve();
        if (!(resolved instanceof RsAbstractable)) return null;
        RsAbstractable target = (RsAbstractable) resolved;
        RsAbstractableOwner owner = RsAbstractableImplUtil.getOwner(target);
        if (!(owner instanceof RsAbstractableOwner.Trait)) return null;
        RsTraitItem trait = ((RsAbstractableOwner.Trait) owner).getTrait();

        PsiElement element = ref.getElement();
        RsStructOrEnumItemElement item;
        if (element instanceof RsPath) {
            RsPath path = (RsPath) element;
            RsPath parentPath = path.getPath();
            if (parentPath == null) return null;
            PsiReference parentRef = parentPath.getReference();
            if (!(parentRef instanceof RsPathReference)) return null;
            PsiElement deepResolved = RsPathReferenceImpl.deepResolve((RsPathReference) parentRef);
            item = deepResolved instanceof RsStructOrEnumItemElement ? (RsStructOrEnumItemElement) deepResolved : null;
        } else {
            Ty receiver;
            if (element instanceof RsMethodCall) {
                RsMethodCall methodCall = (RsMethodCall) element;
                RsDotExpr dotExpr = (RsDotExpr) methodCall.getParent();
                receiver = RsTypesUtil.getType(dotExpr.getExpr());
            } else if (element instanceof RsBinaryOp) {
                PsiElement parent = element.getParent();
                if (!(parent instanceof RsBinaryExpr)) return null;
                RsBinaryExpr binaryExpr = (RsBinaryExpr) parent;
                receiver = RsTypesUtil.getType(binaryExpr.getLeft());
            } else {
                return null;
            }
            item = receiver instanceof TyAdt ? ((TyAdt) receiver).getItem() : null;
        }

        if (item == null) return null;
        Map<RsTraitItem, RsMetaItem> derivedTraitsToMetaItems = RsStructOrEnumItemElementUtil.getDerivedTraitsToMetaItems(item);
        return derivedTraitsToMetaItems.get(trait);
    }

    /**
     * Used to get parent named element when {@code element} is a name identifier
     * <p>
     * Note that if this method returns null, it means "use default logic"
     */
    @Override
    @Nullable
    public PsiElement getNamedElement(@NotNull PsiElement element) {
        // This hack enables some actions (e.g. "find usages") when the element is inside a macro
        // call and this element expands to name identifier of some named element.
        IElementType elementType = element.getNode().getElementType();
        if (elementType == RsElementTypes.IDENTIFIER || elementType == RsElementTypes.QUOTE_IDENTIFIER) {
            List<PsiElement> expansionElements = org.rust.lang.core.macros.RsExpandedElementUtil.findExpansionElements(element);
            if (expansionElements == null || expansionElements.isEmpty()) return null;
            PsiElement delegate = expansionElements.get(0);
            PsiElement delegateParent = delegate.getParent();
            if (delegateParent instanceof RsNameIdentifierOwner) {
                RsNameIdentifierOwner namedOwner = (RsNameIdentifierOwner) delegateParent;
                if (namedOwner.getNameIdentifier() == delegate) {
                    return delegateParent;
                }
            }
        }

        return null;
    }
}
