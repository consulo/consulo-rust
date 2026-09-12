/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import consulo.application.progress.ProgressManager;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.macros.MacroExpansion;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.*;
import org.rust.lang.core.resolve.ref.*;
import org.rust.lang.core.stubs.RsStubLiteralKind;
import org.rust.lang.core.types.*;
import org.rust.lang.core.types.consts.*;
import org.rust.lang.core.types.regions.ReStatic;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.ty.*;
import org.rust.lang.utils.RsDiagnostic;
import org.rust.lang.utils.evaluation.ConstExpr;
import org.rust.lang.utils.evaluation.ConstExprEvaluator;
import org.rust.lang.utils.evaluation.PathExprResolver;
import org.rust.stdext.CollectionsUtil;
import org.rust.openapiext.PsiExtUtil;
import org.rust.stdext.RsResult;
import consulo.util.lang.Pair;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsSelfParameterUtil;

/**
 * Walks function bodies and infers types for all expressions.
 *
 * Private helper methods delegate to the RsInferenceContext as needed.
 */
class RsTypeInferenceWalker {
    @Nonnull
    private final RsInferenceContext ctx;
    @Nonnull
    private final Ty returnTy;
    @Nullable
    private Ty tryTy;
    @Nullable
    private Ty yieldTy;

    /** Folds a type by normalizing every associated type projection inside it. */
    @Nonnull
    private final TypeFolder associatedTypeNormalizer = new TypeFolder() {
        @Nonnull
        @Override
        public Ty foldTy(@Nonnull Ty ty) {
            return normalizeAssociatedTypesIn(ty);
        }
    };

    public RsTypeInferenceWalker(@Nonnull RsInferenceContext ctx, @Nonnull Ty returnTy) {
        this.ctx = ctx;
        this.returnTy = returnTy;
        this.tryTy = returnTy;
        this.yieldTy = null;
    }

    @Nonnull
    public RsInferenceContext getCtx() {
        return ctx;
    }

    @Nonnull
    private ImplLookup getLookup() {
        return ctx.lookup;
    }

    @Nonnull
    private KnownItems getItems() {
        return ctx.items;
    }

    @Nonnull
    private FulfillmentContext getFulfill() {
        return ctx.fulfill;
    }

    @Nonnull
    private Ty resolveTypeVarsWithObligations(@Nonnull Ty ty) {
        return ctx.resolveTypeVarsWithObligations(ty);
    }

    private void selectObligationsWherePossible() {
        getFulfill().selectWherePossible();
    }

    @Nonnull
    public Ty inferFnBody(@Nonnull RsBlock block) {
        return inferBlockTypeCoercableTo(block, returnTy);
    }

    @Nonnull
    public Ty inferLambdaBody(@Nonnull RsExpr expr) {
        return inferExprCoercableTo(expr, returnTy);
    }

    @Nonnull
    private Ty inferBlockTypeCoercableTo(@Nonnull RsBlock block, @Nonnull Ty expected) {
        return inferBlockType(block, new Expectation.ExpectHasType(expected), true);
    }

    @Nonnull
    private Ty inferBlockType(@Nonnull RsBlock block, @Nonnull Expectation expected, boolean coerce) {
        boolean isDiverging = false;
        RsBlockUtil.ExpandedStmtsAndTailExpr expandedStmts = RsBlockUtil.getExpandedStmtsAndTailExpr(block);
        List<RsStmt> stmts = expandedStmts.getStmts();
        RsExpr tailExpr = expandedStmts.getTailExpr();

        for (RsStmt stmt : stmts) {
            boolean result = processStatement(stmt);
            isDiverging = result || isDiverging;
        }

        Ty type;
        if (coerce && expected instanceof Expectation.ExpectHasType) {
            type = tailExpr != null ? inferExprCoercableTo(tailExpr, ((Expectation.ExpectHasType) expected).getTy()) : null;
        } else {
            type = tailExpr != null ? inferExprType(tailExpr, expected) : null;
        }
        if (type == null) type = TyUnit.INSTANCE;
        return isDiverging ? TyNever.INSTANCE : type;
    }

    public void inferReplCodeFragment(@Nonnull RsReplCodeFragment element) {
        for (PsiElement child : element.getStmtList()) {
            if (child instanceof RsStmt) {
                processStatement((RsStmt) child);
            }
        }
    }

    /**
     * Process a statement. Returns true if the expression is always diverging.
     */
    private boolean processStatement(@Nonnull RsStmt psi) {
        if (psi instanceof RsLetDecl letDecl) {
            Ty explicitTy = null;
            if (letDecl.getTypeReference() != null) {
                explicitTy = normalizeAssociatedTypesIn(ExtensionsUtil.getRawType(letDecl.getTypeReference()));
            }
            RsExpr expr = letDecl.getExpr();
            RsPat pat = letDecl.getPat();

            Ty inferredTy;
            Ty coercedInferredTy;
            if (expr != null) {
                inferredTy = inferExprType(expr, Expectation.maybeHasType(explicitTy));
                if (explicitTy != null && coerce(expr, inferredTy, explicitTy)) {
                    coercedInferredTy = explicitTy;
                } else {
                    coercedInferredTy = inferredTy;
                }
            } else {
                inferredTy = TyUnknown.INSTANCE;
                coercedInferredTy = new TyInfer.TyVar();
            }
            if (pat != null) {
                Ty binding = explicitTy != null ? explicitTy : resolveTypeVarsWithObligations(coercedInferredTy);
                PatternMatchingUtil.extractBindings(pat, this, binding);
            }
            RsLetElseBranch elseBranch = letDecl.getLetElseBranch();
            if (elseBranch != null && elseBranch.getBlock() != null) {
                inferBlockType(elseBranch.getBlock(), new Expectation.ExpectHasType(TyNever.INSTANCE), true);
            }
            return inferredTy == TyNever.INSTANCE;
        } else if (psi instanceof RsExprStmt exprStmt) {
            return inferExprType(exprStmt.getExpr()) == TyNever.INSTANCE;
        }
        return false;
    }

    @Nonnull
    public Ty inferExprType(@Nonnull RsExpr expr) {
        return inferExprType(expr, Expectation.NoExpectation);
    }

    @Nonnull
    public Ty inferExprType(@Nonnull RsExpr expr, @Nonnull Expectation expected) {
        ProgressManager.checkCanceled();
        if (ctx.isTypeInferred(expr)) {
            throw new IllegalStateException("Trying to infer expression type twice");
        }

        Ty expectedTy = expected.tyAsNullable(ctx);
        if (expectedTy != null) {
            if (expr instanceof RsPathExpr || expr instanceof RsDotExpr || expr instanceof RsCallExpr) {
                ctx.writeExpectedExprTy(expr, expectedTy);
            }
        }

        Ty ty = inferExprTypeInner(expr, expected);
        ctx.writeExprTy(expr, ty);
        return ty;
    }

    @Nonnull
    private Ty inferExprTypeInner(@Nonnull RsExpr expr, @Nonnull Expectation expected) {
        if (expr instanceof RsPathExpr) return inferPathExprType((RsPathExpr) expr);
        if (expr instanceof RsStructLiteral) return inferStructLiteralType((RsStructLiteral) expr, expected);
        if (expr instanceof RsTupleExpr) return inferTupleExprType((RsTupleExpr) expr, expected);
        if (expr instanceof RsParenExpr) {
            RsExpr inner = ((RsParenExpr) expr).getExpr();
            return inner != null ? inferExprType(inner, expected) : TyUnknown.INSTANCE;
        }
        if (expr instanceof RsUnitExpr) return TyUnit.INSTANCE;
        if (expr instanceof RsCastExpr) return inferCastExprType((RsCastExpr) expr);
        if (expr instanceof RsCallExpr) return inferCallExprType((RsCallExpr) expr, expected);
        if (expr instanceof RsDotExpr) return inferDotExprType((RsDotExpr) expr, expected);
        if (expr instanceof RsLitExpr) return inferLitExprType((RsLitExpr) expr, expected);
        if (expr instanceof RsBlockExpr) return inferBlockExprType((RsBlockExpr) expr, expected);
        if (expr instanceof RsIfExpr) return inferIfExprType((RsIfExpr) expr, expected);
        if (expr instanceof RsLoopExpr) return inferLoopExprType((RsLoopExpr) expr);
        if (expr instanceof RsWhileExpr) return inferWhileExprType((RsWhileExpr) expr);
        if (expr instanceof RsForExpr) return inferForExprType((RsForExpr) expr);
        if (expr instanceof RsMatchExpr) return inferMatchExprType((RsMatchExpr) expr, expected);
        if (expr instanceof RsUnaryExpr) return inferUnaryExprType((RsUnaryExpr) expr, expected);
        if (expr instanceof RsBinaryExpr) return inferBinaryExprType((RsBinaryExpr) expr);
        if (expr instanceof RsTryExpr) return inferTryExprType((RsTryExpr) expr);
        if (expr instanceof RsArrayExpr) return inferArrayType((RsArrayExpr) expr, expected);
        if (expr instanceof RsRangeExpr) return inferRangeType((RsRangeExpr) expr);
        if (expr instanceof RsIndexExpr) return inferIndexExprType((RsIndexExpr) expr);
        if (expr instanceof RsMacroExpr) return inferMacroExprType((RsMacroExpr) expr, expected);
        if (expr instanceof RsLambdaExpr) return inferLambdaExprType((RsLambdaExpr) expr, expected);
        if (expr instanceof RsYieldExpr) return inferYieldExprType((RsYieldExpr) expr);
        if (expr instanceof RsRetExpr) return inferRetExprType((RsRetExpr) expr);
        if (expr instanceof RsBreakExpr) return inferBreakExprType((RsBreakExpr) expr);
        if (expr instanceof RsLetExpr) return inferLetExprType((RsLetExpr) expr);
        if (expr instanceof RsContExpr) return TyNever.INSTANCE;
        return TyUnknown.INSTANCE;
    }

    @Nonnull
    public Ty inferExprCoercableTo(@Nonnull RsExpr expr, @Nonnull Ty expected) {
        Ty inferred = inferExprType(expr, Expectation.maybeHasType(expected));
        return coerce(expr, inferred, expected) ? expected : inferred;
    }

    @Nonnull
    public Ty inferTypeCoercableTo(@Nonnull RsExpr expr, @Nonnull Ty expected) {
        return inferExprCoercableTo(expr, expected);
    }

    @Nonnull
    public Ty inferType(@Nonnull RsExpr expr) {
        return inferExprType(expr);
    }

    public boolean coerce(@Nonnull RsElement element, @Nonnull Ty inferred, @Nonnull Ty expected) {
        return coerceResolved(
            element,
            resolveTypeVarsWithObligations(inferred),
            resolveTypeVarsWithObligations(expected)
        );
    }

    private boolean coerceResolved(@Nonnull RsElement element, @Nonnull Ty inferred, @Nonnull Ty expected) {
        if (element instanceof RsExpr) {
            ctx.writeExpectedExprTyCoercable((RsExpr) element);
        }
        RsResult<CoerceOk, TypeError> result = ctx.tryCoerce(inferred, expected);
        if (result.isOk()) {
            CoerceOk ok = ((RsResult.Ok<CoerceOk, TypeError>) result).getOk();
            ctx.applyAdjustments(element, ok.getAdjustments());
            getFulfill().registerPredicateObligations(ok.getObligations());
            return true;
        } else {
            TypeError err = ((RsResult.Err<CoerceOk, TypeError>) result).getErr();
            if (err instanceof TypeError.TypeMismatch mismatch) {
                checkTypeMismatch(mismatch, element, inferred, expected);
            } else if (err instanceof TypeError.ConstMismatch cm) {
                if (!IGNORED_CONSTS.contains(cm.getConst1().getClass()) && !IGNORED_CONSTS.contains(cm.getConst2().getClass())) {
                    reportTypeMismatch(element, expected, inferred);
                }
            }
            return false;
        }
    }

    private void checkTypeMismatch(@Nonnull TypeError.TypeMismatch result, @Nonnull RsElement element, @Nonnull Ty inferred, @Nonnull Ty expected) {
        if (IGNORED_TYS.contains(result.getTy1().getClass()) || IGNORED_TYS.contains(result.getTy2().getClass())) return;
        if (expected instanceof TyReference && inferred instanceof TyReference) {
            if (FoldUtil.containsTyOfClass(expected, IGNORED_TYS) || FoldUtil.containsTyOfClass(inferred, IGNORED_TYS)) {
                if (!(((TyReference) expected).getMutability() == Mutability.MUTABLE
                    && ((TyReference) inferred).getMutability() == Mutability.IMMUTABLE)) {
                    return;
                }
            }
        }
        reportTypeMismatch(element, expected, inferred);
    }

    private void reportTypeMismatch(@Nonnull RsElement element, @Nonnull Ty expected, @Nonnull Ty inferred) {
        if (ctx.diagnostics.stream().noneMatch(d -> element.equals(d.getElement()) || RsPsiJavaUtil.isAncestorOf(element, d.getElement()))) {
            ctx.reportTypeMismatch(element, expected, inferred);
        }
    }


    @Nonnull
    private Ty inferPathExprType(@Nonnull RsPathExpr expr) {
        // Full implementation delegates to path resolution and instantiation
        // Simplified: resolve and instantiate
        RsPath path = expr.getPath();
        List<?> resolveVariants = RsPathReferenceImpl.resolvePathRaw(path, getLookup(), true);
        ctx.writePath(expr, resolveVariants.stream()
            .map(v -> ResolvedPath.from((ScopeEntry) v, expr))
            .collect(Collectors.toList()));
        ScopeEntry first = resolveVariants.size() == 1 ? (ScopeEntry) resolveVariants.get(0) : null;
        if (first == null) return TyUnknown.INSTANCE;
        return instantiatePath(first.getElement(), first, expr);
    }

    /**
     * The type of a path used in expression position: the resolved item's type with the generic arguments
     * written in the path (and those inferred for its owner) substituted in. A tuple struct or tuple enum
     * variant yields the type of its constructor function.
     */
    @Nonnull
    private Ty instantiatePath(@Nonnull RsElement element, @Nonnull ScopeEntry scopeEntry, @Nonnull RsPathExpr pathExpr) {
        if (element instanceof RsImplItem) {
            RsTypeReference implTypeRef = ((RsImplItem) element).getTypeReference();
            Ty implForTy = implTypeRef != null
                ? normalizeAssociatedTypesIn(ExtensionsUtil.getRawType(implTypeRef))
                : TyUnknown.INSTANCE;
            RsElement item = implForTy instanceof TyAdt ? ((TyAdt) implForTy).getItem() : null;
            RsTupleFields tupleFields = item instanceof RsFieldsOwner ? ((RsFieldsOwner) item).getTupleFields() : null;
            Ty implResult = tupleFields != null
                ? FoldUtil.substitute(callableTy(tupleFields, implForTy, (RsFieldsOwner) item), implForTy.getTypeParameterValues())
                : implForTy;
            return implResult.foldWith(associatedTypeNormalizer);
        }

        RsPath path = pathExpr.getPath();

        if (element instanceof RsGenericDeclaration) {
            inferConstArgumentTypes(
                RsGenericDeclarationUtil.getConstParameters((RsGenericDeclaration) element),
                RsMethodOrPathUtil.getConstArguments(path)
            );
        }

        Substitution subst = TyLowering.lowerPathGenerics(
            path,
            element,
            scopeEntry.getSubst(),
            PathExprResolver.fromContext(ctx),
            Collections.emptyMap()
        ).getSubst();

        Substitution typeParameters = SubstitutionUtil.EMPTY_SUBSTITUTION;
        if (scopeEntry instanceof AssocItemScopeEntry && element instanceof RsAbstractable) {
            RsAbstractableOwner owner = ((RsAbstractable) element).getOwner();
            Substitution ownerParameters = SubstitutionUtil.EMPTY_SUBSTITUTION;
            Ty selfTy = null;
            if (owner instanceof RsAbstractableOwner.Impl) {
                RsImplItem impl = ((RsAbstractableOwner.Impl) owner).getImpl();
                ownerParameters = ctx.instantiateBounds(impl);
                RsTypeReference implTypeRef = impl.getTypeReference();
                selfTy = implTypeRef != null
                    ? FoldUtil.substitute(ExtensionsUtil.getRawType(implTypeRef), ownerParameters)
                    : TyUnknown.INSTANCE;
                Ty writtenSelfTy = subst.get(TyTypeParameter.self());
                if (writtenSelfTy != null) {
                    ctx.combineTypes(selfTy, writtenSelfTy);
                }
            }
            else if (owner instanceof RsAbstractableOwner.Trait) {
                RsTraitItem trait = ((RsAbstractableOwner.Trait) owner).getTrait();
                ownerParameters = ctx.instantiateBounds(trait);
                Ty writtenSelfTy = subst.get(TyTypeParameter.self());
                selfTy = writtenSelfTy != null ? writtenSelfTy : ctx.typeVarForParam(TyTypeParameter.self());
                // Fully qualified call syntax: add the predicate `Self: Trait<Args>`
                Map<TyTypeParameter, Ty> traitTypeSubst = new HashMap<>();
                for (TyTypeParameter generic : TypeInferenceUtil.getGenerics(trait)) {
                    traitTypeSubst.put(generic, generic);
                }
                Map<CtConstParameter, Const> traitConstSubst = new HashMap<>();
                for (CtConstParameter generic : TypeInferenceUtil.getConstGenerics(trait)) {
                    traitConstSubst.put(generic, generic);
                }
                BoundElement<RsTraitItem> boundTrait = FoldUtil.substitute(
                    new BoundElement<>(trait, new Substitution(traitTypeSubst, Collections.emptyMap(), traitConstSubst)),
                    ownerParameters
                );
                TraitRef traitRef = new TraitRef(selfTy, boundTrait);
                getFulfill().registerPredicateObligation(new Obligation(new Predicate.Trait(traitRef)));
                TraitImplSource source = ((AssocItemScopeEntry) scopeEntry).getSource();
                if (source instanceof TraitImplSource.Trait || source instanceof TraitImplSource.Collapsed) {
                    ctx.registerPathRefinement(pathExpr, traitRef);
                }
            }
            typeParameters = element instanceof RsGenericDeclaration
                ? ctx.instantiateBounds((RsGenericDeclaration) element, selfTy, ownerParameters)
                : ownerParameters;
        }
        else if (element instanceof RsEnumVariant) {
            typeParameters = ctx.instantiateBounds(RsEnumVariantUtil.getParentEnum((RsEnumVariant) element));
        }
        else if (element instanceof RsGenericDeclaration) {
            typeParameters = ctx.instantiateBounds((RsGenericDeclaration) element);
        }

        unifySubst(subst, typeParameters);
        ctx.writePathSubst(pathExpr, typeParameters);

        Ty type;
        if (element instanceof RsPatBinding) {
            type = ctx.getBindingType((RsPatBinding) element);
        }
        else if (element instanceof RsTypeDeclarationElement) {
            type = ((RsTypeDeclarationElement) element).getDeclaredType();
        }
        else if (element instanceof RsEnumVariant) {
            type = RsEnumVariantUtil.getParentEnum((RsEnumVariant) element).getDeclaredType();
        }
        else if (element instanceof RsFunction) {
            type = RsTypeInferenceWalkerHelper.getFunctionType((RsFunction) element);
        }
        else if (element instanceof RsConstant) {
            RsTypeReference typeRef = ((RsConstant) element).getTypeReference();
            type = typeRef != null ? ExtensionsUtil.getRawType(typeRef) : TyUnknown.INSTANCE;
        }
        else if (element instanceof RsConstParameter) {
            RsTypeReference typeRef = ((RsConstParameter) element).getTypeReference();
            type = typeRef != null ? ExtensionsUtil.getRawType(typeRef) : TyUnknown.INSTANCE;
        }
        else if (element instanceof RsSelfParameter) {
            type = RsTypeInferenceWalkerHelper.getTypeOfSelfParameter((RsSelfParameter) element);
        }
        else {
            return TyUnknown.INSTANCE;
        }

        RsTupleFields tupleFields = element instanceof RsFieldsOwner ? ((RsFieldsOwner) element).getTupleFields() : null;
        Ty result = tupleFields != null ? callableTy(tupleFields, type, (RsFieldsOwner) element) : type;
        return FoldUtil.substitute(result, typeParameters).foldWith(associatedTypeNormalizer);
    }

    /** Treats a tuple struct or tuple enum variant as a function from its field types to itself. */
    @Nonnull
    private TyFunctionBase callableTy(@Nonnull RsTupleFields tupleFields, @Nonnull Ty implForTy, @Nonnull RsFieldsOwner item) {
        List<Ty> paramTypes = new ArrayList<>();
        for (RsTupleFieldDecl fieldDecl : tupleFields.getTupleFieldDeclList()) {
            paramTypes.add(ExtensionsUtil.getRawType(fieldDecl.getTypeReference()));
        }
        FnSig fnSig = new FnSig(paramTypes, implForTy);
        if (item instanceof RsEnumVariant) {
            return new TyFunctionDef(new RsCallable.EnumVariant((RsEnumVariant) item), fnSig);
        }
        if (item instanceof RsStructItem) {
            return new TyFunctionDef(new RsCallable.StructItem((RsStructItem) item), fnSig);
        }
        return new TyFunctionPointer(fnSig);
    }

    @Nonnull
    private Ty inferStructLiteralType(@Nonnull RsStructLiteral expr, @Nonnull Expectation expected) {
        BoundElement<RsElement> boundElement = expr.getPath().getReference() != null
            ? RsPathReferenceImpl.advancedDeepResolve(expr.getPath().getReference())
            : null;
        if (boundElement == null) {
            for (RsStructLiteralField field : expr.getStructLiteralBody().getStructLiteralFieldList()) {
                if (field.getExpr() != null) inferExprType(field.getExpr());
            }
            // Struct update syntax `{ ..expression }`
            if (expr.getStructLiteralBody().getExpr() != null) {
                inferExprType(expr.getStructLiteralBody().getExpr());
            }
            return TyUnknown.INSTANCE;
        }

        RsElement element = boundElement.element();
        RsGenericDeclaration genericDecl = null;
        if (element instanceof RsStructItem) {
            genericDecl = (RsStructItem) element;
        }
        else if (element instanceof RsEnumVariant) {
            genericDecl = RsEnumVariantUtil.getParentEnum((RsEnumVariant) element);
        }

        Substitution typeParameters = genericDecl != null
            ? ctx.instantiateBounds(genericDecl)
            : SubstitutionUtil.EMPTY_SUBSTITUTION;
        unifySubst(boundElement.getSubst(), typeParameters);
        Ty expectedTy = expected.onlyHasTy(ctx);
        if (expectedTy != null) {
            unifySubst(typeParameters, expectedTy.getTypeParameterValues());
        }

        Ty declaredType;
        if (element instanceof RsStructItem) {
            declaredType = ((RsStructItem) element).getDeclaredType();
        }
        else if (element instanceof RsEnumVariant) {
            declaredType = RsEnumVariantUtil.getParentEnum((RsEnumVariant) element).getDeclaredType();
        }
        else {
            declaredType = TyUnknown.INSTANCE;
        }
        Ty type = FoldUtil.substitute(declaredType, typeParameters);

        inferStructTypeArguments(expr, typeParameters);

        // Struct update syntax `{ ..expression }`
        RsExpr baseExpr = expr.getStructLiteralBody().getExpr();
        if (baseExpr != null) {
            inferTypeCoercableTo(baseExpr, type);
        }

        return type;
    }

    /** Infers each field initializer of a struct literal against the declared type of the field it fills. */
    private void inferStructTypeArguments(@Nonnull RsStructLiteral literal, @Nonnull Substitution typeParameters) {
        for (RsStructLiteralField field : literal.getStructLiteralBody().getStructLiteralFieldList()) {
            Ty fieldTy = FoldUtil.substitute(declaredTypeOf(field), typeParameters);
            RsExpr expr = field.getExpr();
            if (expr != null) {
                inferTypeCoercableTo(expr, fieldTy);
            }
            else {
                RsPatBinding binding = RsStructLiteralFieldUtil.resolveToBinding(field);
                Ty bindingTy = binding != null ? ctx.getBindingType(binding) : TyUnknown.INSTANCE;
                coerce(field, bindingTy, fieldTy);
            }
        }
    }

    /** The type declared for the field a struct-literal field fills. */
    @Nonnull
    private static Ty declaredTypeOf(@Nonnull RsStructLiteralField field) {
        RsFieldDecl declaration = RsStructLiteralFieldUtil.resolveToDeclaration(field);
        RsTypeReference typeReference = declaration != null ? declaration.getTypeReference() : null;
        return typeReference != null ? ExtensionsUtil.getRawType(typeReference) : TyUnknown.INSTANCE;
    }

    @Nonnull
    private Ty inferTupleExprType(@Nonnull RsTupleExpr expr, @Nonnull Expectation expected) {
        List<Ty> types = new ArrayList<>();
        for (RsExpr e : expr.getExprList()) {
            types.add(inferExprType(e));
        }
        return new TyTuple(types);
    }

    @Nonnull
    private Ty inferCastExprType(@Nonnull RsCastExpr expr) {
        inferExprType(expr.getExpr());
        return normalizeAssociatedTypesIn(ExtensionsUtil.getRawType(expr.getTypeReference()));
    }

    @Nonnull
    private Ty inferCallExprType(@Nonnull RsCallExpr expr, @Nonnull Expectation expected) {
        RsExpr calleeExpr = expr.getExpr();
        Ty baseTy = resolveTypeVarsWithObligations(inferExprType(calleeExpr));

        // The callee may need dereferencing before it is callable, e.g. a `&fn()` or a `Box<dyn Fn()>`
        Autoderef coercionSequence = getLookup().coercionSequence(baseTy);
        TyWithObligations<TyFunctionBase> derefTy = null;
        for (Ty ty : coercionSequence) {
            TyWithObligations<TyFunctionBase> fnTy = getLookup().asTyFunction(ty);
            if (fnTy != null) derefTy = fnTy;
        }
        getFulfill().registerPredicateObligations(coercionSequence.obligations());
        ctx.applyAdjustments(calleeExpr, new ArrayList<>(Autoderef.toAdjustments(coercionSequence.steps(), getItems())));

        List<RsExpr> argExprs = expr.getValueArgumentList().getExprList();
        TyFunctionBase rawCalleeType = derefTy != null ? register(derefTy) : unknownTyFunction(argExprs.size());
        Ty normalized = ctx.resolveTypeVarsIfPossible(rawCalleeType.foldWith(associatedTypeNormalizer));
        if (!(normalized instanceof TyFunctionBase)) return TyUnknown.INSTANCE;
        TyFunctionBase calleeType = (TyFunctionBase) normalized;

        List<Ty> expectedInputTys =
            expectedInputsForExpectedOutput(expected, calleeType.getRetType(), calleeType.getParamTypes());
        inferArgumentTypes(calleeType.getParamTypes(), expectedInputTys, argExprs);
        return calleeType.getRetType();
    }

    @Nonnull
    private Ty inferDotExprType(@Nonnull RsDotExpr expr, @Nonnull Expectation expected) {
        Ty receiver = resolveTypeVarsWithObligations(inferExprType(expr.getExpr()));
        RsMethodCall methodCall = expr.getMethodCall();
        if (methodCall != null) {
            return inferMethodCallExprType(receiver, methodCall, expected);
        }
        RsFieldLookup fieldLookup = expr.getFieldLookup();
        if (fieldLookup != null) {
            return inferFieldExprType(receiver, fieldLookup);
        }
        return TyUnknown.INSTANCE;
    }

    /**
     * Resolves the called method against the receiver type, records the receiver adjustments and the
     * resolved substitution, infers the argument types and returns the method's return type.
     */
    @Nonnull
    private Ty inferMethodCallExprType(@Nonnull Ty receiver, @Nonnull RsMethodCall methodCall, @Nonnull Expectation expected) {
        List<RsExpr> argExprs = methodCall.getValueArgumentList().getExprList();

        List<MethodResolveVariant> variants =
            RsMethodCallReferenceImpl.resolveMethodCallReferenceWithReceiverType(getLookup(), receiver, methodCall);
        MethodPick picked = pickSingleMethod(receiver, variants, methodCall);
        // If the ambiguity could not be resolved, record every possible method
        ctx.writeResolvedMethod(methodCall, picked != null
            ? Collections.singletonList(picked.toMethodResolveVariant())
            : variants);

        MethodPick callee = picked;
        if (callee == null && !variants.isEmpty()) {
            callee = MethodPick.from(variants.get(0));
        }
        if (callee == null) {
            TyFunctionPointer unknownFn = unknownTyFunction(argExprs.size());
            inferArgumentTypes(unknownFn.getParamTypes(), unknownFn.getParamTypes(), argExprs);
            return unknownFn.getRetType();
        }

        getFulfill().registerPredicateObligations(callee.getObligations());

        List<Adjustment> adjustments = new ArrayList<>(Autoderef.toAdjustments(callee.getDerefSteps(), getItems()));
        Ty lastDerefTy = adjustments.isEmpty() ? receiver : adjustments.get(adjustments.size() - 1).getTarget();
        MethodPick.AutorefOrPtrAdjustment autorefOrPtr = callee.getAutorefOrPtrAdjustment();
        if (autorefOrPtr instanceof MethodPick.AutorefOrPtrAdjustment.Autoref) {
            MethodPick.AutorefOrPtrAdjustment.Autoref autoref = (MethodPick.AutorefOrPtrAdjustment.Autoref) autorefOrPtr;
            Mutability mutability = autoref.getMutability();
            adjustments.add(new Adjustment.BorrowReference(new TyReference(lastDerefTy, mutability)));
            if (autoref.isUnsize() && lastDerefTy instanceof TyArray) {
                Ty unsizedTy = new TySlice(((TyArray) lastDerefTy).getBase());
                adjustments.add(new Adjustment.Unsize(new TyReference(unsizedTy, mutability)));
            }
        }
        RsExpr receiverExpr = RsMethodCallUtil.getReceiver(methodCall);
        if (receiverExpr != null) {
            ctx.applyAdjustments(receiverExpr, adjustments);
        }

        RsFunction method = callee.getElement();
        List<RsConstParameter> constParameters = RsGenericDeclarationUtil.getConstParameters(method);
        List<RsElement> constArguments = RsMethodOrPathUtil.getConstArguments(methodCall);
        inferConstArgumentTypes(constParameters, constArguments);

        Substitution newSubst = ctx.instantiateMethodOwnerSubstitution(callee, methodCall);
        newSubst = ctx.instantiateBounds(method, callee.getFormalSelfTy(), newSubst);

        Map<TyTypeParameter, Ty> typeSubst = new HashMap<>();
        List<RsTypeParameter> typeParameters = RsGenericDeclarationUtil.getTypeParameters(method);
        List<RsTypeReference> typeArguments = RsMethodOrPathUtil.getTypeArguments(methodCall);
        for (int i = 0; i < typeParameters.size() && i < typeArguments.size(); i++) {
            typeSubst.put(TyTypeParameter.named(typeParameters.get(i)), ExtensionsUtil.getRawType(typeArguments.get(i)));
        }

        Map<CtConstParameter, Const> constSubst = new HashMap<>();
        PathExprResolver resolver = PathExprResolver.fromContext(ctx);
        for (int i = 0; i < constParameters.size() && i < constArguments.size(); i++) {
            RsConstParameter parameter = constParameters.get(i);
            RsTypeReference parameterTypeRef = parameter.getTypeReference();
            Ty expectedTy = parameterTypeRef != null
                ? normalizeAssociatedTypesIn(ExtensionsUtil.getRawType(parameterTypeRef))
                : TyUnknown.INSTANCE;
            constSubst.put(new CtConstParameter(parameter),
                ConstExprEvaluator.toConst(constArguments.get(i), expectedTy, resolver));
        }

        unifySubst(new Substitution(typeSubst, Collections.emptyMap(), constSubst), newSubst);

        Ty substitutedTy = FoldUtil
            .substitute(RsTypeInferenceWalkerHelper.getFunctionType(method), newSubst)
            .foldWith(associatedTypeNormalizer);
        if (!(substitutedTy instanceof TyFunctionBase)) return TyUnknown.INSTANCE;
        TyFunctionBase methodType = (TyFunctionBase) substitutedTy;

        // The first parameter type is `self`, which has no corresponding value argument
        List<Ty> paramTypes = methodType.getParamTypes();
        List<Ty> formalInputTys = paramTypes.isEmpty()
            ? Collections.emptyList()
            : new ArrayList<>(paramTypes.subList(1, paramTypes.size()));
        List<Ty> expectedInputTys = RsFunctionUtil.isAsync(method)
            ? Collections.emptyList()
            : expectedInputsForExpectedOutput(expected, methodType.getRetType(), formalInputTys);
        inferArgumentTypes(formalInputTys, expectedInputTys, argExprs);
        ctx.writeResolvedMethodSubst(methodCall, newSubst, methodType);

        return methodType.getRetType();
    }

    /**
     * Picks the method whose {@code self} type matches the receiver after the fewest dereferences,
     * optionally taking a reference to it (autoref).
     */
    @Nullable
    private MethodPick pickSingleMethod(
        @Nonnull Ty receiver,
        @Nonnull List<MethodResolveVariant> variants,
        @Nonnull RsMethodCall methodCall
    ) {
        List<MethodResolveVariant> list = filterAssocItems(variants, methodCall);

        TypeInferenceMarks.MethodPickDerefOrder.hit();

        Autoderef autoderef = getLookup().coercionSequence(receiver);
        List<MethodPick> picked = Collections.emptyList();
        for (Ty ty : autoderef) {
            List<MethodPick> byValue = pickByMethodSelfTy(list, autoderef, ty, null);
            if (!byValue.isEmpty()) {
                picked = byValue;
                break;
            }
            List<MethodPick> byRef =
                pickByMethodSelfTy(list, autoderef, new TyReference(ty, Mutability.IMMUTABLE), Mutability.IMMUTABLE);
            if (!byRef.isEmpty()) {
                picked = byRef;
                break;
            }
            List<MethodPick> byMutRef =
                pickByMethodSelfTy(list, autoderef, new TyReference(ty, Mutability.MUTABLE), Mutability.MUTABLE);
            if (!byMutRef.isEmpty()) {
                picked = byMutRef;
                break;
            }
        }
        if (picked.isEmpty() && list.size() == 1) {
            picked = Collections.singletonList(MethodPick.from(list.get(0)));
        }

        if (picked.isEmpty()) return null;
        if (picked.size() == 1) return picked.get(0);

        // Collapse several methods of the same trait into the single method declared in that trait, e.g.
        //
        //     trait Foo<T> { fn foo(&self, _: T) {} }
        //     impl Foo<Bar> for S { fn foo(&self, _: Bar) {} }
        //     impl Foo<Baz> for S { fn foo(&self, _: Baz) {} }
        //
        // The specific impl is selected later, by the type of the method parameter.
        List<RsFunction> elements = new ArrayList<>(picked.size());
        for (MethodPick pick : picked) {
            elements.add(pick.getElement());
        }
        RsFunction collapsed = collapseToTrait(elements);
        if (collapsed == null) return null;
        RsAbstractableOwner owner = collapsed.getOwner();
        if (!(owner instanceof RsAbstractableOwner.Trait)) return null;
        TypeInferenceMarks.MethodPickCollapseTraits.hit();
        MethodPick first = picked.get(0);
        return new MethodPick(
            collapsed,
            first.getFormalSelfTy(),
            first.getMethodSelfTy(),
            first.getDerefCount(),
            new TraitImplSource.Collapsed(((RsAbstractableOwner.Trait) owner).getTrait()),
            first.getDerefSteps(),
            first.getAutorefOrPtrAdjustment(),
            first.isValid(),
            Collections.emptyList()
        );
    }

    /** The methods of {@code list} whose {@code self} parameter has exactly the type {@code methodSelfTy}. */
    @Nonnull
    private List<MethodPick> pickByMethodSelfTy(
        @Nonnull List<MethodResolveVariant> list,
        @Nonnull Autoderef autoderef,
        @Nonnull Ty methodSelfTy,
        @Nullable Mutability borrow
    ) {
        List<MethodResolveVariant> matching = new ArrayList<>();
        for (MethodResolveVariant variant : list) {
            RsSelfParameter selfParameter = RsFunctionUtil.getSelfParameter(variant.getElement());
            if (selfParameter == null) continue;
            Ty selfTy = variant.getSelfTy();
            if (selfTy == null) continue;
            if (methodSelfTy.equals(RsTypeInferenceWalkerHelper.getTypeOfSelfParameterValue(selfParameter, selfTy))) {
                matching.add(variant);
            }
        }
        if (matching.isEmpty()) return Collections.emptyList();

        List<Autoderef.AutoderefStep> derefSteps = autoderef.steps();
        MethodPick.AutorefOrPtrAdjustment adjustment = null;
        if (borrow != null) {
            Autoderef.AutoderefStep last = derefSteps.isEmpty() ? null : derefSteps.get(derefSteps.size() - 1);
            boolean unsize = last != null && last.getKind(getItems()) == Autoderef.AutoderefKind.ArrayToSlice;
            adjustment = new MethodPick.AutorefOrPtrAdjustment.Autoref(borrow, unsize);
        }
        List<Obligation> obligations = autoderef.obligations();

        List<MethodPick> result = new ArrayList<>(matching.size());
        for (MethodResolveVariant variant : matching) {
            result.add(MethodPick.from(variant, methodSelfTy, derefSteps, adjustment, obligations));
        }
        return result;
    }

    /**
     * Narrows a list of candidate associated items down to the ones that are actually applicable:
     * declared by a trait that is in scope, visible from the call site, and whose bounds can be met.
     * Each step is skipped once a single candidate remains.
     */
    @Nonnull
    private <T extends AssocItemScopeEntryBase<?>> List<T> filterAssocItems(@Nonnull List<T> variants, @Nonnull RsElement context) {
        RsMod containingMod = RsElementUtil.getContainingMod(context);

        List<T> inScope = CollectionsUtil.singleOrLet(variants, list -> {
            TypeInferenceMarks.MethodPickTraitScope.hit();
            Map<RsTraitItem, List<T>> traitToCallee = new LinkedHashMap<>();
            List<T> filtered = new ArrayList<>();
            for (T callee : list) {
                RsTraitItem trait = callee.getSource().getRequiredTraitInScope();
                if (trait != null) {
                    traitToCallee.computeIfAbsent(trait, k -> new ArrayList<>()).add(callee);
                }
                else {
                    filtered.add(callee); // inherent impl
                }
            }
            for (RsTraitItem trait : RsElementUtil.filterInScope(new ArrayList<>(traitToCallee.keySet()), context)) {
                filtered.addAll(traitToCallee.get(trait));
            }
            if (filtered.isEmpty()) {
                TypeInferenceMarks.MethodPickTraitsOutOfScope.hit();
                return list;
            }
            return filtered;
        });

        List<T> visible = CollectionsUtil.singleOrFilter(inScope, callee -> {
            TraitImplSource source = callee.getSource();
            if (!(source instanceof TraitImplSource.ExplicitImpl) || !source.isInherent()) return true;
            RsAbstractable element = callee.getElement();
            return containingMod == null
                || !(element instanceof RsVisible)
                || RsVisibilityUtil.isVisibleFrom((RsVisible) element, containingMod);
        });

        return CollectionsUtil.singleOrFilter(visible, callee -> {
            TypeInferenceMarks.MethodPickCheckBounds.hit();
            Ty selfTy = callee.getSelfTy();
            return selfTy == null || ctx.canEvaluateBounds(callee.getSource(), selfTy);
        });
    }

    /**
     * When every candidate is the same method of the same trait, implemented several times, returns that
     * trait's declaration of the method. Otherwise {@code null}.
     */
    @Nullable
    private RsFunction collapseToTrait(@Nonnull List<RsFunction> elements) {
        if (elements.size() <= 1) return null;

        List<RsTraitItem> traits = new ArrayList<>(elements.size());
        for (RsFunction element : elements) {
            RsAbstractableOwner owner = element.getOwner();
            if (owner instanceof RsAbstractableOwner.Impl) {
                RsTraitRef traitRef = ((RsAbstractableOwner.Impl) owner).getImpl().getTraitRef();
                RsTraitItem trait = traitRef != null ? RsTraitRefUtil.resolveToTrait(traitRef) : null;
                if (trait != null) traits.add(trait);
            }
            else if (owner instanceof RsAbstractableOwner.Trait) {
                traits.add(((RsAbstractableOwner.Trait) owner).getTrait());
            }
        }
        if (traits.size() != elements.size() || new HashSet<>(traits).size() != 1) return null;

        String fnName = elements.get(0).getName();
        if (fnName == null) return null;
        RsFunction result = null;
        for (RsAbstractable member : RsTraitOrImplUtil.getExpandedMembers(traits.get(0))) {
            if (member instanceof RsFunction && fnName.equals(member.getName())) {
                if (result != null) return null;
                result = (RsFunction) member;
            }
        }
        return result;
    }

    /** Equates the explicitly written generic arguments with the inferred ones. */
    private void unifySubst(@Nonnull Substitution subst1, @Nonnull Substitution subst2) {
        for (Map.Entry<TyTypeParameter, Ty> entry : subst1.getTypeSubst().entrySet()) {
            TyTypeParameter key = entry.getKey();
            Ty value1 = entry.getValue();
            Ty value2 = subst2.get(key);
            if (value2 == null) continue;
            if (!key.equals(value1)
                && !key.equals(TyTypeParameter.self())
                && !(value1 instanceof TyTypeParameter)
                && !(value1 instanceof TyUnknown)) {
                ctx.combineTypes(value2, value1);
            }
        }
        for (Map.Entry<CtConstParameter, Const> entry : subst1.getConstSubst().entrySet()) {
            CtConstParameter key = entry.getKey();
            Const const1 = entry.getValue();
            Const const2 = subst2.get(key);
            if (const2 == null) continue;
            if (!key.equals(const1) && !(const1 instanceof CtConstParameter) && !(const1 instanceof CtUnknown)) {
                ctx.combineConsts(const2, const1);
            }
        }
    }

    /**
     * The argument types implied by the expected result type of the call, used as a hint while inferring
     * the actual arguments. Empty when the expected result type does not constrain them.
     */
    @Nonnull
    private List<Ty> expectedInputsForExpectedOutput(
        @Nonnull Expectation expectedRet,
        @Nonnull Ty formalRet,
        @Nonnull List<Ty> formalArgs
    ) {
        Ty resolvedFormalRet = resolveTypeVarsWithObligations(formalRet);
        Ty retTy = expectedRet.onlyHasTy(ctx);
        if (retTy == null) return Collections.emptyList();
        return ctx.probe(() -> {
            if (!ctx.combineTypes(retTy, resolvedFormalRet).isOk()) return Collections.<Ty>emptyList();
            List<Ty> result = new ArrayList<>(formalArgs.size());
            for (Ty formalArg : formalArgs) {
                result.add(ctx.resolveTypeVarsIfPossible(formalArg));
            }
            return result;
        });
    }

    @Nonnull
    private TyFunctionPointer unknownTyFunction(int arity) {
        List<Ty> paramTypes = new ArrayList<>(arity);
        for (int i = 0; i < arity; i++) {
            paramTypes.add(TyUnknown.INSTANCE);
        }
        return new TyFunctionPointer(new FnSig(paramTypes, TyUnknown.INSTANCE));
    }


    @Nonnull
    private Ty inferFieldExprType(@Nonnull Ty receiver, @Nonnull RsFieldLookup fieldLookup) {
        var variants = RsMethodCallReferenceImpl.resolveFieldLookupReferenceWithReceiverType(getLookup(), receiver, fieldLookup);
        ctx.writeResolvedField(fieldLookup, variants.stream().map(v -> v.getElement()).collect(Collectors.toList()));
        var field = variants.isEmpty() ? null : variants.get(0);
        if (field == null) return TyUnknown.INSTANCE;
        RsElement fieldElement = field.getElement();
        if (fieldElement instanceof RsFieldDecl fieldDecl) {
            var typeRef = fieldDecl.getTypeReference();
            if (typeRef == null) return TyUnknown.INSTANCE;
            return FoldUtil.substitute(ExtensionsUtil.getRawType(typeRef), field.getSelfTy().getTypeParameterValues());
        }
        return TyUnknown.INSTANCE;
    }

    @Nonnull
    private Ty inferLitExprType(@Nonnull RsLitExpr expr, @Nonnull Expectation expected) {
        RsStubLiteralKind stubKind = RsLitExprUtil.getStubKind(expr);
        if (stubKind instanceof RsStubLiteralKind.Boolean) return TyBool.INSTANCE;
        if (stubKind instanceof RsStubLiteralKind.Char) {
            return ((RsStubLiteralKind.Char) stubKind).isByte() ? TyInteger.U8.INSTANCE : TyChar.INSTANCE;
        }
        if (stubKind instanceof RsStubLiteralKind.StringLiteral str) {
            if (str.isByte()) return new TyReference(new TyArray(TyInteger.U8.INSTANCE, CtUnknown.INSTANCE), Mutability.IMMUTABLE, ReStatic.INSTANCE);
            return new TyReference(TyStr.INSTANCE, Mutability.IMMUTABLE, ReStatic.INSTANCE);
        }
        if (stubKind instanceof RsStubLiteralKind.Integer intLit) {
            String suffix = intLit.getSuffix();
            if (suffix != null && TyInteger.NAMES.contains(suffix)) {
                return integerFromSuffix(suffix);
            }
            Ty expectedTy = expected.tyAsNullable(ctx);
            if (expectedTy instanceof TyInteger) return expectedTy;
            return new TyInfer.IntVar();
        }
        if (stubKind instanceof RsStubLiteralKind.Float floatLit) {
            String suffix = floatLit.getSuffix();
            if ("f32".equals(suffix)) return TyFloat.F32.INSTANCE;
            if ("f64".equals(suffix)) return TyFloat.F64.INSTANCE;
            Ty expectedTy = expected.tyAsNullable(ctx);
            if (expectedTy instanceof TyFloat) return expectedTy;
            return new TyInfer.FloatVar();
        }
        return TyUnknown.INSTANCE;
    }

    @Nonnull
    private static TyInteger integerFromSuffix(@Nonnull String suffix) {
        switch (suffix) {
            case "i8": return TyInteger.I8.INSTANCE;
            case "i16": return TyInteger.I16.INSTANCE;
            case "i32": return TyInteger.I32.INSTANCE;
            case "i64": return TyInteger.I64.INSTANCE;
            case "i128": return TyInteger.I128.INSTANCE;
            case "isize": return TyInteger.ISize.INSTANCE;
            case "u8": return TyInteger.U8.INSTANCE;
            case "u16": return TyInteger.U16.INSTANCE;
            case "u32": return TyInteger.U32.INSTANCE;
            case "u64": return TyInteger.U64.INSTANCE;
            case "u128": return TyInteger.U128.INSTANCE;
            case "usize": return TyInteger.USize.INSTANCE;
            default: return TyInteger.I32.INSTANCE;
        }
    }

    @Nonnull
    private Ty inferBlockExprType(@Nonnull RsBlockExpr blockExpr, @Nonnull Expectation expected) {
        return inferBlockType(blockExpr.getBlock(), expected, false);
    }

    @Nonnull
    private Ty inferIfExprType(@Nonnull RsIfExpr expr, @Nonnull Expectation expected) {
        RsCondition condition = expr.getCondition();
        if (condition != null && condition.getExpr() != null) {
            inferExprCoercableTo(condition.getExpr(), TyBool.INSTANCE);
        }
        Ty blockTy = expr.getBlock() != null ? inferBlockType(expr.getBlock(), expected, false) : null;
        RsElseBranch elseBranch = expr.getElseBranch();
        if (elseBranch != null) {
            if (elseBranch.getIfExpr() != null) inferExprType(elseBranch.getIfExpr(), expected);
            if (elseBranch.getBlock() != null) inferBlockType(elseBranch.getBlock(), expected, false);
        }
        return expr.getElseBranch() == null ? TyUnit.INSTANCE : (blockTy != null ? blockTy : TyUnknown.INSTANCE);
    }

    @Nonnull
    private Ty inferLoopExprType(@Nonnull RsLoopExpr expr) {
        if (expr.getBlock() != null) {
            inferBlockType(expr.getBlock(), Expectation.NoExpectation, false);
        }
        return TyNever.INSTANCE;
    }

    @Nonnull
    private Ty inferWhileExprType(@Nonnull RsWhileExpr expr) {
        RsCondition condition = expr.getCondition();
        if (condition != null && condition.getExpr() != null) {
            inferExprCoercableTo(condition.getExpr(), TyBool.INSTANCE);
        }
        if (expr.getBlock() != null) {
            inferBlockType(expr.getBlock(), Expectation.NoExpectation, false);
        }
        return TyUnit.INSTANCE;
    }

    @Nonnull
    private Ty inferForExprType(@Nonnull RsForExpr expr) {
        Ty exprTy = resolveTypeVarsWithObligations(expr.getExpr() != null ? inferExprType(expr.getExpr()) : TyUnknown.INSTANCE);
        TyWithObligations<Ty> itemTyObl = getLookup().findIteratorItemType(exprTy);
        Ty itemTy = itemTyObl != null ? resolveTypeVarsWithObligations(getFulfill().register(itemTyObl)) : TyUnknown.INSTANCE;
        if (expr.getPat() != null) PatternMatchingUtil.extractBindings(expr.getPat(), this, itemTy);
        if (expr.getBlock() != null) inferBlockType(expr.getBlock(), Expectation.NoExpectation, false);
        return TyUnit.INSTANCE;
    }

    @Nonnull
    private Ty inferMatchExprType(@Nonnull RsMatchExpr expr, @Nonnull Expectation expected) {
        Ty matchingExprTy = resolveTypeVarsWithObligations(expr.getExpr() != null ? inferExprType(expr.getExpr()) : TyUnknown.INSTANCE);
        List<RsMatchArm> arms = RsMatchExprUtil.getArms(expr);
        for (RsMatchArm arm : arms) {
            PatternMatchingUtil.extractBindings(arm.getPat(), this, matchingExprTy);
            if (arm.getExpr() != null) inferExprType(arm.getExpr(), expected);
            if (arm.getMatchArmGuard() != null && arm.getMatchArmGuard().getExpr() != null) {
                inferExprCoercableTo(arm.getMatchArmGuard().getExpr(), TyBool.INSTANCE);
            }
        }
        List<Ty> armTypes = new ArrayList<>();
        for (RsMatchArm arm : arms) {
            if (arm.getExpr() != null) armTypes.add(ctx.getExprType(arm.getExpr()));
        }
        return getMoreCompleteType(armTypes);
    }

    @Nonnull
    private Ty inferUnaryExprType(@Nonnull RsUnaryExpr expr, @Nonnull Expectation expected) {
        RsExpr innerExpr = expr.getExpr();
        if (innerExpr == null) return TyUnknown.INSTANCE;
        return innerExpr instanceof RsExpr ? inferExprType(innerExpr, expected) : TyUnknown.INSTANCE;
    }

    @Nonnull
    private Ty inferBinaryExprType(@Nonnull RsBinaryExpr expr) {
        BinaryOperator op = RsBinaryExprUtil.getOperatorType(expr);
        Ty lhsType = resolveTypeVarsWithObligations(inferExprType(expr.getLeft()));
        RsExpr right = expr.getRight();

        Ty rhsType;
        Ty retTy;
        if (op instanceof BoolOp) {
            if (op instanceof OverloadableBinaryOperator) {
                Ty rhsTypeVar = new TyInfer.TyVar();
                enforceOverloadedBinopTypes(lhsType, rhsTypeVar, (OverloadableBinaryOperator) op);
                rhsType = resolveTypeVarsWithObligations(
                    right != null ? inferTypeCoercableTo(right, rhsTypeVar) : TyUnknown.INSTANCE);
                ctx.applyAdjustment(expr.getLeft(),
                    new Adjustment.BorrowReference(new TyReference(lhsType, Mutability.IMMUTABLE)));
                if (right != null) {
                    ctx.applyAdjustment(right,
                        new Adjustment.BorrowReference(new TyReference(rhsType, Mutability.IMMUTABLE)));
                }
            }
            else {
                rhsType = resolveTypeVarsWithObligations(
                    right != null ? inferTypeCoercableTo(right, lhsType) : TyUnknown.INSTANCE);
            }
            retTy = TyBool.INSTANCE;
        }
        else if (op instanceof ArithmeticOp) {
            Ty rhsTypeVar = new TyInfer.TyVar();
            TyWithObligations<Ty> output =
                getLookup().findArithmeticBinaryExprOutputType(lhsType, rhsTypeVar, (ArithmeticOp) op);
            retTy = output != null ? register(output) : TyUnknown.INSTANCE;
            rhsType = resolveTypeVarsWithObligations(
                right != null ? inferTypeCoercableTo(right, rhsTypeVar) : TyUnknown.INSTANCE);
        }
        else if (op instanceof ArithmeticAssignmentOp) {
            Ty rhsTypeVar = new TyInfer.TyVar();
            enforceOverloadedBinopTypes(lhsType, rhsTypeVar, (OverloadableBinaryOperator) op);
            rhsType = resolveTypeVarsWithObligations(
                right != null ? inferTypeCoercableTo(right, rhsTypeVar) : TyUnknown.INSTANCE);
            ctx.applyAdjustment(expr.getLeft(),
                new Adjustment.BorrowReference(new TyReference(lhsType, Mutability.MUTABLE)));
            retTy = TyUnit.INSTANCE;
        }
        else {
            rhsType = right != null ? inferTypeCoercableTo(right, lhsType) : TyUnknown.INSTANCE;
            retTy = TyUnit.INSTANCE;
        }

        if (op != AssignmentOp.EQ && isBuiltinBinop(lhsType, rhsType, op)) {
            Ty builtinRetTy = enforceBuiltinBinopTypes(lhsType, rhsType, op);
            if (!(op instanceof ArithmeticAssignmentOp)) {
                ctx.combineTypes(builtinRetTy, retTy);
            }
        }

        return retTy;
    }

    /** Requires the left operand to implement the trait the operator desugars to, e.g. `a + b` needs `Add`. */
    private void enforceOverloadedBinopTypes(@Nonnull Ty lhsType, @Nonnull Ty rhsType, @Nonnull OverloadableBinaryOperator op) {
        RsTraitItem trait = op.findTrait(getItems());
        if (trait == null) return;
        TraitRef traitRef = new TraitRef(lhsType, new BoundElement<>(trait).withSubst(rhsType));
        getFulfill().registerPredicateObligation(new Obligation(new Predicate.Trait(traitRef)));
    }

    /** Whether the operator applies to these operand types without going through a trait implementation. */
    private static boolean isBuiltinBinop(@Nonnull Ty lhsType, @Nonnull Ty rhsType, @Nonnull BinaryOperator op) {
        BinOpCategory category = binOpCategory(op);
        if (category == null) return false;
        switch (category) {
            case Shortcircuit:
                return true;
            case Shift:
                return TyUtil.isIntegral(lhsType) && TyUtil.isIntegral(rhsType);
            case Math:
                return TyUtil.isIntegral(lhsType) && TyUtil.isIntegral(rhsType)
                    || TyUtil.isFloat(lhsType) && TyUtil.isFloat(rhsType);
            case Bitwise:
                return TyUtil.isIntegral(lhsType) && TyUtil.isIntegral(rhsType)
                    || TyUtil.isFloat(lhsType) && TyUtil.isFloat(rhsType)
                    || lhsType instanceof TyBool && rhsType instanceof TyBool;
            case Comparison:
                return TyUtil.isScalar(lhsType) && TyUtil.isScalar(rhsType);
            default:
                return false;
        }
    }

    /** Equates the operand types as the builtin operator requires, and returns the type it produces. */
    @Nonnull
    private Ty enforceBuiltinBinopTypes(@Nonnull Ty lhsType, @Nonnull Ty rhsType, @Nonnull BinaryOperator op) {
        BinOpCategory category = binOpCategory(op);
        if (category == null) return TyUnknown.INSTANCE;
        switch (category) {
            case Shortcircuit:
                ctx.combineTypes(lhsType, TyBool.INSTANCE);
                ctx.combineTypes(rhsType, TyBool.INSTANCE);
                return TyBool.INSTANCE;
            case Shift:
                return lhsType;
            case Math:
            case Bitwise:
                ctx.combineTypes(lhsType, rhsType);
                return lhsType;
            case Comparison:
                ctx.combineTypes(lhsType, rhsType);
                return TyBool.INSTANCE;
            default:
                return TyUnknown.INSTANCE;
        }
    }

    /**
     * How the operator behaves with respect to the builtin operations, or {@code null} for plain assignment,
     * which has no builtin behaviour of its own.
     */
    @Nullable
    private static BinOpCategory binOpCategory(@Nonnull BinaryOperator op) {
        BinaryOperator effective = op instanceof ArithmeticAssignmentOp
            ? nonAssignEquivalent((ArithmeticAssignmentOp) op)
            : op;
        if (effective instanceof ArithmeticOp) {
            if (effective == ArithmeticOp.SHL || effective == ArithmeticOp.SHR) return BinOpCategory.Shift;
            if (effective == ArithmeticOp.BIT_AND || effective == ArithmeticOp.BIT_OR || effective == ArithmeticOp.BIT_XOR) {
                return BinOpCategory.Bitwise;
            }
            return BinOpCategory.Math;
        }
        if (effective instanceof LogicOp) return BinOpCategory.Shortcircuit;
        if (effective instanceof EqualityOp || effective instanceof ComparisonOp) return BinOpCategory.Comparison;
        return null;
    }

    /** The operator a compound assignment operator applies, e.g. {@code +} for {@code +=}. */
    @Nonnull
    private static BinaryOperator nonAssignEquivalent(@Nonnull ArithmeticAssignmentOp op) {
        if (op == ArithmeticAssignmentOp.ANDEQ) return LogicOp.AND;
        if (op == ArithmeticAssignmentOp.OREQ) return LogicOp.OR;
        if (op == ArithmeticAssignmentOp.PLUSEQ) return ArithmeticOp.ADD;
        if (op == ArithmeticAssignmentOp.MINUSEQ) return ArithmeticOp.SUB;
        if (op == ArithmeticAssignmentOp.MULEQ) return ArithmeticOp.MUL;
        if (op == ArithmeticAssignmentOp.DIVEQ) return ArithmeticOp.DIV;
        if (op == ArithmeticAssignmentOp.REMEQ) return ArithmeticOp.REM;
        if (op == ArithmeticAssignmentOp.XOREQ) return ArithmeticOp.BIT_XOR;
        if (op == ArithmeticAssignmentOp.GTGTEQ) return ArithmeticOp.SHR;
        return ArithmeticOp.SHL;
    }

    /** Registers the obligations that came with a normalized type and returns the type itself. */
    @Nonnull
    private <T> T register(@Nonnull TyWithObligations<T> tyWithObligations) {
        for (Obligation obligation : tyWithObligations.getObligations()) {
            getFulfill().registerPredicateObligation(obligation);
        }
        return tyWithObligations.getValue();
    }

    @Nonnull
    private Ty inferTryExprType(@Nonnull RsTryExpr expr) {
        Ty base = resolveTypeVarsWithObligations(inferExprType(expr.getExpr()));
        if (base instanceof TyAdt adt) {
            if (adt.getItem() == getItems().getResult() || adt.getItem() == getItems().getOption()) {
                TypeInferenceMarks.QuestionOperator.hit();
                return adt.getTypeArguments().isEmpty() ? TyUnknown.INSTANCE : adt.getTypeArguments().get(0);
            }
        }
        return TyUnknown.INSTANCE;
    }

    @Nonnull
    private Ty inferArrayType(@Nonnull RsArrayExpr expr, @Nonnull Expectation expected) {
        if (expr.getSemicolon() != null) {
            RsExpr init = RsArrayExprUtil.getInitializer(expr);
            Ty elemTy = init != null ? inferExprType(init) : TyUnknown.INSTANCE;
            RsExpr sizeExpr = RsArrayExprUtil.getSizeExpr(expr);
            if (sizeExpr != null) inferExprCoercableTo(sizeExpr, TyInteger.USize.INSTANCE);
            return new TyArray(elemTy, CtUnknown.INSTANCE);
        } else {
            List<RsExpr> elements = RsArrayExprUtil.getArrayElements(expr);
            if (elements == null || elements.isEmpty()) return new TyArray(new TyInfer.TyVar(), CtUnknown.INSTANCE);
            List<Ty> types = new ArrayList<>();
            for (RsExpr e : elements) types.add(inferExprType(e));
            Ty elemTy = getMoreCompleteType(types);
            long size = elements.size();
            Const sizeConst = ConstExpr.toConst(new ConstExpr.Value.Integer(size, TyInteger.USize.INSTANCE));
            return new TyArray(elemTy, sizeConst);
        }
    }

    @Nonnull
    private Ty inferRangeType(@Nonnull RsRangeExpr expr) {
        for (RsExpr e : expr.getExprList()) inferExprType(e);
        return TyUnknown.INSTANCE;
    }

    @Nonnull
    private Ty inferIndexExprType(@Nonnull RsIndexExpr expr) {
        RsExpr containerExpr = RsIndexExprUtil.getContainerExpr(expr);
        RsExpr indexExpr = RsIndexExprUtil.getIndexExpr(expr);
        if (indexExpr == null) {
            inferExprType(containerExpr);
            return TyUnknown.INSTANCE;
        }

        Ty containerType = inferExprType(containerExpr);
        Ty indexType = ctx.resolveTypeVarsIfPossible(inferExprType(indexExpr));

        Autoderef autoderef = getLookup().coercionSequence(containerType);
        Ty result = null;
        for (Ty ty : autoderef) {
            TyWithObligations<Ty> output = getLookup().findIndexOutputType(ty, indexType);
            if (output != null) {
                Ty registered = register(output);
                result = registered instanceof TyUnknown ? null : registered;
                break;
            }
        }
        if (result == null) return TyUnknown.INSTANCE;

        getFulfill().registerPredicateObligations(autoderef.obligations());

        List<Autoderef.AutoderefStep> steps = autoderef.steps();
        Autoderef.AutoderefStep lastStep = steps.isEmpty() ? null : steps.get(steps.size() - 1);
        Ty adjustedTy = lastStep != null ? lastStep.getTo() : containerType;
        List<Adjustment> adjustments = new ArrayList<>(Autoderef.toAdjustments(steps, getItems()));
        if (!isBuiltinIndex(adjustedTy, ctx.resolveTypeVarsIfPossible(indexType))) {
            adjustments.add(new Adjustment.BorrowReference(new TyReference(adjustedTy, Mutability.IMMUTABLE)));
            if (lastStep != null && lastStep.getKind(getItems()) == Autoderef.AutoderefKind.ArrayToSlice) {
                adjustments.add(new Adjustment.Unsize(new TyReference(lastStep.getTo(), Mutability.IMMUTABLE)));
            }
            ctx.writeOverloadedOperator(expr);
        }
        ctx.applyAdjustments(containerExpr, adjustments);
        return result;
    }

    /** Indexing an array or slice by a {@code usize} is a builtin operation, not a call to {@code Index}. */
    private static boolean isBuiltinIndex(@Nonnull Ty baseType, @Nonnull Ty indexType) {
        return (baseType instanceof TyArray || baseType instanceof TySlice) && indexType instanceof TyInteger.USize;
    }

    @Nonnull
    private Ty inferMacroExprType(@Nonnull RsMacroExpr macroExpr, @Nonnull Expectation expected) {
        RsMacroCall macroCall = macroExpr.getMacroCall();
        inferChildExprsRecursively(macroCall);
        MacroExpansion expansion = RsMacroCallUtil.getExpansion(macroCall);
        if (expansion instanceof MacroExpansion.Expr) {
            return inferExprType(((MacroExpansion.Expr) expansion).getExpr(), expected);
        }
        return TyUnknown.INSTANCE;
    }

    @Nonnull
    private Ty inferLambdaExprType(@Nonnull RsLambdaExpr expr, @Nonnull Expectation expected) {
        List<RsValueParameter> params = expr.getValueParameters();
        List<Ty> paramTypes = new ArrayList<>();
        for (RsValueParameter param : params) {
            Ty paramTy = param.getTypeReference() != null
                ? normalizeAssociatedTypesIn(ExtensionsUtil.getRawType(param.getTypeReference()))
                : new TyInfer.TyVar();
            if (param.getPat() != null) PatternMatchingUtil.extractBindings(param.getPat(), this, paramTy);
            paramTypes.add(paramTy);
        }
        Ty retTy = expr.getRetType() != null && expr.getRetType().getTypeReference() != null
            ? normalizeAssociatedTypesIn(ExtensionsUtil.getRawType(expr.getRetType().getTypeReference()))
            : new TyInfer.TyVar();
        RsTypeInferenceWalker lambdaCtx = new RsTypeInferenceWalker(ctx, retTy);
        if (expr.getExpr() != null) lambdaCtx.inferLambdaBody(expr.getExpr());
        return new TyClosure(expr, new FnSig(paramTypes, retTy));
    }

    @Nonnull
    private Ty inferYieldExprType(@Nonnull RsYieldExpr expr) {
        if (yieldTy == null) {
            yieldTy = expr.getExpr() != null ? inferExprType(expr.getExpr()) : TyUnit.INSTANCE;
        } else {
            if (expr.getExpr() != null) inferExprCoercableTo(expr.getExpr(), yieldTy);
        }
        return TyUnit.INSTANCE;
    }

    @Nonnull
    private Ty inferRetExprType(@Nonnull RsRetExpr expr) {
        if (expr.getExpr() != null) inferExprCoercableTo(expr.getExpr(), returnTy);
        return TyNever.INSTANCE;
    }

    @Nonnull
    private Ty inferBreakExprType(@Nonnull RsBreakExpr expr) {
        if (expr.getExpr() != null) inferExprType(expr.getExpr());
        return TyNever.INSTANCE;
    }

    @Nonnull
    private Ty inferLetExprType(@Nonnull RsLetExpr letExpr) {
        Ty exprTy = letExpr.getExpr() != null ? inferExprType(letExpr.getExpr()) : null;
        if (exprTy != null) exprTy = resolveTypeVarsWithObligations(exprTy);
        if (letExpr.getPat() != null) {
            PatternMatchingUtil.extractBindings(letExpr.getPat(), this, exprTy != null ? exprTy : TyUnknown.INSTANCE);
        }
        return TyBool.INSTANCE;
    }

    private void inferArgumentTypes(@Nonnull List<Ty> formalInputTys, @Nonnull List<Ty> expectedInputTys, @Nonnull List<RsExpr> argExprs) {
        for (int i = 0; i < argExprs.size(); i++) {
            Ty formalInputTy = i < formalInputTys.size() ? formalInputTys.get(i) : TyUnknown.INSTANCE;
            Ty expectedInputTy = i < expectedInputTys.size() ? expectedInputTys.get(i) : formalInputTy;
            Ty inferred = inferExprType(argExprs.get(i), Expectation.rvalueHint(expectedInputTy));
            Ty coercedTy = resolveTypeVarsWithObligations(expectedInputTy);
            coerce(argExprs.get(i), inferred, coercedTy);
            ctx.combineTypes(formalInputTy, coercedTy);
        }
    }

    public void inferConstArgumentTypes(@Nonnull List<RsConstParameter> constParameters, @Nonnull List<RsElement> constArguments) {
        for (int i = 0; i < constArguments.size() && i < constParameters.size(); i++) {
            Ty type = constParameters.get(i).getTypeReference() != null
                ? normalizeAssociatedTypesIn(ExtensionsUtil.getRawType(constParameters.get(i).getTypeReference()))
                : TyUnknown.INSTANCE;
            RsElement arg = constArguments.get(i);
            if (arg instanceof RsExpr) {
                inferExprCoercableTo((RsExpr) arg, type);
            }
        }
    }

    private void inferChildExprsRecursively(@Nonnull PsiElement psi) {
        if (psi instanceof RsExpr) {
            inferExprType((RsExpr) psi);
        } else {
            PsiExtUtil.forEachChild(psi, this::inferChildExprsRecursively);
        }
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    private <T extends TypeFoldable<T>> T normalizeAssociatedTypesIn(@Nonnull T ty) {
        TyWithObligations<T> result = ctx.normalizeAssociatedTypesIn(ty);
        for (Obligation obligation : result.getObligations()) {
            getFulfill().registerPredicateObligation(obligation);
        }
        return result.getValue();
    }

    @Nonnull
    private Ty getMoreCompleteType(@Nonnull List<Ty> types) {
        if (types.isEmpty()) return TyUnknown.INSTANCE;
        Ty result = types.get(0);
        for (int i = 1; i < types.size(); i++) {
            result = getMoreCompleteType(result, types.get(i));
        }
        return result;
    }

    @Nonnull
    private Ty getMoreCompleteType(@Nonnull Ty ty1, @Nonnull Ty ty2) {
        if (ty1 instanceof TyNever) return ty2;
        if (ty2 instanceof TyNever) return ty1;
        if (ty1 instanceof TyUnknown) return ty2 instanceof TyNever ? TyUnknown.INSTANCE : ty2;
        ctx.combineTypes(ty1, ty2);
        return ty1;
    }

    public void extractParameterBindings(@Nonnull RsFunction fn) {
        for (RsValueParameter param : fn.getValueParameters()) {
            if (param.getPat() != null) {
                Ty ty = param.getTypeReference() != null
                    ? normalizeAssociatedTypesIn(ExtensionsUtil.getRawType(param.getTypeReference()))
                    : TyUnknown.INSTANCE;
                PatternMatchingUtil.extractBindings(param.getPat(), this, ty);
            }
        }
    }

    public void writePatTy(@Nonnull RsPat psi, @Nonnull Ty ty) {
        ctx.writePatTy(psi, ty);
    }

    public void writePatFieldTy(@Nonnull RsPatField psi, @Nonnull Ty ty) {
        ctx.writePatFieldTy(psi, ty);
    }

    @Nonnull
    public List<ResolvedPath> getResolvedPath(@Nonnull RsPathExpr expr) {
        return ctx.getResolvedPath(expr);
    }

    // Ignored types and consts for error reporting
    public static final List<Class<?>> IGNORED_TYS = List.of(
        TyUnknown.class,
        TyInfer.TyVar.class,
        TyTypeParameter.class,
        TyProjection.class,
        TyTraitObject.class,
        TyAnon.class
    );

    public static final List<Class<?>> IGNORED_CONSTS = List.of(
        CtUnknown.class,
        CtInferVar.class
    );
}

/**
 * Helper utilities for RsTypeInferenceWalker.
 */
final class RsTypeInferenceWalkerHelper {
    private RsTypeInferenceWalkerHelper() {
    }

    @Nonnull
    public static TyFunctionDef getFunctionType(@Nonnull RsFunction fn) {
        RsCallable callable = new RsCallable.Function(fn);
        return new TyFunctionDef(callable, FnSig.of(callable));
    }

    @Nullable
    public static Ty getSelfType(@Nonnull RsFunction fn) {
        RsAbstractableOwner owner = fn.getOwner();
        if (owner instanceof RsAbstractableOwner.Impl impl) {
            return ExtensionsUtil.getSelfType(impl.getImpl());
        }
        if (owner instanceof RsAbstractableOwner.Trait trait) {
            return ExtensionsUtil.getSelfType(trait.getTrait());
        }
        return null;
    }

    @Nonnull
    public static Ty getTypeOfSelfParameter(@Nonnull RsSelfParameter self) {
        Ty selfType = getSelfType(RsSelfParameterUtil.getParentFunction(self));
        if (selfType == null) selfType = TyUnknown.INSTANCE;
        return getTypeOfSelfParameterValue(self, selfType);
    }

    @Nonnull
    public static Ty getTypeOfSelfParameterValue(@Nonnull RsSelfParameter self, @Nonnull Ty selfType) {
        if (RsSelfParameterUtil.isRef(self)) {
            return new TyReference(selfType, RsSelfParameterUtil.getMutability(self));
        }
        return selfType;
    }
}

/**
 * RsPat extension for extractBindings.
 */
// In Java, we delegate to PatternMatching.extractBindings.
