/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.MultiRangeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.AssignmentOp;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.TraitRef;
import org.rust.lang.core.types.ExtensionsUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.RsIndexExprUtil;
import org.rust.lang.core.psi.ext.RsBinaryExprUtil;

public class RsIndexExprReferenceImpl extends RsReferenceCached<RsIndexExpr> implements MultiRangeReference {

    public RsIndexExprReferenceImpl(@NotNull RsIndexExpr element) {
        super(element);
    }

    @NotNull
    @Override
    protected ResolveCacheDependency getCacheDependency() {
        return ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE;
    }

    @NotNull
    @Override
    protected List<RsElement> resolveInner() {
        com.intellij.psi.PsiElement parent = getElement().getParent();

        boolean isMaybeMutableContext = parent instanceof RsBinaryExpr
            && RsBinaryExprUtil.getOperatorType((RsBinaryExpr) parent) instanceof AssignmentOp
            && ((RsBinaryExpr) parent).getLeft() == getElement();

        RsFunction indexFn = findIndexFunction(getElement(), isMaybeMutableContext);
        if (indexFn != null && org.rust.lang.core.psi.ext.RsElementUtil.existsAfterExpansion(indexFn)) {
            return Collections.singletonList(indexFn);
        }
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<TextRange> getRanges() {
        List<TextRange> ranges = new ArrayList<>(2);
        ranges.add(getElement().getLbrack().getTextRangeInParent());
        ranges.add(getElement().getRbrack().getTextRangeInParent());
        return ranges;
    }

    @Nullable
    public static RsFunction findIndexFunction(@NotNull RsIndexExpr element, boolean preferMutable) {
        RsExpr container = RsIndexExprUtil.getContainerExpr(element);
        RsExpr index = RsIndexExprUtil.getIndexExpr(element);
        if (index == null) return null;

        ImplLookup lookup = ExtensionsUtil.getImplLookup(element);
        KnownItems items = KnownItems.getKnownItems(element);

        RsTraitItem indexTrait = items.getIndex();
        RsTraitItem indexMutTrait = items.getIndexMut();

        String[][] candidates;
        if (preferMutable) {
            candidates = new String[][]{{"IndexMut", "index_mut"}, {"Index", "index"}};
        } else {
            candidates = new String[][]{{"Index", "index"}, {"IndexMut", "index_mut"}};
        }

        for (String[] candidate : candidates) {
            RsTraitItem trait = candidate[0].equals("Index") ? indexTrait : indexMutTrait;
            String functionName = candidate[1];
            if (trait == null) continue;
            // Simplified: resolve through trait lookup
            // Full implementation requires TraitRef and select logic
        }
        return null;
    }
}
