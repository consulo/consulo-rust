/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.dfa.ControlFlowGraph;
import org.rust.lang.core.dfa.MemoryCategorization;
import org.rust.lang.core.dfa.borrowck.BorrowChecker;
import org.rust.lang.core.dfa.liveness.Liveness;
import org.rust.lang.core.mir.MirExtensions;
import org.rust.lang.core.mir.borrowck.FacadeBorrowck;
import org.rust.lang.core.mir.borrowck.MirBorrowCheckResult;
import org.rust.lang.core.mir.schemas.MirBody;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.infer.*;
import org.rust.lang.core.types.regions.RegionScopeTreeUtil;
import org.rust.lang.core.types.regions.ScopeTree;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTypeParameter;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.Collections;
import java.util.List;

/**
 * Contains extension-like static methods for Rust PSI type operations.
 */
public final class ExtensionsUtil {
    private ExtensionsUtil() {
    }

    @NotNull
    public static Substitution emptySubstitution() {
        return SubstitutionUtil.emptySubstitution();
    }

    private static final Key<CachedValue<RsInferenceResult>> TYPE_INFERENCE_KEY =
        Key.create("TYPE_INFERENCE_KEY");

    private static final Key<CachedValue<BorrowChecker.BorrowCheckResult>> BORROW_CHECKER_KEY =
        Key.create("BORROW_CHECKER_KEY");

    private static final Key<CachedValue<MirBorrowCheckResult>> MIR_BORROW_CHECKER_KEY =
        Key.create("MIR_BORROW_CHECKER_KEY");

    private static final Key<CachedValue<ControlFlowGraph>> CONTROL_FLOW_KEY =
        Key.create("CONTROL_FLOW_KEY");

    private static final Key<CachedValue<Liveness.LivenessResult>> LIVENESS_KEY =
        Key.create("LIVENESS_KEY");

    // ---- RsTypeReference extensions ----

    /**
     * A type of the type reference without normalization of normalizable associated type projections.
     *
     * @see TyLowering#lowerTypeReference(RsTypeReference)
     */
    @NotNull
    public static Ty getRawType(@NotNull RsTypeReference typeRef) {
        return TyLowering.lowerTypeReference(typeRef);
    }

    /**
     * A type of the type reference WITH normalization of normalizable associated type projections.
     */
    @NotNull
    public static Ty getNormType(@NotNull RsTypeReference typeRef) {
        Ty rawType = getRawType(typeRef);
        if (FoldUtil.hasTyProjection(rawType)) {
            return getImplLookup(typeRef).getCtx().fullyNormalizeAssociatedTypesIn(rawType);
        } else {
            return rawType;
        }
    }

    @NotNull
    public static Ty normType(@NotNull RsTypeReference typeRef, @NotNull ImplLookup implLookup) {
        return normType(typeRef, implLookup.getCtx());
    }

    @NotNull
    public static Ty normType(@NotNull RsTypeReference typeRef, @NotNull RsInferenceContext ctx) {
        return ctx.fullyNormalizeAssociatedTypesIn(getRawType(typeRef));
    }

    public static boolean isLifetimeElidable(@NotNull RsTypeReference typeRef) {
        PsiElement ownerElement = RsTypeReferenceUtil.getOwner(typeRef);
        if (!(ownerElement instanceof RsTypeReference)) return false;
        RsTypeReference owner = (RsTypeReference) ownerElement;
        PsiElement typeOwner = owner.getParent();

        boolean isAssociatedConstant = typeOwner instanceof RsConstant
            && RsAbstractableUtil.getOwner((RsConstant) typeOwner).isImplOrTrait();

        return !(typeOwner instanceof RsNamedFieldDecl)
            && !(typeOwner instanceof RsTupleFieldDecl)
            && !(typeOwner instanceof RsTypeAlias)
            && !isAssociatedConstant;
    }

    // ---- RsInferenceContextOwner extensions ----

    @NotNull
    public static RsInferenceResult getSelfInferenceResult(@NotNull RsInferenceContextOwner owner) {
        if (owner instanceof RsPath) {
            PsiElement parent = owner.getParent();
            if (parent != null && !isAllowedPathParent(parent)) {
                return RsInferenceResult.EMPTY;
            }
        }
        return CachedValuesManager.getCachedValue(owner, TYPE_INFERENCE_KEY, () -> {
            RsInferenceResult inferred = TypeInference.inferTypesIn(owner);
            return RsInferenceContextOwnerUtil.createCachedResult(owner, inferred);
        });
    }

    @Nullable
    public static RsInferenceContextOwner getInferenceContextOwner(@NotNull PsiElement element) {
        PsiElement current = element;
        while (current != null) {
            PsiElement next = current instanceof RsFile ? null : current.getContext();
            if (current instanceof RsInferenceContextOwner) {
                if (!(current instanceof RsPath) || (next != null && isAllowedPathParent(next))) {
                    return (RsInferenceContextOwner) current;
                }
            }
            current = next;
        }
        return null;
    }

    private static boolean isAllowedPathParent(@NotNull PsiElement element) {
        return element instanceof RsPathType
            || element instanceof RsTraitRef
            || element instanceof RsStructLiteral
            || element instanceof RsAssocTypeBinding
            || element instanceof RsPath;
    }

    @Nullable
    public static RsInferenceResult getInference(@NotNull PsiElement element) {
        RsInferenceContextOwner owner = getInferenceContextOwner(element);
        return owner != null ? getSelfInferenceResult(owner) : null;
    }

    @Nullable
    public static BorrowChecker.BorrowCheckResult getBorrowCheckResult(@NotNull RsInferenceContextOwner owner) {
        return CachedValuesManager.getCachedValue(owner, BORROW_CHECKER_KEY, () -> {
            if (!RsElementUtil.existsAfterExpansion(owner)) {
                return RsInferenceContextOwnerUtil.createCachedResult(owner, null);
            }
            BorrowChecker.BorrowCheckContext bccx = BorrowChecker.BorrowCheckContext.buildFor(owner);
            BorrowChecker.BorrowCheckResult borrowCheckResult = bccx != null ? bccx.check() : null;
            return RsInferenceContextOwnerUtil.createCachedResult(owner, borrowCheckResult);
        });
    }

    @Nullable
    public static MirBorrowCheckResult getMirBorrowCheckResult(@NotNull RsInferenceContextOwner owner) {
        return CachedValuesManager.getCachedValue(owner, MIR_BORROW_CHECKER_KEY, () -> {
            if (!RsElementUtil.existsAfterExpansion(owner)) {
                return RsInferenceContextOwnerUtil.createCachedResult(owner, null);
            }
            MirBody mirBody = MirExtensions.getMirBody(owner);
            if (mirBody == null) {
                return RsInferenceContextOwnerUtil.createCachedResult(owner, null);
            }
            MirBorrowCheckResult result = FacadeBorrowck.doMirBorrowCheck(mirBody);
            return RsInferenceContextOwnerUtil.createCachedResult(owner, result);
        });
    }

    @Nullable
    public static ControlFlowGraph getControlFlowGraph(@NotNull RsInferenceContextOwner owner) {
        return CachedValuesManager.getCachedValue(owner, CONTROL_FLOW_KEY, () -> {
            if (!RsElementUtil.existsAfterExpansion(owner)) {
                return RsInferenceContextOwnerUtil.createCachedResult(owner, null);
            }
            ScopeTree regionScopeTree = RegionScopeTreeUtil.getRegionScopeTree(owner);
            RsElement body = RsInferenceContextOwnerUtil.getBody(owner);
            ControlFlowGraph cfg = body instanceof RsBlock
                ? ControlFlowGraph.buildFor((RsBlock) body, regionScopeTree)
                : null;
            return RsInferenceContextOwnerUtil.createCachedResult(owner, cfg);
        });
    }

    @Nullable
    public static Liveness.LivenessResult getLiveness(@NotNull RsInferenceContextOwner owner) {
        return CachedValuesManager.getCachedValue(owner, LIVENESS_KEY, () -> {
            if (!RsElementUtil.existsAfterExpansion(owner)) {
                return RsInferenceContextOwnerUtil.createCachedResult(owner, null);
            }
            Liveness.LivenessContext livenessContext = Liveness.LivenessContext.buildFor(owner);
            Liveness.LivenessResult livenessResult = livenessContext != null ? livenessContext.check() : null;
            return RsInferenceContextOwnerUtil.createCachedResult(owner, livenessResult);
        });
    }

    // ---- RsPatBinding extensions ----

    @NotNull
    public static Ty getType(@NotNull RsPatBinding binding) {
        RsInferenceResult inference = getInference(binding);
        return inference != null ? inference.getBindingType(binding) : TyUnknown.INSTANCE;
    }

    // ---- RsPat extensions ----

    @NotNull
    public static Ty getType(@NotNull RsPat pat) {
        RsInferenceResult inference = getInference(pat);
        return inference != null ? inference.getPatType(pat) : TyUnknown.INSTANCE;
    }

    // ---- RsPatField extensions ----

    @NotNull
    public static Ty getType(@NotNull RsPatField patField) {
        RsInferenceResult inference = getInference(patField);
        return inference != null ? inference.getPatFieldType(patField) : TyUnknown.INSTANCE;
    }

    // ---- RsExpr extensions ----

    @NotNull
    public static Ty getType(@NotNull RsExpr expr) {
        RsInferenceResult inference = getInference(expr);
        return inference != null ? inference.getExprType(expr) : TyUnknown.INSTANCE;
    }

    @NotNull
    public static Ty getAdjustedType(@NotNull RsExpr expr) {
        List<Adjustment> adjustments = getAdjustments(expr);
        if (!adjustments.isEmpty()) {
            return adjustments.get(adjustments.size() - 1).getTarget();
        }
        return getType(expr);
    }

    @NotNull
    public static List<Adjustment> getAdjustments(@NotNull RsExpr expr) {
        RsInferenceResult inference = getInference(expr);
        return inference != null ? inference.getExprAdjustments(expr) : Collections.emptyList();
    }

    @Nullable
    public static Ty getExpectedType(@NotNull RsExpr expr) {
        ExpectedType expected = getExpectedTypeCoercable(expr);
        return expected != null ? expected.getTy() : null;
    }

    @Nullable
    public static ExpectedType getExpectedTypeCoercable(@NotNull RsExpr expr) {
        RsInferenceResult inference = getInference(expr);
        return inference != null ? inference.getExpectedExprType(expr) : null;
    }

    @Nullable
    public static RsElement getDeclaration(@NotNull RsExpr expr) {
        if (expr instanceof RsPathExpr) {
            RsPathExpr pathExpr = (RsPathExpr) expr;
            return pathExpr.getPath().getReference() != null
                ? (RsElement) pathExpr.getPath().getReference().resolve()
                : null;
        }
        if (expr instanceof RsDotExpr) {
            return getDeclaration(((RsDotExpr) expr).getExpr());
        }
        if (expr instanceof RsCallExpr) {
            return getDeclaration(((RsCallExpr) expr).getExpr());
        }
        if (expr instanceof RsIndexExpr) {
            return getDeclaration(RsIndexExprUtil.getContainerExpr((RsIndexExpr) expr));
        }
        if (expr instanceof RsStructLiteral) {
            RsStructLiteral structLiteral = (RsStructLiteral) expr;
            return structLiteral.getPath().getReference() != null
                ? (RsElement) structLiteral.getPath().getReference().resolve()
                : null;
        }
        return null;
    }

    // ---- RsStructLiteralField extensions ----

    @NotNull
    public static Ty getType(@NotNull RsStructLiteralField field) {
        if (RsStructLiteralFieldUtil.isShorthand(field)) {
            RsPatBinding binding = RsStructLiteralFieldUtil.resolveToBinding(field);
            return binding != null ? getType(binding) : TyUnknown.INSTANCE;
        } else {
            RsExpr expr = field.getExpr();
            return expr != null ? getType(expr) : TyUnknown.INSTANCE;
        }
    }

    @NotNull
    public static List<Adjustment> getAdjustments(@NotNull RsStructLiteralField field) {
        RsInferenceResult inference = getInference(field);
        return inference != null ? inference.getExprAdjustments(field) : Collections.emptyList();
    }

    // ---- RsTraitOrImpl extensions ----

    @NotNull
    public static Ty getSelfType(@NotNull RsTraitOrImpl traitOrImpl) {
        if (traitOrImpl instanceof RsImplItem) {
            RsTypeReference typeRef = ((RsImplItem) traitOrImpl).getTypeReference();
            return typeRef != null ? getRawType(typeRef) : TyUnknown.INSTANCE;
        }
        if (traitOrImpl instanceof RsTraitItem) {
            return TyTypeParameter.self(traitOrImpl);
        }
        throw new IllegalStateException("Unreachable");
    }

    // ---- RsElement extensions ----

    @NotNull
    public static ImplLookup getImplLookup(@NotNull RsElement element) {
        return ImplLookup.relativeTo(element);
    }

    // ---- RsExpr Cmt / mutability extensions ----

    @Nullable
    public static MemoryCategorization.Cmt getCmt(@NotNull RsExpr expr) {
        ImplLookup lookup = getImplLookup(expr);
        RsInferenceResult inference = getInference(expr);
        if (inference == null) return null;
        return new MemoryCategorization.MemoryCategorizationContext(lookup, inference).processExpr(expr);
    }

    public static boolean isMutable(@NotNull RsExpr expr) {
        if (getType(expr) instanceof TyUnknown) return false;
        MemoryCategorization.Cmt cmt = getCmt(expr);
        return cmt != null && cmt.isMutable();
    }

    public static boolean isImmutable(@NotNull RsExpr expr) {
        if (getType(expr) instanceof TyUnknown) return false;
        MemoryCategorization.Cmt cmt = getCmt(expr);
        return cmt != null && !cmt.isMutable();
    }

    // ---- RsNamedElement extensions ----

    @NotNull
    public static Ty asTy(@Nullable RsNamedElement element) {
        if (element instanceof RsTypeDeclarationElement) {
            return ((RsTypeDeclarationElement) element).getDeclaredType();
        }
        return TyUnknown.INSTANCE;
    }

    @NotNull
    public static Ty asTy(@Nullable RsNamedElement element, @NotNull Ty... subst) {
        if (!(element instanceof RsTypeDeclarationElement)) return TyUnknown.INSTANCE;
        Ty declaredType = ((RsTypeDeclarationElement) element).getDeclaredType();
        if (element instanceof RsGenericDeclaration) {
            return FoldUtil.substitute(declaredType,
                RsGenericDeclarationUtil.withSubst((RsGenericDeclaration) element, subst).getSubst());
        } else {
            return declaredType;
        }
    }
}
