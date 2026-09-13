/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import consulo.document.util.TextRange;
import consulo.language.psi.MultiRangeReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.impl.AssignmentOp;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.resolve.Selection;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.TraitRef;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.psi.ext.impl.RsTraitOrImplUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.impl.RsIndexExprUtil;
import org.rust.lang.core.psi.ext.impl.RsBinaryExprUtil;
import consulo.language.psi.PsiElement;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;

public class RsIndexExprReferenceImpl extends RsReferenceCached<RsIndexExpr> implements MultiRangeReference {

    public RsIndexExprReferenceImpl(@Nonnull RsIndexExpr element) {
        super(element);
    }

    @Nonnull
    @Override
    protected ResolveCacheDependency getCacheDependency() {
        return ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE;
    }

    @Nonnull
    @Override
    protected List<RsElement> resolveInner() {
        consulo.language.psi.PsiElement parent = getElement().getParent();

        boolean isMaybeMutableContext = parent instanceof RsBinaryExpr
            && RsBinaryExprUtil.getOperatorType((RsBinaryExpr) parent) instanceof AssignmentOp
            && ((RsBinaryExpr) parent).getLeft() == getElement();

        RsFunction indexFn = findIndexFunction(getElement(), isMaybeMutableContext);
        if (indexFn != null && org.rust.lang.core.psi.ext.impl.RsElementUtil.existsAfterExpansion(indexFn)) {
            return Collections.singletonList(indexFn);
        }
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<TextRange> getRanges() {
        List<TextRange> ranges = new ArrayList<>(2);
        ranges.add(getElement().getLbrack().getTextRangeInParent());
        ranges.add(getElement().getRbrack().getTextRangeInParent());
        return ranges;
    }

    /**
     * The {@code Index::index} or {@code IndexMut::index_mut} function that an index expression calls,
     * or {@code null} if neither trait is implemented for the indexed type. When {@code preferMutable}
     * is set, {@code IndexMut} is tried first.
     */
    @Nullable
    public static RsFunction findIndexFunction(@Nonnull RsIndexExpr element, boolean preferMutable) {
        RsExpr container = RsIndexExprUtil.getContainerExpr(element);
        RsExpr index = RsIndexExprUtil.getIndexExpr(element);
        if (index == null) return null;

        ImplLookup lookup = ExtensionsUtil.getImplLookup(element);
        KnownItems items = KnownItems.getKnownItems(element);

        RsTraitItem[] traits;
        String[] functionNames;
        if (preferMutable) {
            traits = new RsTraitItem[]{items.getIndexMut(), items.getIndex()};
            functionNames = new String[]{"index_mut", "index"};
        }
        else {
            traits = new RsTraitItem[]{items.getIndex(), items.getIndexMut()};
            functionNames = new String[]{"index", "index_mut"};
        }

        Ty containerTy = ExtensionsUtil.getType(container);
        Ty indexTy = ExtensionsUtil.getType(index);
        for (int i = 0; i < traits.length; i++) {
            RsTraitItem trait = traits[i];
            if (trait == null) continue;
            Selection selection = lookup.select(new TraitRef(containerTy, new BoundElement<>(trait).withSubst(indexTy))).ok();
            if (selection == null) continue;
            for (RsAbstractable member : RsTraitOrImplUtil.getExpandedMembers(selection.getImpl())) {
                if (member instanceof RsFunction && functionNames[i].equals(member.getName())) {
                    return (RsFunction) member;
                }
            }
        }
        return null;
    }
}
