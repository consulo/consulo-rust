/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractFunction;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.presentation.RenderingUtil;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsUnaryExpr;
import org.rust.lang.core.psi.RsValueArgumentList;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.UnaryOperator;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.TypeUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyReference;

import java.util.List;
import org.rust.lang.core.psi.ext.RsPatBindingUtil;
import org.rust.lang.core.psi.ext.RsUnaryExprUtil;
import org.rust.ide.presentation.TypeRendering;
import org.rust.lang.core.types.RsTypesUtil;

public class Parameter {
    @NotNull
    private String myName;
    @Nullable
    private final Ty myType;
    private final boolean myIsReference;
    private boolean myIsMutable;
    private final boolean myRequiresMut;
    private boolean myIsSelected;
    @NotNull
    private final String myOriginalName;
    @NotNull
    private final String myTypeText;

    private Parameter(
        @NotNull String name,
        @Nullable Ty type,
        boolean isReference,
        boolean isMutable,
        boolean requiresMut,
        boolean isSelected
    ) {
        myName = name;
        myType = type;
        myIsReference = isReference;
        myIsMutable = isMutable;
        myRequiresMut = requiresMut;
        myIsSelected = isSelected;
        myOriginalName = name;
        myTypeText = type != null ? (TypeRendering.renderInsertionSafe(type) != null ? TypeRendering.renderInsertionSafe(type) : "") : "";
    }

    @NotNull
    public String getName() {
        return myName;
    }

    public void setName(@NotNull String name) {
        myName = name;
    }

    @Nullable
    public Ty getType() {
        return myType;
    }

    public boolean isMutable() {
        return myIsMutable;
    }

    public void setMutable(boolean mutable) {
        myIsMutable = mutable;
    }

    public boolean isSelected() {
        return myIsSelected;
    }

    public void setSelected(boolean selected) {
        myIsSelected = selected;
    }

    public boolean isSelf() {
        return myType == null;
    }

    @NotNull
    private String getMutText() {
        return myIsMutable && (!myIsReference || myRequiresMut) ? "mut " : "";
    }

    @NotNull
    private String getReferenceText() {
        if (!myIsReference) return "";
        return myIsMutable ? "&mut " : "&";
    }

    @NotNull
    public String getOriginalParameterText() {
        if (myType != null) {
            return getMutText() + myOriginalName + ": " + getReferenceText() + myTypeText;
        }
        return myOriginalName;
    }

    @NotNull
    public String getParameterText() {
        if (myType != null) {
            return getMutText() + myName + ": " + getReferenceText() + myTypeText;
        }
        return myName;
    }

    @NotNull
    public String getArgumentText() {
        return getReferenceText() + myOriginalName;
    }

    @NotNull
    public static Parameter self(@NotNull String name) {
        return new Parameter(name, null, false, false, false, true);
    }

    @NotNull
    public static Parameter build(
        @NotNull RsPatBinding binding,
        @NotNull List<PsiReference> references,
        boolean isUsedAfterEnd,
        @NotNull ImplLookup implLookup
    ) {
        boolean hasRefOperator = false;
        for (PsiReference ref : references) {
            RsUnaryExpr unaryExpr = RsElementUtil.ancestorStrict(ref.getElement(), RsUnaryExpr.class);
            if (unaryExpr != null) {
                UnaryOperator opType = RsUnaryExprUtil.getOperatorType(unaryExpr);
                if (opType == UnaryOperator.REF || opType == UnaryOperator.REF_MUT) {
                    hasRefOperator = true;
                    break;
                }
            }
        }
        Ty bindingType = RsTypesUtil.getType(binding);
        boolean requiredBorrowing = hasRefOperator
            || (isUsedAfterEnd && !(bindingType instanceof TyReference) && !implLookup.isCopy(bindingType).isTrue());

        boolean requiredMutableValue = RsPatBindingUtil.getMutability(binding).isMut();
        if (requiredMutableValue) {
            boolean anyMutRef = false;
            for (PsiReference ref : references) {
                if (RsElementUtil.ancestorStrict(ref.getElement(), RsValueArgumentList.class) == null) continue;
                RsUnaryExpr unary = RsElementUtil.ancestorStrict(ref.getElement(), RsUnaryExpr.class);
                if (unary == null || RsUnaryExprUtil.getOperatorType(unary) == UnaryOperator.REF_MUT) {
                    anyMutRef = true;
                    break;
                }
            }
            requiredMutableValue = anyMutRef;
        }

        boolean reference;
        if (requiredMutableValue) {
            reference = requiredBorrowing;
        } else if (RsPatBindingUtil.getMutability(binding).isMut()) {
            reference = true;
        } else {
            reference = requiredBorrowing;
        }
        boolean mutable;
        if (requiredMutableValue) {
            mutable = true;
        } else {
            mutable = RsPatBindingUtil.getMutability(binding).isMut();
        }

        return new Parameter(binding.getReferenceName(), bindingType, reference, mutable, requiredMutableValue, true);
    }
}
