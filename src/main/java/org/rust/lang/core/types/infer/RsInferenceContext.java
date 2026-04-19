/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.*;
import org.rust.lang.core.resolve.ref.MethodResolveVariant;
import org.rust.lang.core.resolve.ref.PathPsiSubstUtil;
import org.rust.lang.core.resolve.ref.RsPathReferenceImpl;
import org.rust.lang.core.types.*;
import org.rust.lang.core.types.consts.*;
import org.rust.lang.core.types.ty.*;
import org.rust.lang.utils.RsDiagnostic;
import org.rust.lang.utils.snapshot.CombinedSnapshot;
import org.rust.lang.utils.snapshot.Snapshot;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.RsResult;

import java.util.*;
import java.util.function.Supplier;
import org.rust.lang.core.psi.ext.RsBinaryExprUtil;

/**
 * A mutable object, which is filled while we walk function body top down.
 */
public class RsInferenceContext implements RsInferenceData {
    @NotNull
    public final Project project;
    @NotNull
    public final ImplLookup lookup;
    @NotNull
    public final KnownItems items;
    @NotNull
    public final FulfillmentContext fulfill;

    private final Map<RsExpr, Ty> myExprTypes = new HashMap<>();
    private final Map<RsPat, Ty> myPatTypes = new HashMap<>();
    private final Map<RsPatField, Ty> myPatFieldTypes = new HashMap<>();
    private final Map<RsExpr, ExpectedType> myExpectedExprTypes = new HashMap<>();
    private final Map<RsPathExpr, List<ResolvedPath>> myResolvedPaths = new HashMap<>();
    private final Map<RsMethodCall, InferredMethodCallInfo> myResolvedMethods = new HashMap<>();
    private final Map<RsFieldLookup, List<RsElement>> myResolvedFields = new HashMap<>();
    private final List<Map.Entry<RsPathExpr, TraitRef>> myPathRefinements = new ArrayList<>();
    private final List<Map.Entry<RsMethodCall, TraitRef>> myMethodRefinements = new ArrayList<>();
    private final Map<RsElement, List<Adjustment>> myAdjustments = new HashMap<>();
    private final Set<RsElement> myOverloadedOperators = new HashSet<>();
    public final List<RsDiagnostic> diagnostics = new ArrayList<>();

    private final UnificationTable<TyInfer.IntVar, TyInteger> myIntUnificationTable = new UnificationTable<>();
    private final UnificationTable<TyInfer.FloatVar, TyFloat> myFloatUnificationTable = new UnificationTable<>();
    private final UnificationTable<TyInfer.TyVar, Ty> myVarUnificationTable = new UnificationTable<>();
    private final UnificationTable<CtInferVar, Const> myConstUnificationTable = new UnificationTable<>();
    private final ProjectionCache myProjectionCache = new ProjectionCache();

    private final ShallowResolver myShallowResolver = new ShallowResolver();
    private final OpportunisticVarResolver myOpportunisticVarResolver = new OpportunisticVarResolver();
    private final FullTypeResolver myFullTypeResolver = new FullTypeResolver();
    private final FullTypeWithOriginsResolver myFullTypeWithOriginsResolver = new FullTypeWithOriginsResolver();

    public RsInferenceContext(@NotNull Project project,
                              @NotNull ImplLookup lookup,
                              @NotNull KnownItems items,
                              @NotNull TypeInferenceOptions options) {
        this.project = project;
        this.lookup = lookup;
        this.items = items;
        this.fulfill = new FulfillmentContext(this, lookup, options.isTraceObligations());
    }

    @NotNull
    public Snapshot startSnapshot() {
        return new CombinedSnapshot(
            myIntUnificationTable.startSnapshot(),
            myFloatUnificationTable.startSnapshot(),
            myVarUnificationTable.startSnapshot(),
            myConstUnificationTable.startSnapshot(),
            myProjectionCache.startSnapshot()
        );
    }

    @Nullable
    public <T> T probe(@NotNull Supplier<T> action) {
        Snapshot snapshot = startSnapshot();
        try {
            return action.get();
        } finally {
            snapshot.rollback();
        }
    }

    @Nullable
    public <T> T commitIfNotNull(@NotNull Supplier<T> action) {
        Snapshot snapshot = startSnapshot();
        T result = action.get();
        if (result == null) {
            snapshot.rollback();
        } else {
            snapshot.commit();
        }
        return result;
    }

    @NotNull
    public RsInferenceResult infer(@NotNull RsInferenceContextOwner element) {
        if (element instanceof RsFunction) {
            RsFunction fn = (RsFunction) element;
            Ty retTy = normalizeAssociatedTypesIn(RsFunctionUtil.getRawReturnType(fn)).getValue();
            RsTypeInferenceWalker fctx = new RsTypeInferenceWalker(this, retTy);
            fctx.extractParameterBindings(fn);
            RsBlock block = RsFunctionUtil.getBlock(fn);
            if (block != null) fctx.inferFnBody(block);
        } else if (element instanceof RsReplCodeFragment) {
            RsReplCodeFragment fragment = (RsReplCodeFragment) element;
            RsInferenceResult contextInference = RsElementUtil.getInference(fragment.getContext());
            if (contextInference != null) {
                myPatTypes.putAll(contextInference.getPatTypes());
                myPatFieldTypes.putAll(contextInference.getPatFieldTypes());
                myExprTypes.putAll(contextInference.getExprTypes());
            }
            new RsTypeInferenceWalker(this, TyUnknown.INSTANCE).inferReplCodeFragment(fragment);
        } else if (element instanceof RsPath) {
            inferPath((RsPath) element);
        } else {
            inferOther(element);
        }

        fulfill.selectWherePossible();
        fallbackUnresolvedTypeVarsIfPossible();
        fulfill.selectWherePossible();

        myExprTypes.replaceAll((k, ty) -> fullyResolve(ty));
        myExpectedExprTypes.replaceAll((k, ty) -> fullyResolveWithOrigins(ty));
        myPatTypes.replaceAll((k, ty) -> fullyResolve(ty));
        myPatFieldTypes.replaceAll((k, ty) -> fullyResolve(ty));
        diagnostics.replaceAll(d -> {
            if (d instanceof RsDiagnostic.TypeError) {
                return fullyResolve((RsDiagnostic.TypeError) d);
            }
            return d;
        });
        fixScalarBuiltinExprs();
        myAdjustments.replaceAll((k, list) -> {
            List<Adjustment> result = new ArrayList<>(list.size());
            for (Adjustment adj : list) {
                result.add(fullyResolve(adj));
            }
            return result;
        });

        performPathsRefinement(lookup);

        for (List<ResolvedPath> paths : myResolvedPaths.values()) {
            for (ResolvedPath rp : paths) {
                rp.setSubst(rp.getSubst().foldValues(myFullTypeWithOriginsResolver));
            }
        }
        myResolvedMethods.replaceAll((k, v) -> fullyResolveWithOrigins(v));

        return new RsInferenceResult(
            myExprTypes,
            myPatTypes,
            myPatFieldTypes,
            myExpectedExprTypes,
            myResolvedPaths,
            myResolvedMethods,
            myResolvedFields,
            myAdjustments,
            myOverloadedOperators,
            diagnostics
        );
    }

    private void inferPath(@NotNull RsPath element) {
        // Infer const argument types for generic paths
        Object parentObj = element.getParent();
        RsGenericDeclaration declaration = null;
        if (parentObj instanceof RsAssocTypeBinding) {
            // Handle assoc type bindings
        } else {
            List<?> resolved = org.rust.lang.core.resolve.ref.RsPathReferenceImpl.resolvePathRaw(element, lookup, true);
            if (resolved.size() == 1) {
                Object first = resolved.get(0);
                if (first instanceof ScopeEntry) {
                    RsElement el = ((ScopeEntry) first).getElement();
                    if (el instanceof RsGenericDeclaration) {
                        declaration = (RsGenericDeclaration) el;
                    }
                }
            }
        }
        if (declaration != null) {
            List<RsConstParameter> constParameters = new ArrayList<>();
            List<RsElement> constArguments = new ArrayList<>();
            RsPsiSubstitution psiSubst = org.rust.lang.core.resolve.ref.PathPsiSubstUtil.pathPsiSubst(element, declaration);
            for (Map.Entry<RsConstParameter, RsPsiSubstitution.Value<RsElement, RsExpr>> entry : psiSubst.getConstSubst().entrySet()) {
                if (entry.getValue() instanceof RsPsiSubstitution.Value.Present) {
                    constParameters.add(entry.getKey());
                    constArguments.add(((RsPsiSubstitution.Value.Present<RsElement, RsExpr>) entry.getValue()).getValue());
                }
            }
            new RsTypeInferenceWalker(this, TyUnknown.INSTANCE).inferConstArgumentTypes(constParameters, constArguments);
        }
    }

    private void inferOther(@NotNull RsInferenceContextOwner element) {
        Ty retTy = null;
        RsExpr expr = null;
        if (element instanceof RsConstant) {
            RsConstant c = (RsConstant) element;
            RsTypeReference tr = c.getTypeReference();
            retTy = tr != null ? ExtensionsUtil.getRawType(tr) : null;
            expr = c.getExpr();
        } else if (element instanceof RsConstParameter) {
            RsConstParameter cp = (RsConstParameter) element;
            RsTypeReference tr = cp.getTypeReference();
            retTy = tr != null ? ExtensionsUtil.getRawType(tr) : null;
            expr = cp.getExpr();
        } else if (element instanceof RsArrayType) {
            retTy = TyInteger.USize.INSTANCE;
            expr = ((RsArrayType) element).getExpr();
        } else if (element instanceof RsVariantDiscriminant) {
            RsEnumItem enumItem = RsElementUtil.contextStrict((RsElement) element, RsEnumItem.class);
            retTy = enumItem != null ? RsEnumItemUtil.getReprType(enumItem) : null;
            expr = ((RsVariantDiscriminant) element).getExpr();
        } else if (element instanceof RsExpressionCodeFragment) {
            RsExpressionCodeFragment fragment = (RsExpressionCodeFragment) element;
            RsInferenceResult contextInference = RsElementUtil.getInference(fragment.getContext());
            if (contextInference != null) {
                myPatTypes.putAll(contextInference.getPatTypes());
                myPatFieldTypes.putAll(contextInference.getPatFieldTypes());
                myExprTypes.putAll(contextInference.getExprTypes());
            }
            expr = fragment.getExpr();
        } else if (element instanceof RsDefaultParameterValue) {
            RsDefaultParameterValue dpv = (RsDefaultParameterValue) element;
            Object parent = dpv.getParent();
            if (parent instanceof RsValueParameter) {
                RsTypeReference tr = ((RsValueParameter) parent).getTypeReference();
                retTy = tr != null ? ExtensionsUtil.getRawType(tr) : null;
            } else if (parent instanceof RsNamedFieldDecl) {
                RsTypeReference tr = ((RsNamedFieldDecl) parent).getTypeReference();
                retTy = tr != null ? ExtensionsUtil.getRawType(tr) : null;
            } else if (parent instanceof RsTupleFieldDecl) {
                retTy = ExtensionsUtil.getRawType(((RsTupleFieldDecl) parent).getTypeReference());
            }
            expr = dpv.getExpr();
        }
        if (expr != null) {
            new RsTypeInferenceWalker(this, retTy != null ? retTy : TyUnknown.INSTANCE).inferLambdaBody(expr);
        }
    }

    private void fixScalarBuiltinExprs() {
        for (RsExpr expr : myExprTypes.keySet()) {
            if (expr instanceof RsBinaryExpr) {
                RsBinaryExpr binExpr = (RsBinaryExpr) expr;
                RsExpr lhs = binExpr.getLeft();
                RsExpr rhs = binExpr.getRight();
                if (rhs == null) continue;
                Ty lhsTy = myExprTypes.get(lhs);
                Ty rhsTy = myExprTypes.get(rhs);
                if (lhsTy != null && TyUtil.isScalar(lhsTy) && rhsTy != null && TyUtil.isScalar(rhsTy)) {
                    Object op = RsBinaryExprUtil.getOperatorType(binExpr);
                    if (op instanceof AssignmentOp && op == AssignmentOp.EQ) {
                        // no-op
                    } else if (op instanceof ArithmeticOp || op instanceof BoolOp) {
                        if (op instanceof EqualityOp || op instanceof ComparisonOp) {
                            List<Adjustment> lhsAdj = myAdjustments.get(lhs);
                            if (lhsAdj != null && !lhsAdj.isEmpty()) lhsAdj.remove(lhsAdj.size() - 1);
                            List<Adjustment> rhsAdj = myAdjustments.get(rhs);
                            if (rhsAdj != null && !rhsAdj.isEmpty()) rhsAdj.remove(rhsAdj.size() - 1);
                        }
                    } else if (op instanceof ArithmeticAssignmentOp) {
                        List<Adjustment> lhsAdj = myAdjustments.get(lhs);
                        if (lhsAdj != null && !lhsAdj.isEmpty()) lhsAdj.remove(lhsAdj.size() - 1);
                    }
                }
            }
        }
    }

    private void fallbackUnresolvedTypeVarsIfPossible() {
        List<Ty> allTypes = new ArrayList<>();
        allTypes.addAll(myExprTypes.values());
        allTypes.addAll(myPatTypes.values());
        allTypes.addAll(myPatFieldTypes.values());
        for (ExpectedType et : myExpectedExprTypes.values()) {
            allTypes.add(et.getTy());
        }
        for (Ty ty : allTypes) {
            FoldUtil.visitInferTys(ty, tyInfer -> {
                Ty rty = shallowResolve(tyInfer);
                if (rty instanceof TyInfer) {
                    fallbackIfPossible((TyInfer) rty);
                }
                return false;
            });
        }
    }

    private void fallbackIfPossible(@NotNull TyInfer ty) {
        if (ty instanceof TyInfer.IntVar) {
            myIntUnificationTable.unifyVarValue((TyInfer.IntVar) ty, TyInteger.DEFAULT);
        } else if (ty instanceof TyInfer.FloatVar) {
            myFloatUnificationTable.unifyVarValue((TyInfer.FloatVar) ty, TyFloat.DEFAULT);
        }
        // TyVar - do nothing
    }

    private void performPathsRefinement(@NotNull ImplLookup lookup) {
        for (Map.Entry<RsPathExpr, TraitRef> entry : myPathRefinements) {
            RsPathExpr path = entry.getKey();
            TraitRef traitRef = entry.getValue();
            List<ResolvedPath> variants = myResolvedPaths.get(path);
            if (variants == null || variants.isEmpty()) continue;
            ResolvedPath variant = variants.get(0);
            String fnName = (variant.getElement() instanceof RsFunction) ? ((RsFunction) variant.getElement()).getName() : null;
            SelectionResult<?> sel = lookup.select(resolveTypeVarsIfPossible(traitRef));
            if (!(sel instanceof SelectionResult.Ok)) continue;
            Object impl = ((SelectionResult.Ok<?>) sel).getResult();
            if (!(impl instanceof RsImplItem)) continue;
            // find fn by name in impl
            RsFunction fn = null;
            for (RsFunction f : RsMembersUtil.getFunctions(RsMembersUtil.getExpandedMembers((RsImplItem) impl))) {
                if (Objects.equals(f.getName(), fnName)) {
                    fn = f;
                    break;
                }
            }
            if (fn == null) continue;
            TraitImplSource source = RsCachedImplItem.forImpl((RsImplItem) impl).getExplicitImpl();
            ResolvedPath.AssocItem result = new ResolvedPath.AssocItem(fn, source);
            result.setSubst(variant.getSubst());
            myResolvedPaths.put(path, Collections.singletonList(result));
        }
        for (Map.Entry<RsMethodCall, TraitRef> entry : myMethodRefinements) {
            RsMethodCall call = entry.getKey();
            TraitRef traitRef = entry.getValue();
            InferredMethodCallInfo info = myResolvedMethods.get(call);
            if (info == null) continue;
            List<MethodResolveVariant> resolveVariants = info.getResolveVariants();
            if (resolveVariants.isEmpty()) continue;
            MethodResolveVariant variant = resolveVariants.get(0);
            SelectionResult<?> sel = lookup.select(resolveTypeVarsIfPossible(traitRef));
            if (!(sel instanceof SelectionResult.Ok)) continue;
            Object impl = ((SelectionResult.Ok<?>) sel).getResult();
            if (!(impl instanceof RsImplItem)) continue;
            RsFunction fn = null;
            for (RsFunction f : RsMembersUtil.getFunctions(RsMembersUtil.getExpandedMembers((RsImplItem) impl))) {
                if (Objects.equals(f.getName(), variant.getName())) {
                    fn = f;
                    break;
                }
            }
            if (fn == null) continue;
            TraitImplSource source = RsCachedImplItem.forImpl((RsImplItem) impl).getExplicitImpl();
            MethodResolveVariant newVariant = new MethodResolveVariant(variant.getName(), fn, variant.getSelfTy(), variant.getDerefCount(), source);
            myResolvedMethods.put(call, info.copy(Collections.singletonList(newVariant)));
        }
    }

    // --- RsInferenceData implementation ---

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

    public boolean isTypeInferred(@NotNull RsExpr expr) {
        return myExprTypes.containsKey(expr);
    }

    // --- Write methods ---

    public void writeExprTy(@NotNull RsExpr psi, @NotNull Ty ty) {
        myExprTypes.put(psi, ty);
    }

    public void writePatTy(@NotNull RsPat psi, @NotNull Ty ty) {
        myPatTypes.put(psi, ty);
    }

    public void writePatFieldTy(@NotNull RsPatField psi, @NotNull Ty ty) {
        myPatFieldTypes.put(psi, ty);
    }

    public void writeExpectedExprTy(@NotNull RsExpr psi, @NotNull Ty ty) {
        myExpectedExprTypes.put(psi, new ExpectedType(ty));
    }

    public void writeExpectedExprTyCoercable(@NotNull RsExpr psi) {
        myExpectedExprTypes.computeIfPresent(psi, (k, v) -> v.withCoercable(true));
    }

    public void writePath(@NotNull RsPathExpr path, @NotNull List<ResolvedPath> resolved) {
        myResolvedPaths.put(path, resolved);
    }

    public void writePathSubst(@NotNull RsPathExpr path, @NotNull Substitution subst) {
        List<ResolvedPath> paths = myResolvedPaths.get(path);
        if (paths != null && paths.size() == 1) {
            paths.get(0).setSubst(subst);
        }
    }

    public void writeResolvedMethod(@NotNull RsMethodCall call, @NotNull List<MethodResolveVariant> resolvedTo) {
        myResolvedMethods.put(call, new InferredMethodCallInfo(resolvedTo));
    }

    public void writeResolvedMethodSubst(@NotNull RsMethodCall call, @NotNull Substitution subst, @NotNull TyFunctionBase ty) {
        InferredMethodCallInfo info = myResolvedMethods.get(call);
        if (info != null) {
            info.setSubst(subst);
            info.setType(ty);
        }
    }

    public void writeResolvedField(@NotNull RsFieldLookup lookup, @NotNull List<RsElement> resolvedTo) {
        myResolvedFields.put(lookup, resolvedTo);
    }

    public void registerPathRefinement(@NotNull RsPathExpr path, @NotNull TraitRef traitRef) {
        myPathRefinements.add(new AbstractMap.SimpleEntry<>(path, traitRef));
    }

    public void registerMethodRefinement(@NotNull RsMethodCall path, @NotNull TraitRef traitRef) {
        myMethodRefinements.add(new AbstractMap.SimpleEntry<>(path, traitRef));
    }

    public void addDiagnostic(@NotNull RsDiagnostic diagnostic) {
        if (diagnostic.getElement().getContainingFile().isPhysical()) {
            diagnostics.add(diagnostic);
        }
    }

    public void applyAdjustment(@NotNull RsElement expr, @NotNull Adjustment adjustment) {
        applyAdjustments(expr, Collections.singletonList(adjustment));
    }

    public void applyAdjustments(@NotNull RsElement expr, @NotNull List<Adjustment> adjustment) {
        if (adjustment.isEmpty()) return;
        RsElement unwrappedExpr = expr instanceof RsExpr ? RsExprUtil.unwrapParenExprs((RsExpr) expr) : expr;

        boolean isAutoborrowMut = false;
        for (Adjustment adj : adjustment) {
            if (adj instanceof Adjustment.BorrowReference
                && ((Adjustment.BorrowReference) adj).getMutability() instanceof AutoBorrowMutability.Mutable) {
                isAutoborrowMut = true;
                break;
            }
        }

        myAdjustments.computeIfAbsent(unwrappedExpr, k -> new ArrayList<>()).addAll(adjustment);

        if (isAutoborrowMut && unwrappedExpr instanceof RsExpr) {
            convertPlaceDerefsToMutable((RsExpr) unwrappedExpr);
        }
    }

    public void writeOverloadedOperator(@NotNull RsExpr expr) {
        myOverloadedOperators.add(expr);
    }

    public void reportTypeMismatch(@NotNull RsElement element, @NotNull Ty expected, @NotNull Ty actual) {
        addDiagnostic(new RsDiagnostic.TypeError(element, expected, actual));
    }

    // --- Type combination ---

    public boolean canCombineTypes(@NotNull Ty ty1, @NotNull Ty ty2) {
        return probe(() -> combineTypes(ty1, ty2).isOk());
    }

    @NotNull
    public RsResult<Object, TypeError> combineTypes(@NotNull Ty ty1, @NotNull Ty ty2) {
        return combineTypesResolved(shallowResolve(ty1), shallowResolve(ty2));
    }

    @NotNull
    private RsResult<Object, TypeError> combineTypesResolved(@NotNull Ty ty1, @NotNull Ty ty2) {
        if (ty1 instanceof TyInfer.TyVar) return combineTyVar((TyInfer.TyVar) ty1, ty2);
        if (ty2 instanceof TyInfer.TyVar) return combineTyVar((TyInfer.TyVar) ty2, ty1);
        if (ty1 instanceof TyInfer) return combineIntOrFloatVar((TyInfer) ty1, ty2);
        if (ty2 instanceof TyInfer) return combineIntOrFloatVar((TyInfer) ty2, ty1);
        return combineTypesNoVars(ty1, ty2);
    }

    @NotNull
    private RsResult<Object, TypeError> combineTyVar(@NotNull TyInfer.TyVar ty1, @NotNull Ty ty2) {
        if (ty2 instanceof TyInfer.TyVar) {
            myVarUnificationTable.unifyVarVar(ty1, (TyInfer.TyVar) ty2);
        } else {
            TyInfer.TyVar ty1r = myVarUnificationTable.findRoot(ty1);
            boolean isCyclic = ty2.visitWith(new TypeVisitor() {
                @Override
                public boolean visitTy(@NotNull Ty ty) {
                    if (ty instanceof TyInfer.TyVar && myVarUnificationTable.findRoot((TyInfer.TyVar) ty) == ty1r) return true;
                    if (FoldUtil.hasTyInfer(ty)) return ty.superVisitWith(this);
                    return false;
                }
            });
            if (isCyclic) {
                TypeInferenceMarks.CyclicType.hit();
                myVarUnificationTable.unifyVarValue(ty1r, TyUnknown.INSTANCE);
            } else {
                myVarUnificationTable.unifyVarValue(ty1r, ty2);
            }
        }
        return new RsResult.Ok<>(null);
    }

    @NotNull
    private RsResult<Object, TypeError> combineIntOrFloatVar(@NotNull TyInfer ty1, @NotNull Ty ty2) {
        if (ty1 instanceof TyInfer.IntVar) {
            if (ty2 instanceof TyInfer.IntVar) {
                myIntUnificationTable.unifyVarVar((TyInfer.IntVar) ty1, (TyInfer.IntVar) ty2);
            } else if (ty2 instanceof TyInteger) {
                myIntUnificationTable.unifyVarValue((TyInfer.IntVar) ty1, (TyInteger) ty2);
            } else {
                return new RsResult.Err<>(new TypeError.TypeMismatch(ty1, ty2));
            }
        } else if (ty1 instanceof TyInfer.FloatVar) {
            if (ty2 instanceof TyInfer.FloatVar) {
                myFloatUnificationTable.unifyVarVar((TyInfer.FloatVar) ty1, (TyInfer.FloatVar) ty2);
            } else if (ty2 instanceof TyFloat) {
                myFloatUnificationTable.unifyVarValue((TyInfer.FloatVar) ty1, (TyFloat) ty2);
            } else {
                return new RsResult.Err<>(new TypeError.TypeMismatch(ty1, ty2));
            }
        }
        return new RsResult.Ok<>(null);
    }

    @NotNull
    public RsResult<Object, TypeError> combineTypesNoVars(@NotNull Ty ty1, @NotNull Ty ty2) {
        if (ty1 == ty2) return new RsResult.Ok<>(null);
        if (ty1 instanceof TyPrimitive && ty2 instanceof TyPrimitive && ty1.getClass() == ty2.getClass())
            return new RsResult.Ok<>(null);
        if (ty1 instanceof TyTypeParameter && ty2 instanceof TyTypeParameter && ty1.equals(ty2))
            return new RsResult.Ok<>(null);
        if (ty1 instanceof TyReference && ty2 instanceof TyReference
            && ((TyReference) ty1).getMutability() == ((TyReference) ty2).getMutability()) {
            return combineTypes(((TyReference) ty1).getReferenced(), ((TyReference) ty2).getReferenced());
        }
        if (ty1 instanceof TyPointer && ty2 instanceof TyPointer
            && ((TyPointer) ty1).getMutability() == ((TyPointer) ty2).getMutability()) {
            return combineTypes(((TyPointer) ty1).getReferenced(), ((TyPointer) ty2).getReferenced());
        }
        if (ty1 instanceof TySlice && ty2 instanceof TySlice) {
            return combineTypes(((TySlice) ty1).getElementType(), ((TySlice) ty2).getElementType());
        }
        if (ty1 instanceof TyArray && ty2 instanceof TyArray) {
            TyArray a1 = (TyArray) ty1;
            TyArray a2 = (TyArray) ty2;
            if (a1.getSize() == null || a2.getSize() == null || a1.getSize().equals(a2.getSize())) {
                RsResult<Object, TypeError> r = combineTypes(a1.getBase(), a2.getBase());
                if (r.isOk()) return combineConsts(a1.getConst(), a2.getConst());
                return r;
            }
        }
        if (ty1 instanceof TyTuple && ty2 instanceof TyTuple
            && ((TyTuple) ty1).getTypes().size() == ((TyTuple) ty2).getTypes().size()) {
            return combineTypePairs(zip(((TyTuple) ty1).getTypes(), ((TyTuple) ty2).getTypes()));
        }
        if (ty1 instanceof TyAdt && ty2 instanceof TyAdt && ((TyAdt) ty1).getItem() == ((TyAdt) ty2).getItem()) {
            RsResult<Object, TypeError> r = combineTypePairs(zip(((TyAdt) ty1).getTypeArguments(), ((TyAdt) ty2).getTypeArguments()));
            if (r.isOk()) return combineConstPairs(zip(((TyAdt) ty1).getConstArguments(), ((TyAdt) ty2).getConstArguments()));
            return r;
        }
        if (ty1 instanceof TyNever || ty2 instanceof TyNever) return new RsResult.Ok<>(null);
        if (ty1 instanceof TyFunctionBase && ty2 instanceof TyFunctionBase) {
            return combineFunctions((TyFunctionBase) ty1, (TyFunctionBase) ty2);
        }
        if (ty1 instanceof TyAnon && ty2 instanceof TyAnon
            && ((TyAnon) ty1).getDefinition() != null
            && ((TyAnon) ty1).getDefinition() == ((TyAnon) ty2).getDefinition()) {
            return new RsResult.Ok<>(null);
        }
        return new RsResult.Err<>(new TypeError.TypeMismatch(ty1, ty2));
    }

    @NotNull
    private RsResult<Object, TypeError> combineFunctions(@NotNull TyFunctionBase ty1, @NotNull TyFunctionBase ty2) {
        if (ty1.getParamTypes().size() != ty2.getParamTypes().size()) {
            return new RsResult.Err<>(new TypeError.TypeMismatch(ty1, ty2));
        }
        boolean compatible = (ty1 instanceof TyFunctionPointer && ty2 instanceof TyFunctionPointer)
            || (ty1 instanceof TyFunctionDef && ty2 instanceof TyFunctionDef && ((TyFunctionDef) ty1).getDef() == ((TyFunctionDef) ty2).getDef())
            || (ty1 instanceof TyClosure && ty2 instanceof TyClosure && ((TyClosure) ty1).getDef() == ((TyClosure) ty2).getDef());
        if (!compatible) return new RsResult.Err<>(new TypeError.TypeMismatch(ty1, ty2));

        RsResult<Object, TypeError> r = combineTypePairs(zip(ty1.getParamTypes(), ty2.getParamTypes()));
        if (!r.isOk()) return r;
        r = combineTypes(ty1.getRetType(), ty2.getRetType());
        if (!r.isOk()) return r;
        if (ty1.getFnSig().getUnsafety() != ty2.getFnSig().getUnsafety()) {
            return new RsResult.Err<>(new TypeError.TypeMismatch(ty1, ty2));
        }
        return new RsResult.Ok<>(null);
    }

    @NotNull
    public RsResult<Object, TypeError> combineConsts(@NotNull Const const1, @NotNull Const const2) {
        Const c1 = shallowResolve(const1);
        Const c2 = shallowResolve(const2);
        if (c1 instanceof CtInferVar) return combineConstVar((CtInferVar) c1, c2);
        if (c2 instanceof CtInferVar) return combineConstVar((CtInferVar) c2, c1);
        return combineConstsNoVars(c1, c2);
    }

    @NotNull
    private RsResult<Object, TypeError> combineConstVar(@NotNull CtInferVar const1, @NotNull Const const2) {
        if (const2 instanceof CtInferVar) {
            myConstUnificationTable.unifyVarVar(const1, (CtInferVar) const2);
        } else {
            CtInferVar c1r = myConstUnificationTable.findRoot(const1);
            myConstUnificationTable.unifyVarValue(c1r, const2);
        }
        return new RsResult.Ok<>(null);
    }

    @NotNull
    private RsResult<Object, TypeError> combineConstsNoVars(@NotNull Const c1, @NotNull Const c2) {
        if (c1 == c2) return new RsResult.Ok<>(null);
        if (c1 instanceof CtUnknown || c2 instanceof CtUnknown) return new RsResult.Ok<>(null);
        if (c1 instanceof CtUnevaluated || c2 instanceof CtUnevaluated) return new RsResult.Ok<>(null);
        if (c1.equals(c2)) return new RsResult.Ok<>(null);
        return new RsResult.Err<>(new TypeError.ConstMismatch(c1, c2));
    }

    @NotNull
    public RsResult<Object, TypeError> combineTypePairs(@NotNull List<com.intellij.openapi.util.Pair<Ty, Ty>> pairs) {
        RsResult<Object, TypeError> result = new RsResult.Ok<>(null);
        for (com.intellij.openapi.util.Pair<Ty, Ty> pair : pairs) {
            RsResult<Object, TypeError> r = combineTypes(pair.getFirst(), pair.getSecond());
            if (!r.isOk()) result = r;
        }
        return result;
    }

    @NotNull
    public RsResult<Object, TypeError> combineConstPairs(@NotNull List<com.intellij.openapi.util.Pair<Const, Const>> pairs) {
        RsResult<Object, TypeError> result = new RsResult.Ok<>(null);
        for (com.intellij.openapi.util.Pair<Const, Const> pair : pairs) {
            RsResult<Object, TypeError> r = combineConsts(pair.getFirst(), pair.getSecond());
            if (!r.isOk()) result = r;
        }
        return result;
    }

    public boolean combineTraitRefs(@NotNull TraitRef ref1, @NotNull TraitRef ref2) {
        if (ref1.getTrait().getElement() != ref2.getTrait().getElement()) return false;
        if (!combineTypes(ref1.getSelfTy(), ref2.getSelfTy()).isOk()) return false;
        for (com.intellij.openapi.util.Pair<Ty, Ty> pair : ref1.getTrait().getSubst().zipTypeValues(ref2.getTrait().getSubst())) {
            if (!combineTypes(pair.getFirst(), pair.getSecond()).isOk()) return false;
        }
        for (com.intellij.openapi.util.Pair<Const, Const> pair : ref1.getTrait().getSubst().zipConstValues(ref2.getTrait().getSubst())) {
            if (!combineConsts(pair.getFirst(), pair.getSecond()).isOk()) return false;
        }
        return true;
    }

    public <T extends RsElement> boolean combineBoundElements(@NotNull BoundElement<T> be1, @NotNull BoundElement<T> be2) {
        if (be1.getElement() != be2.getElement()) return false;
        if (!combineTypePairs(be1.getSubst().zipTypeValues(be2.getSubst())).isOk()) return false;
        if (!combineConstPairs(be1.getSubst().zipConstValues(be2.getSubst())).isOk()) return false;
        return combineTypePairs(org.rust.stdext.CollectionsUtil.zipValues(be1.getAssoc(), be2.getAssoc())).isOk();
    }

    // --- Coercion ---

    @NotNull
    public RsResult<CoerceOk, TypeError> tryCoerce(@NotNull Ty inferred, @NotNull Ty expected) {
        if (inferred == expected) return new RsResult.Ok<>(new CoerceOk());
        if (inferred instanceof TyNever) {
            return new RsResult.Ok<>(new CoerceOk(Collections.singletonList(new Adjustment.NeverToAny(expected))));
        }
        if (inferred instanceof TyInfer.TyVar) {
            RsResult<Object, TypeError> r = combineTypes(inferred, expected);
            return r.isOk() ? new RsResult.Ok<>(new CoerceOk()) : new RsResult.Err<>(((RsResult.Err<Object, TypeError>) r).getErr());
        }
        // Try unsizing
        CoerceOk unsize = commitIfNotNull(() -> coerceUnsized(inferred, expected));
        if (unsize != null) return new RsResult.Ok<>(unsize);

        // Reference to pointer
        if (inferred instanceof TyReference && expected instanceof TyPointer
            && coerceMutability(((TyReference) inferred).getMutability(), ((TyPointer) expected).getMutability())) {
            RsResult<Object, TypeError> r = combineTypes(((TyReference) inferred).getReferenced(), ((TyPointer) expected).getReferenced());
            if (r.isOk()) {
                List<Adjustment> adjs = new ArrayList<>();
                adjs.add(new Adjustment.Deref(((TyReference) inferred).getReferenced(), null));
                adjs.add(new Adjustment.BorrowPointer((TyPointer) expected));
                return new RsResult.Ok<>(new CoerceOk(adjs));
            }
        }
        // Mut pointer to const pointer
        if (inferred instanceof TyPointer && ((TyPointer) inferred).getMutability().isMut()
            && expected instanceof TyPointer && !((TyPointer) expected).getMutability().isMut()) {
            RsResult<Object, TypeError> r = combineTypes(((TyPointer) inferred).getReferenced(), ((TyPointer) expected).getReferenced());
            if (r.isOk()) {
                return new RsResult.Ok<>(new CoerceOk(Collections.singletonList(new Adjustment.MutToConstPointer((TyPointer) expected))));
            }
        }
        // Reference coercion
        if (inferred instanceof TyReference && expected instanceof TyReference
            && coerceMutability(((TyReference) inferred).getMutability(), ((TyReference) expected).getMutability())) {
            return coerceReference((TyReference) inferred, (TyReference) expected);
        }
        // Closure to fn pointer
        if (inferred instanceof TyClosure && expected instanceof TyFunctionPointer) {
            Ty inferredFnPtr = new TyFunctionPointer(((TyClosure) inferred).getFnSig().copy(((TyFunctionPointer) expected).getUnsafety()));
            RsResult<Object, TypeError> r = combineTypes(inferredFnPtr, expected);
            if (r.isOk()) {
                return new RsResult.Ok<>(new CoerceOk(Collections.singletonList(new Adjustment.ClosureFnPointer((TyFunctionPointer) expected))));
            }
        }
        // FnDef to fn pointer
        if (inferred instanceof TyFunctionDef && expected instanceof TyFunctionPointer) {
            List<Adjustment> adjs = Collections.singletonList(new Adjustment.ReifyFnPointer(new TyFunctionPointer(((TyFunctionDef) inferred).getFnSig())));
            return coerceFromSafeFn((TyFunctionBase) inferred, (TyFunctionPointer) expected, adjs);
        }
        // FnPointer to fn pointer
        if (inferred instanceof TyFunctionPointer && expected instanceof TyFunctionPointer) {
            return coerceFromSafeFn((TyFunctionBase) inferred, (TyFunctionPointer) expected, Collections.emptyList());
        }

        RsResult<Object, TypeError> r = combineTypes(inferred, expected);
        return r.isOk() ? new RsResult.Ok<>(new CoerceOk()) : new RsResult.Err<>(((RsResult.Err<Object, TypeError>) r).getErr());
    }

    @NotNull
    private RsResult<CoerceOk, TypeError> coerceFromSafeFn(
        @NotNull TyFunctionBase inferred,
        @NotNull TyFunctionPointer expected,
        @NotNull List<Adjustment> adjustments
    ) {
        List<Adjustment> finalAdjustments = new ArrayList<>(adjustments);
        TyFunctionPointer inf;
        if (inferred.getUnsafety() == Unsafety.Normal && expected.getUnsafety() == Unsafety.Unsafe) {
            finalAdjustments.add(new Adjustment.UnsafeFnPointer(expected));
            inf = new TyFunctionPointer(inferred.getFnSig().copy(Unsafety.Unsafe));
        } else {
            inf = new TyFunctionPointer(inferred.getFnSig());
        }
        RsResult<Object, TypeError> r = combineTypes(inf, expected);
        if (r.isOk()) return new RsResult.Ok<>(new CoerceOk(finalAdjustments));
        return new RsResult.Err<>(((RsResult.Err<Object, TypeError>) r).getErr());
    }

    @Nullable
    private CoerceOk coerceUnsized(@NotNull Ty source, @NotNull Ty target) {
        if (source instanceof TyInfer.TyVar || target instanceof TyInfer.TyVar) return null;
        if (TyUtil.isScalar(target) || canCombineTypes(source, target)) return null;
        // Simplified unsizing - delegate to lookup
        return null; // Full implementation would check Unsize/CoerceUnsized traits
    }

    @NotNull
    private RsResult<CoerceOk, TypeError> coerceReference(@NotNull TyReference inferred, @NotNull TyReference expected) {
        Autoderef autoderef = lookup.coercionSequence(inferred);
        Iterator<Ty> iter = autoderef.iterator();
        boolean first = true;
        while (iter.hasNext()) {
            Ty derefTy = iter.next();
            if (first) {
                first = false;
                continue;
            }
            TyReference derefTyRef = new TyReference(derefTy, expected.getMutability(), expected.getRegion());
            Snapshot snapshot = startSnapshot();
            boolean ok = combineTypesResolved(shallowResolve(derefTyRef), shallowResolve(expected)).isOk();
            if (ok) {
                snapshot.commit();
                boolean isTrivialReborrow = autoderef.stepCount() == 1
                    && inferred.getMutability() == expected.getMutability()
                    && !expected.getMutability().isMut();
                if (!isTrivialReborrow) {
                    List<Adjustment> adjs = new ArrayList<>(Autoderef.toAdjustments(autoderef.steps(), items));
                    adjs.add(new Adjustment.BorrowReference(derefTyRef));
                    return new RsResult.Ok<>(new CoerceOk(adjs, autoderef.obligations()));
                }
                return new RsResult.Ok<>(new CoerceOk(Collections.emptyList(), autoderef.obligations()));
            } else {
                snapshot.rollback();
            }
        }
        return new RsResult.Err<>(new TypeError.TypeMismatch(inferred, expected));
    }

    private boolean coerceMutability(@NotNull Mutability from, @NotNull Mutability to) {
        return from == to || from.isMut() && !to.isMut();
    }

    // --- Resolution ---

    @NotNull
    public <T extends TypeFoldable<T>> T shallowResolve(@NotNull T value) {
        return value.foldWith(myShallowResolver);
    }

    @NotNull
    public Ty resolveTypeVarsWithObligations(@NotNull Ty ty) {
        if (!FoldUtil.needsInfer(ty)) return ty;
        Ty tyRes = resolveTypeVarsIfPossible(ty);
        if (!FoldUtil.needsInfer(tyRes)) return tyRes;
        fulfill.selectWherePossible();
        return resolveTypeVarsIfPossible(tyRes);
    }

    @NotNull
    public <T extends TypeFoldable<T>> T resolveTypeVarsIfPossible(@NotNull T value) {
        return value.foldWith(myOpportunisticVarResolver);
    }

    @NotNull
    public <T extends TypeFoldable<T>> T fullyResolve(@NotNull T value) {
        return value.foldWith(myFullTypeResolver);
    }

    @NotNull
    public <T extends TypeFoldable<T>> T fullyResolveWithOrigins(@NotNull T value) {
        return value.foldWith(myFullTypeWithOriginsResolver);
    }

    @NotNull
    public Ty typeVarForParam(@NotNull TyTypeParameter ty) {
        return new TyInfer.TyVar(ty);
    }

    @NotNull
    public Const constVarForParam(@NotNull CtConstParameter c) {
        return new CtInferVar(c);
    }

    @NotNull
    public <T extends TypeFoldable<T>> T fullyNormalizeAssociatedTypesIn(@NotNull T ty) {
        TyWithObligations<T> result = normalizeAssociatedTypesIn(ty);
        for (Obligation o : result.getObligations()) {
            fulfill.registerPredicateObligation(o);
        }
        fulfill.selectWherePossible();
        return fullyResolve(result.getValue());
    }

    @NotNull
    public <T extends TypeFoldable<T>> TyWithObligations<T> normalizeAssociatedTypesIn(@NotNull T ty) {
        return normalizeAssociatedTypesIn(ty, 0);
    }

    @NotNull
    public <T extends TypeFoldable<T>> TyWithObligations<T> normalizeAssociatedTypesIn(@NotNull T ty, int recursionDepth) {
        List<Obligation> obligations = new ArrayList<>();
        T normTy = ty.foldWith(new TypeFolder() {
            @NotNull
            @Override
            public Ty foldTy(@NotNull Ty t) {
                if (t instanceof TyProjection) {
                    TyWithObligations<Ty> normResult = normalizeProjectionType((TyProjection) t, recursionDepth);
                    obligations.addAll(normResult.getObligations());
                    return normResult.getValue();
                }
                if (FoldUtil.hasTyProjection(t)) return t.superFoldWith(this);
                return t;
            }
        });
        return new TyWithObligations<>(normTy, obligations);
    }

    @NotNull
    private TyWithObligations<Ty> normalizeProjectionType(@NotNull TyProjection projectionTy, int recursionDepth) {
        TyWithObligations<Ty> result = optNormalizeProjectionType(projectionTy, recursionDepth);
        if (result != null) return result;
        TyInfer.TyVar tyVar = new TyInfer.TyVar(projectionTy);
        Obligation obligation = new Obligation(recursionDepth + 1, new Predicate.Projection(projectionTy, tyVar));
        return new TyWithObligations<>(tyVar, Collections.singletonList(obligation));
    }

    @Nullable
    public TyWithObligations<Ty> optNormalizeProjectionType(@NotNull TyProjection projectionTy, int recursionDepth) {
        TyProjection resolved = (TyProjection) resolveTypeVarsIfPossible(projectionTy);
        return optNormalizeProjectionTypeResolved(resolved, recursionDepth);
    }

    @Nullable
    private TyWithObligations<Ty> optNormalizeProjectionTypeResolved(@NotNull TyProjection projectionTy, int recursionDepth) {
        if (projectionTy.getType() instanceof TyInfer.TyVar) return null;

        ProjectionCacheEntry cacheResult = myProjectionCache.tryStart(projectionTy);
        if (cacheResult instanceof ProjectionCacheEntry.Ambiguous) return null;
        if (cacheResult instanceof ProjectionCacheEntry.InProgress) {
            TypeInferenceMarks.RecursiveProjectionNormalization.hit();
            return new TyWithObligations<>(TyUnknown.INSTANCE);
        }
        if (cacheResult instanceof ProjectionCacheEntry.Error) {
            return new TyWithObligations<>(TyUnknown.INSTANCE);
        }
        if (cacheResult instanceof ProjectionCacheEntry.NormalizedTy) {
            TyWithObligations<Ty> ty = ((ProjectionCacheEntry.NormalizedTy) cacheResult).getTy();
            if (!hasUnresolvedTypeVars(ty.getValue())) {
                ty = new TyWithObligations<>(ty.getValue());
                myProjectionCache.putTy(projectionTy, ty);
            }
            return ty;
        }

        // null - not in cache
        SelectionResult<?> selResult = lookup.selectProjection(projectionTy, recursionDepth);
        if (selResult instanceof SelectionResult.Ok) {
            @SuppressWarnings("unchecked")
            TyWithObligations<Ty> result = (TyWithObligations<Ty>) ((SelectionResult.Ok<?>) selResult).getResult();
            if (result == null) result = new TyWithObligations<>((Ty) projectionTy);
            myProjectionCache.putTy(projectionTy, pruneCacheValueObligations(result));
            return result;
        }
        if (selResult instanceof SelectionResult.Err) {
            myProjectionCache.error(projectionTy);
            return new TyWithObligations<>(TyUnknown.INSTANCE);
        }
        // Ambiguous
        myProjectionCache.ambiguous(projectionTy);
        return null;
    }

    @NotNull
    private TyWithObligations<Ty> pruneCacheValueObligations(@NotNull TyWithObligations<Ty> ty) {
        if (!hasUnresolvedTypeVars(ty.getValue())) return new TyWithObligations<>(ty.getValue());
        List<Obligation> filtered = new ArrayList<>();
        for (Obligation o : ty.getObligations()) {
            if (o.getPredicate() instanceof Predicate.Projection && hasUnresolvedTypeVars(o.getPredicate())) {
                filtered.add(o);
            }
        }
        return new TyWithObligations<>(ty.getValue(), filtered);
    }

    private <T extends TypeFoldable<T>> boolean hasUnresolvedTypeVars(@NotNull T ty) {
        return ty.visitWith(new TypeVisitor() {
            @Override
            public boolean visitTy(@NotNull Ty t) {
                Ty resolved = shallowResolve(t);
                if (resolved instanceof TyInfer) return true;
                if (FoldUtil.hasTyInfer(resolved)) return resolved.superVisitWith(this);
                return false;
            }
        });
    }

    public <T extends TypeFoldable<T>> boolean hasResolvableTypeVars(@NotNull T ty) {
        return FoldUtil.visitInferTys(ty, it -> !it.equals(shallowResolve(it)));
    }

    public boolean isTypeVarAffected(@NotNull TyInfer.TyVar ty) {
        return myVarUnificationTable.findRoot(ty) != ty || myVarUnificationTable.findValue(ty) != null;
    }

    @NotNull
    public Iterable<Obligation> instantiateBounds(@NotNull List<Predicate> bounds, @NotNull Substitution subst, int recursionDepth) {
        List<Obligation> result = new ArrayList<>();
        for (Predicate p : bounds) {
            Predicate substituted = FoldUtil.substitute(p, subst);
            TyWithObligations<Predicate> normalized = normalizeAssociatedTypesIn(substituted, recursionDepth);
            result.addAll(normalized.getObligations());
            result.add(new Obligation(recursionDepth, normalized.getValue()));
        }
        return result;
    }

    @NotNull
    public Substitution instantiateBounds(@NotNull RsGenericDeclaration element) {
        return instantiateBounds(element, null, SubstitutionUtil.EMPTY_SUBSTITUTION);
    }

    @NotNull
    public Substitution instantiateBounds(@NotNull RsGenericDeclaration element, @Nullable Ty selfTy, @NotNull Substitution subst) {
        Map<TyTypeParameter, Ty> typeSubst = new HashMap<>();
        for (TyTypeParameter gen : TypeInferenceUtil.getGenerics(element)) {
            typeSubst.put(gen, typeVarForParam(gen));
        }
        if (selfTy != null) {
            typeSubst.put(TyTypeParameter.self(), selfTy);
        }
        Map<CtConstParameter, Const> constSubst = new HashMap<>();
        for (CtConstParameter cgen : TypeInferenceUtil.getConstGenerics(element)) {
            constSubst.put(cgen, constVarForParam(cgen));
        }
        Substitution map = subst.plus(new Substitution(typeSubst, Collections.emptyMap(), constSubst));
        @SuppressWarnings("unchecked")
        List<Predicate> predicates = (List<Predicate>) (List<?>) TypeInferenceUtil.getPredicates(element);
        for (Obligation o : instantiateBounds(predicates, map, 0)) {
            fulfill.registerPredicateObligation(o);
        }
        return map;
    }

    public boolean canEvaluateBounds(@NotNull TraitImplSource source, @NotNull Ty selfTy) {
        if (source instanceof TraitImplSource.ExplicitImpl) {
            return canEvaluateBoundsImpl(((TraitImplSource.ExplicitImpl) source).getValue(), selfTy);
        }
        if (source instanceof TraitImplSource.Derived || source instanceof TraitImplSource.Builtin) {
            if (!source.getValue().getTypeParameters().isEmpty()) return true;
            return lookup.canSelect(new TraitRef(selfTy, new BoundElement<>((RsTraitItem) source.getValue())));
        }
        return true;
    }

    private boolean canEvaluateBoundsImpl(@NotNull RsImplItem impl, @NotNull Ty selfTy) {
        FulfillmentContext ff = new FulfillmentContext(this, lookup);
        Map<TyTypeParameter, Ty> typeSubst = new HashMap<>();
        for (TyTypeParameter gen : TypeInferenceUtil.getGenerics(impl)) {
            typeSubst.put(gen, typeVarForParam(gen));
        }
        Map<CtConstParameter, Const> constSubst = new HashMap<>();
        for (CtConstParameter cgen : TypeInferenceUtil.getConstGenerics(impl)) {
            constSubst.put(cgen, constVarForParam(cgen));
        }
        Substitution subst = new Substitution(typeSubst, Collections.emptyMap(), constSubst);
        return probe(() -> {
            @SuppressWarnings("unchecked")
            List<Predicate> implPredicates = (List<Predicate>) (List<?>) TypeInferenceUtil.getPredicates(impl);
            for (Obligation o : instantiateBounds(implPredicates, subst, 0)) {
                ff.registerPredicateObligation(o);
            }
            RsTypeReference typeRef = impl.getTypeReference();
            if (typeRef != null) {
                Ty implTy = FoldUtil.substitute(ExtensionsUtil.getRawType(typeRef), subst);
                combineTypes(selfTy, implTy);
            }
            return ff.selectUntilError();
        });
    }

    @NotNull
    public Substitution instantiateMethodOwnerSubstitution(@NotNull AssocItemScopeEntryBase<?> callee) {
        return instantiateMethodOwnerSubstitution(callee, null);
    }

    @NotNull
    public Substitution instantiateMethodOwnerSubstitution(@NotNull AssocItemScopeEntryBase<?> callee, @Nullable RsMethodCall methodCall) {
        return instantiateMethodOwnerSubstitutionImpl(callee.getSource(), callee.getSelfTy(), callee.getElement(), methodCall);
    }

    @NotNull
    public Substitution instantiateMethodOwnerSubstitution(@NotNull MethodPick callee) {
        return instantiateMethodOwnerSubstitution(callee, null);
    }

    @NotNull
    public Substitution instantiateMethodOwnerSubstitution(@NotNull MethodPick callee, @Nullable RsMethodCall methodCall) {
        return instantiateMethodOwnerSubstitutionImpl(callee.getSource(), callee.getFormalSelfTy(), callee.getElement(), methodCall);
    }

    @NotNull
    private Substitution instantiateMethodOwnerSubstitutionImpl(
        @NotNull TraitImplSource source,
        @NotNull Ty selfTy,
        @NotNull RsAbstractable element,
        @Nullable RsMethodCall methodCall
    ) {
        if (source instanceof TraitImplSource.ExplicitImpl) {
            RsImplItem impl = ((TraitImplSource.ExplicitImpl) source).getValue();
            Substitution typeParameters = instantiateBounds(impl);
            Ty implTy = ((TraitImplSource.ExplicitImpl) source).getType();
            if (implTy != null) {
                combineTypes(selfTy, FoldUtil.substitute(implTy, typeParameters));
            }
            if (element.getOwner() instanceof RsAbstractableOwner.Trait) {
                BoundElement<RsTraitItem> implTrait = ((TraitImplSource.ExplicitImpl) source).getImplementedTrait();
                return implTrait != null ? FoldUtil.substitute(implTrait, typeParameters).getSubst() : SubstitutionUtil.EMPTY_SUBSTITUTION;
            }
            return typeParameters;
        }
        if (source instanceof TraitImplSource.TraitBound) {
            Iterable<BoundElement<RsTraitItem>> bounds = lookup.getEnvBoundTransitivelyFor(selfTy);
            for (BoundElement<RsTraitItem> b : bounds) {
                if (b.getElement() == ((TraitImplSource.TraitBound) source).getValue()) return b.getSubst();
            }
            return SubstitutionUtil.EMPTY_SUBSTITUTION;
        }
        if (source instanceof TraitImplSource.Collapsed || source instanceof TraitImplSource.Builtin) {
            RsTraitItem trait = (RsTraitItem) source.getValue();
            Substitution typeParameters = instantiateBounds(trait);
            Map<TyTypeParameter, Ty> tsMap = new HashMap<>();
            for (TyTypeParameter gen : TypeInferenceUtil.getGenerics(trait)) {
                tsMap.put(gen, gen);
            }
            Map<CtConstParameter, Const> csMap = new HashMap<>();
            for (CtConstParameter cgen : TypeInferenceUtil.getConstGenerics(trait)) {
                csMap.put(cgen, cgen);
            }
            Substitution traitSubst = new Substitution(tsMap, Collections.emptyMap(), csMap);
            BoundElement<RsTraitItem> boundTrait = FoldUtil.substitute(new BoundElement<>(trait, traitSubst), typeParameters);
            TraitRef traitRef = new TraitRef(selfTy, boundTrait);
            fulfill.registerPredicateObligation(new Obligation(new Predicate.Trait(traitRef)));
            if (methodCall != null) {
                registerMethodRefinement(methodCall, traitRef);
            }
            return typeParameters;
        }
        return SubstitutionUtil.EMPTY_SUBSTITUTION;
    }

    public void convertPlaceDerefsToMutable(@NotNull RsExpr receiver) {
        List<RsExpr> exprs = new ArrayList<>();
        exprs.add(receiver);
        while (true) {
            RsExpr last = exprs.get(exprs.size() - 1);
            RsExpr next = null;
            if (last instanceof RsIndexExpr) {
                next = RsIndexExprUtil.getContainerExpr((RsIndexExpr) last);
            } else if (last instanceof RsUnaryExpr && RsUnaryExprExtUtil.isDereference((RsUnaryExpr) last)) {
                next = ((RsUnaryExpr) last).getExpr();
            } else if (last instanceof RsDotExpr && ((RsDotExpr) last).getFieldLookup() != null) {
                next = ((RsDotExpr) last).getExpr();
            } else if (last instanceof RsParenExpr) {
                next = ((RsParenExpr) last).getExpr();
            }
            if (next == null) break;
            exprs.add(next);
        }

        for (int i = exprs.size() - 1; i >= 0; i--) {
            RsExpr expr = exprs.get(i);
            List<Adjustment> exprAdjustments = myAdjustments.get(expr);
            if (exprAdjustments != null) {
                for (int j = 0; j < exprAdjustments.size(); j++) {
                    Adjustment adj = exprAdjustments.get(j);
                    if (adj instanceof Adjustment.Deref && ((Adjustment.Deref) adj).getOverloaded() == Mutability.IMMUTABLE) {
                        exprAdjustments.set(j, new Adjustment.Deref(adj.getTarget(), Mutability.MUTABLE));
                    }
                }
            }

            RsExpr base = null;
            if (expr instanceof RsIndexExpr) {
                base = RsIndexExprUtil.getContainerExpr((RsIndexExpr) expr);
            } else if (expr instanceof RsUnaryExpr && RsUnaryExprExtUtil.isDereference((RsUnaryExpr) expr)) {
                base = ((RsUnaryExpr) expr).getExpr();
            }
            if (base == null) continue;
            base = RsExprUtil.unwrapParenExprs(base);

            List<Adjustment> baseAdjustments = myAdjustments.get(base);
            if (baseAdjustments == null) continue;

            for (int j = 0; j < baseAdjustments.size(); j++) {
                Adjustment adj = baseAdjustments.get(j);
                if (adj instanceof Adjustment.BorrowReference
                    && ((Adjustment.BorrowReference) adj).getMutability() == AutoBorrowMutability.Immutable) {
                    TyReference target = ((Adjustment.BorrowReference) adj).getTarget();
                    baseAdjustments.set(j, new Adjustment.BorrowReference(target.copy(Mutability.MUTABLE)));
                }
            }

            if (!baseAdjustments.isEmpty()) {
                Adjustment lastAdj = baseAdjustments.get(baseAdjustments.size() - 1);
                if (lastAdj instanceof Adjustment.Unsize
                    && baseAdjustments.size() >= 2
                    && baseAdjustments.get(baseAdjustments.size() - 2) instanceof Adjustment.BorrowReference
                    && lastAdj.getTarget() instanceof TyReference) {
                    TyReference lastTarget = (TyReference) lastAdj.getTarget();
                    baseAdjustments.set(baseAdjustments.size() - 1, new Adjustment.Unsize(lastTarget.copy(Mutability.MUTABLE)));
                }
            }
        }
    }

    // --- Inner type folder/visitor classes ---

    private class ShallowResolver implements TypeFolder {
        @NotNull
        @Override
        public Ty foldTy(@NotNull Ty ty) {
            return shallowResolveTy(ty);
        }

        @NotNull
        @Override
        public Const foldConst(@NotNull Const c) {
            if (c instanceof CtInferVar) {
                Const value = myConstUnificationTable.findValue((CtInferVar) c);
                return value != null ? value : c;
            }
            return c;
        }

        @NotNull
        private Ty shallowResolveTy(@NotNull Ty ty) {
            if (!(ty instanceof TyInfer)) return ty;
            if (ty instanceof TyInfer.IntVar) {
                TyInteger val = myIntUnificationTable.findValue((TyInfer.IntVar) ty);
                return val != null ? val : ty;
            }
            if (ty instanceof TyInfer.FloatVar) {
                TyFloat val = myFloatUnificationTable.findValue((TyInfer.FloatVar) ty);
                return val != null ? val : ty;
            }
            if (ty instanceof TyInfer.TyVar) {
                Ty val = myVarUnificationTable.findValue((TyInfer.TyVar) ty);
                return val != null ? shallowResolveTy(val) : ty;
            }
            return ty;
        }
    }

    private class OpportunisticVarResolver implements TypeFolder {
        @NotNull
        @Override
        public Ty foldTy(@NotNull Ty ty) {
            if (!FoldUtil.needsInfer(ty)) return ty;
            Ty res = shallowResolve(ty);
            return res.superFoldWith(this);
        }

        @NotNull
        @Override
        public Const foldConst(@NotNull Const c) {
            if (!FoldUtil.hasCtInfer(c)) return c;
            Const res = shallowResolve(c);
            return res.superFoldWith(this);
        }
    }

    private class FullTypeResolver implements TypeFolder {
        @NotNull
        @Override
        public Ty foldTy(@NotNull Ty ty) {
            if (!FoldUtil.needsInfer(ty)) return ty;
            Ty res = shallowResolve(ty);
            return res instanceof TyInfer ? TyUnknown.INSTANCE : res.superFoldWith(this);
        }

        @NotNull
        @Override
        public Const foldConst(@NotNull Const c) {
            if (c instanceof CtInferVar) {
                Const val = myConstUnificationTable.findValue((CtInferVar) c);
                return val != null ? val : CtUnknown.INSTANCE;
            }
            return c;
        }
    }

    private class FullTypeWithOriginsResolver implements TypeFolder {
        @NotNull
        @Override
        public Ty foldTy(@NotNull Ty ty) {
            if (!FoldUtil.needsInfer(ty)) return ty;
            Ty res = shallowResolve(ty);
            if (res instanceof TyUnknown) {
                if (ty instanceof TyInfer.TyVar) {
                    Ty origin = ((TyInfer.TyVar) ty).getOrigin();
                    return origin instanceof TyTypeParameter ? origin : TyUnknown.INSTANCE;
                }
                return TyUnknown.INSTANCE;
            }
            if (res instanceof TyInfer.TyVar) {
                Ty origin = ((TyInfer.TyVar) res).getOrigin();
                return origin instanceof TyTypeParameter ? origin : TyUnknown.INSTANCE;
            }
            if (res instanceof TyInfer) return TyUnknown.INSTANCE;
            return res.superFoldWith(this);
        }

        @NotNull
        @Override
        public Const foldConst(@NotNull Const c) {
            if (c instanceof CtInferVar) {
                Const val = myConstUnificationTable.findValue((CtInferVar) c);
                return val != null ? val : CtUnknown.INSTANCE;
            }
            return c;
        }
    }

    // --- Utility ---

    @NotNull
    private static <T> List<com.intellij.openapi.util.Pair<T, T>> zip(@NotNull List<T> a, @NotNull List<T> b) {
        int size = Math.min(a.size(), b.size());
        List<com.intellij.openapi.util.Pair<T, T>> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(new com.intellij.openapi.util.Pair<>(a.get(i), b.get(i)));
        }
        return result;
    }
}
