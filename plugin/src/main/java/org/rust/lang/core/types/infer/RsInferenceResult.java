/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.ref.MethodResolveVariant;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.SubstitutionUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyFunctionBase;
import org.rust.lang.core.types.ty.TyUnknown;
import org.rust.lang.utils.RsDiagnostic;

import java.util.*;

/**
 * RsInferenceResult is an immutable per-function map from expressions to their types.
 */
public class RsInferenceResult implements RsInferenceData {
    public static final RsInferenceResult EMPTY = new RsInferenceResult(
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptySet(),
        Collections.emptyList()
    );

    @Nonnull
    private final Map<RsExpr, Ty> myExprTypes;
    @Nonnull
    private final Map<RsPat, Ty> myPatTypes;
    @Nonnull
    private final Map<RsPatField, Ty> myPatFieldTypes;
    @Nonnull
    private final Map<RsExpr, ExpectedType> myExpectedExprTypes;
    @Nonnull
    private final Map<RsPathExpr, List<ResolvedPath>> myResolvedPaths;
    @Nonnull
    private final Map<RsMethodCall, InferredMethodCallInfo> myResolvedMethods;
    @Nonnull
    private final Map<RsFieldLookup, List<RsElement>> myResolvedFields;
    @Nonnull
    private final Map<RsElement, List<Adjustment>> myAdjustments;
    @Nonnull
    private final Set<RsElement> myOverloadedOperators;
    @Nonnull
    private final List<RsDiagnostic> myDiagnostics;
    private final long myTimestamp;

    public RsInferenceResult(
        @Nonnull Map<RsExpr, Ty> exprTypes,
        @Nonnull Map<RsPat, Ty> patTypes,
        @Nonnull Map<RsPatField, Ty> patFieldTypes,
        @Nonnull Map<RsExpr, ExpectedType> expectedExprTypes,
        @Nonnull Map<RsPathExpr, List<ResolvedPath>> resolvedPaths,
        @Nonnull Map<RsMethodCall, InferredMethodCallInfo> resolvedMethods,
        @Nonnull Map<RsFieldLookup, List<RsElement>> resolvedFields,
        @Nonnull Map<RsElement, List<Adjustment>> adjustments,
        @Nonnull Set<RsElement> overloadedOperators,
        @Nonnull List<RsDiagnostic> diagnostics
    ) {
        myExprTypes = exprTypes;
        myPatTypes = patTypes;
        myPatFieldTypes = patFieldTypes;
        myExpectedExprTypes = expectedExprTypes;
        myResolvedPaths = resolvedPaths;
        myResolvedMethods = resolvedMethods;
        myResolvedFields = resolvedFields;
        myAdjustments = adjustments;
        myOverloadedOperators = overloadedOperators;
        myDiagnostics = diagnostics;
        myTimestamp = System.nanoTime();
    }

    @Nonnull
    public Map<RsExpr, Ty> getExprTypes() {
        return myExprTypes;
    }

    @Nonnull
    public Map<RsPat, Ty> getPatTypes() {
        return myPatTypes;
    }

    @Nonnull
    public Map<RsPatField, Ty> getPatFieldTypes() {
        return myPatFieldTypes;
    }

    @Nonnull
    public List<RsDiagnostic> getDiagnostics() {
        return myDiagnostics;
    }

    @Nonnull
    @Override
    public List<Adjustment> getExprAdjustments(@Nonnull RsElement expr) {
        List<Adjustment> result = myAdjustments.get(expr);
        return result != null ? result : Collections.emptyList();
    }

    @Nonnull
    @Override
    public Ty getExprType(@Nonnull RsExpr expr) {
        Ty ty = myExprTypes.get(expr);
        return ty != null ? ty : TyUnknown.INSTANCE;
    }

    @Nonnull
    @Override
    public Ty getPatType(@Nonnull RsPat pat) {
        Ty ty = myPatTypes.get(pat);
        return ty != null ? ty : TyUnknown.INSTANCE;
    }

    @Nonnull
    @Override
    public Ty getPatFieldType(@Nonnull RsPatField patField) {
        Ty ty = myPatFieldTypes.get(patField);
        return ty != null ? ty : TyUnknown.INSTANCE;
    }

    @Nonnull
    @Override
    public ExpectedType getExpectedExprType(@Nonnull RsExpr expr) {
        ExpectedType et = myExpectedExprTypes.get(expr);
        return et != null ? et : ExpectedType.UNKNOWN;
    }

    @Nonnull
    @Override
    public List<ResolvedPath> getResolvedPath(@Nonnull RsPathExpr expr) {
        List<ResolvedPath> result = myResolvedPaths.get(expr);
        return result != null ? result : Collections.emptyList();
    }

    @Override
    public boolean isOverloadedOperator(@Nonnull RsExpr expr) {
        return myOverloadedOperators.contains(expr);
    }

    @Nonnull
    public List<MethodResolveVariant> getResolvedMethod(@Nonnull RsMethodCall call) {
        InferredMethodCallInfo info = myResolvedMethods.get(call);
        return info != null ? info.getResolveVariants() : Collections.emptyList();
    }

    @Nullable
    public TyFunctionBase getResolvedMethodType(@Nonnull RsMethodCall call) {
        InferredMethodCallInfo info = myResolvedMethods.get(call);
        return info != null ? info.getType() : null;
    }

    @Nonnull
    public Substitution getResolvedMethodSubst(@Nonnull RsMethodCall call) {
        InferredMethodCallInfo info = myResolvedMethods.get(call);
        return info != null ? info.getSubst() : SubstitutionUtil.EMPTY_SUBSTITUTION;
    }

    @Nonnull
    public List<RsElement> getResolvedField(@Nonnull RsFieldLookup call) {
        List<RsElement> result = myResolvedFields.get(call);
        return result != null ? result : Collections.emptyList();
    }

    
    public boolean isExprTypeInferred(@Nonnull RsExpr expr) {
        return myExprTypes.containsKey(expr);
    }

    
    public long getTimestamp() {
        return myTimestamp;
    }
}
