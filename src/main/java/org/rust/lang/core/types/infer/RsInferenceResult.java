/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
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

    @NotNull
    private final Map<RsExpr, Ty> myExprTypes;
    @NotNull
    private final Map<RsPat, Ty> myPatTypes;
    @NotNull
    private final Map<RsPatField, Ty> myPatFieldTypes;
    @NotNull
    private final Map<RsExpr, ExpectedType> myExpectedExprTypes;
    @NotNull
    private final Map<RsPathExpr, List<ResolvedPath>> myResolvedPaths;
    @NotNull
    private final Map<RsMethodCall, InferredMethodCallInfo> myResolvedMethods;
    @NotNull
    private final Map<RsFieldLookup, List<RsElement>> myResolvedFields;
    @NotNull
    private final Map<RsElement, List<Adjustment>> myAdjustments;
    @NotNull
    private final Set<RsElement> myOverloadedOperators;
    @NotNull
    private final List<RsDiagnostic> myDiagnostics;
    private final long myTimestamp;

    public RsInferenceResult(
        @NotNull Map<RsExpr, Ty> exprTypes,
        @NotNull Map<RsPat, Ty> patTypes,
        @NotNull Map<RsPatField, Ty> patFieldTypes,
        @NotNull Map<RsExpr, ExpectedType> expectedExprTypes,
        @NotNull Map<RsPathExpr, List<ResolvedPath>> resolvedPaths,
        @NotNull Map<RsMethodCall, InferredMethodCallInfo> resolvedMethods,
        @NotNull Map<RsFieldLookup, List<RsElement>> resolvedFields,
        @NotNull Map<RsElement, List<Adjustment>> adjustments,
        @NotNull Set<RsElement> overloadedOperators,
        @NotNull List<RsDiagnostic> diagnostics
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

    @NotNull
    public Map<RsExpr, Ty> getExprTypes() {
        return myExprTypes;
    }

    @NotNull
    public Map<RsPat, Ty> getPatTypes() {
        return myPatTypes;
    }

    @NotNull
    public Map<RsPatField, Ty> getPatFieldTypes() {
        return myPatFieldTypes;
    }

    @NotNull
    public List<RsDiagnostic> getDiagnostics() {
        return myDiagnostics;
    }

    @NotNull
    @Override
    public List<Adjustment> getExprAdjustments(@NotNull RsElement expr) {
        List<Adjustment> result = myAdjustments.get(expr);
        return result != null ? result : Collections.emptyList();
    }

    @NotNull
    @Override
    public Ty getExprType(@NotNull RsExpr expr) {
        Ty ty = myExprTypes.get(expr);
        return ty != null ? ty : TyUnknown.INSTANCE;
    }

    @NotNull
    @Override
    public Ty getPatType(@NotNull RsPat pat) {
        Ty ty = myPatTypes.get(pat);
        return ty != null ? ty : TyUnknown.INSTANCE;
    }

    @NotNull
    @Override
    public Ty getPatFieldType(@NotNull RsPatField patField) {
        Ty ty = myPatFieldTypes.get(patField);
        return ty != null ? ty : TyUnknown.INSTANCE;
    }

    @NotNull
    @Override
    public ExpectedType getExpectedExprType(@NotNull RsExpr expr) {
        ExpectedType et = myExpectedExprTypes.get(expr);
        return et != null ? et : ExpectedType.UNKNOWN;
    }

    @NotNull
    @Override
    public List<ResolvedPath> getResolvedPath(@NotNull RsPathExpr expr) {
        List<ResolvedPath> result = myResolvedPaths.get(expr);
        return result != null ? result : Collections.emptyList();
    }

    @Override
    public boolean isOverloadedOperator(@NotNull RsExpr expr) {
        return myOverloadedOperators.contains(expr);
    }

    @NotNull
    public List<MethodResolveVariant> getResolvedMethod(@NotNull RsMethodCall call) {
        InferredMethodCallInfo info = myResolvedMethods.get(call);
        return info != null ? info.getResolveVariants() : Collections.emptyList();
    }

    @Nullable
    public TyFunctionBase getResolvedMethodType(@NotNull RsMethodCall call) {
        InferredMethodCallInfo info = myResolvedMethods.get(call);
        return info != null ? info.getType() : null;
    }

    @NotNull
    public Substitution getResolvedMethodSubst(@NotNull RsMethodCall call) {
        InferredMethodCallInfo info = myResolvedMethods.get(call);
        return info != null ? info.getSubst() : SubstitutionUtil.EMPTY_SUBSTITUTION;
    }

    @NotNull
    public List<RsElement> getResolvedField(@NotNull RsFieldLookup call) {
        List<RsElement> result = myResolvedFields.get(call);
        return result != null ? result : Collections.emptyList();
    }

    @TestOnly
    public boolean isExprTypeInferred(@NotNull RsExpr expr) {
        return myExprTypes.containsKey(expr);
    }

    @TestOnly
    public long getTimestamp() {
        return myTimestamp;
    }
}
