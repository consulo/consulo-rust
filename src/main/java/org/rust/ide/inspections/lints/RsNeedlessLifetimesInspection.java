/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.ElideLifetimesFix;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.regions.ReEarlyBound;
import org.rust.lang.core.types.regions.ReStatic;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTraitObject;
import org.rust.openapiext.PsiElementExtUtil;
import org.rust.stdext.CollectionExtUtil;

import java.util.*;
import org.rust.lang.core.psi.ext.RsSelfParameterUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsTypeParameterUtil;
import org.rust.lang.core.psi.ext.RsLifetimeParameterUtil;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsRefLikeTypeUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.PsiElementUtil;

/**
 * Checks for lifetime annotations which can be removed by relying on lifetime elision.
 * Corresponds to needless_lifetimes lint from Rust Clippy.
 */
public class RsNeedlessLifetimesInspection extends RsLintInspection {

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.NeedlessLifetimes;
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @SuppressWarnings("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            @Override
            public void visitFunction2(@NotNull RsFunction fn) {
                if (couldUseElision(fn)) {
                    registerProblem(holder, fn);
                }
            }
        };
    }

    private void registerProblem(@NotNull RsProblemsHolder holder, @NotNull RsFunction fn) {
        PsiElement fnKeyword = fn.getFn();
        PsiElement block = RsFunctionUtil.getBlock(fn);
        int endOffset;
        if (block != null) {
            PsiElement prevNonComment = PsiElementUtil.getPrevNonCommentSibling(block);
            endOffset = prevNonComment != null ? RsPsiElementExtUtil.getEndOffsetInParent(prevNonComment) : RsPsiElementExtUtil.getEndOffsetInParent(fn.getIdentifier());
        } else {
            endOffset = RsPsiElementExtUtil.getEndOffsetInParent(fn.getIdentifier());
        }

        registerLintProblem(
            holder,
            fn,
            RsBundle.message("inspection.message.explicit.lifetimes.given.in.parameter.types.where.they.could.be.elided"),
            new TextRange(fnKeyword.getStartOffsetInParent(), endOffset),
            RsLintHighlightingType.WEAK_WARNING,
            Collections.singletonList(new ElideLifetimesFix(fn))
        );
    }

    /**
     * There are two scenarios where elision works:
     * - no output references, all input references have different LT
     * - output references, exactly one input reference with same LT
     * All lifetimes must be unnamed, 'static or defined without bounds on the level of the current item.
     * Note: async fn syntax does not allow lifetime elision outside of & and &mut references.
     */
    private static boolean couldUseElision(@NotNull RsFunction fn) {
        if (hasWhereLifetimes(fn.getWhereClause())) return false;

        for (RsTypeParameter tp : fn.getTypeParameters()) {
            for (RsPolybound tpb : RsTypeParameterUtil.getBounds(tp)) {
                if (hasNamedReferenceLifetime(tpb.getBound())) return false;
            }
        }

        LifetimesCollector inputCollector = new LifetimesCollector(true);
        LifetimesCollector outputCollector = new LifetimesCollector(false);
        if (!collectLifetimesFromFnSignature(fn, inputCollector, outputCollector)) return false;

        List<ReferenceLifetime> inputLifetimes = inputCollector.myLifetimes;
        List<ReferenceLifetime> outputLifetimes = outputCollector.myLifetimes;

        if (RsFunctionUtil.isAsync(fn) && (inputCollector.myHasLifetimeOutsideRef || outputCollector.myHasLifetimeOutsideRef)) {
            return false;
        }

        // no input lifetimes? easy case!
        if (inputLifetimes.isEmpty()) return false;

        if (checkLifetimesUsedInBody(RsFunctionUtil.getBlock(fn))) return false;

        // check for lifetimes from higher scopes
        Set<ReferenceLifetime> allowedLifetimes = allowedLifetimesFrom(fn.getLifetimeParameters());
        for (ReferenceLifetime lt : inputLifetimes) {
            if (!allowedLifetimes.contains(lt)) return false;
        }
        for (ReferenceLifetime lt : outputLifetimes) {
            if (!allowedLifetimes.contains(lt)) return false;
        }

        Set<ReferenceLifetime> distinctInput = new LinkedHashSet<>(inputLifetimes);
        boolean areInputsDistinct = inputLifetimes.size() == distinctInput.size();

        // no output lifetimes, check distinctness of input lifetimes
        if (outputLifetimes.isEmpty()) {
            boolean hasNamed = false;
            for (ReferenceLifetime lt : inputLifetimes) {
                if (lt instanceof NamedLifetime) {
                    hasNamed = true;
                    break;
                }
            }
            // only unnamed and static, ok
            if (!hasNamed) return false;
            // we have no output reference, so we only need all distinct lifetimes
            return areInputsDistinct;
        }

        Set<ReferenceLifetime> distinctOutput = new LinkedHashSet<>(outputLifetimes);
        if (distinctOutput.size() > 1) return false;

        RsSelfParameter selfParam = fn.getSelfParameter();
        boolean selfIsRefLike = selfParam != null && isRefLike(selfParam);
        if (inputLifetimes.size() == 1 || (selfIsRefLike && areInputsDistinct)) {
            ReferenceLifetime input = inputLifetimes.get(0);
            ReferenceLifetime output = outputLifetimes.get(0);
            if (input instanceof NamedLifetime && output instanceof NamedLifetime
                && ((NamedLifetime) input).myName.equals(((NamedLifetime) output).myName)) {
                return true;
            }
            if (input instanceof NamedLifetime && output == UnnamedLifetime.INSTANCE) {
                return true;
            }
            if (input == UnnamedLifetime.INSTANCE && output == UnnamedLifetime.INSTANCE) {
                boolean anyNamed = false;
                for (ReferenceLifetime lt : inputLifetimes) {
                    if (lt instanceof NamedLifetime) {
                        anyNamed = true;
                        break;
                    }
                }
                return anyNamed;
            }
            return false;
        }
        return false;
    }

    /**
     * Includes:
     * - &self and &mut self
     * - self: &Self and self: &mut Self
     * - self: Box<&Self>, self: Rc<&Self>, self: Arc<&Self>, self: Pin<&Self>
     * - self: Rc<Box<&Self>> and other combinations
     */
    public static boolean isRefLike(@NotNull RsSelfParameter self) {
        if (RsSelfParameterUtil.isRef(self)) return true;
        RsTypeReference typeReference = self.getTypeReference();
        if (typeReference == null) return false;
        for (RsRefLikeType refLikeType : RsElementUtil.descendantsOfTypeOrSelf(typeReference, RsRefLikeType.class)) {
            if (refLikeType.getAnd() != null) return true;
        }
        return false;
    }

    public static boolean hasMissingLifetimes(@NotNull RsFunction fn) {
        if (fn.getRetType() == null) return false;
        RsSelfParameter selfParam = fn.getSelfParameter();
        if (selfParam != null && isRefLike(selfParam)) return false;
        LifetimesCollector inputCollector = new LifetimesCollector(true);
        LifetimesCollector outputCollector = new LifetimesCollector(false);
        if (!collectLifetimesFromFnSignature(fn, inputCollector, outputCollector)) return false;
        boolean hasUnnamedOutput = false;
        for (ReferenceLifetime lt : outputCollector.myLifetimes) {
            if (lt == UnnamedLifetime.INSTANCE) {
                hasUnnamedOutput = true;
                break;
            }
        }
        return hasUnnamedOutput && inputCollector.myLifetimes.size() != 1;
    }

    private static boolean collectLifetimesFromFnSignature(
        @NotNull RsFunction fn,
        @NotNull LifetimesCollector inputCollector,
        @NotNull LifetimesCollector outputCollector
    ) {
        RsValueParameterList paramList = fn.getValueParameterList();
        if (paramList != null) {
            RsSelfParameter selfParam = paramList.getSelfParameter();
            if (selfParam != null) selfParam.accept(inputCollector);
            for (RsValueParameter param : paramList.getValueParameterList()) {
                RsTypeReference typeRef = param.getTypeReference();
                if (typeRef != null) typeRef.accept(inputCollector);
            }
        }
        if (inputCollector.myAbort) return false;

        RsRetType retType = fn.getRetType();
        if (retType != null) {
            RsTypeReference typeRef = retType.getTypeReference();
            if (typeRef != null) typeRef.accept(outputCollector);
        }
        if (outputCollector.myAbort) return false;
        return true;
    }

    @NotNull
    private static Set<ReferenceLifetime> allowedLifetimesFrom(@NotNull List<RsLifetimeParameter> lifetimeParameters) {
        Set<ReferenceLifetime> allowed = new HashSet<>();
        for (RsLifetimeParameter lp : lifetimeParameters) {
            if (RsLifetimeParameterUtil.getBounds(lp).isEmpty()) {
                String name = lp.getName();
                if (name != null) {
                    allowed.add(new NamedLifetime(name));
                }
            }
        }
        allowed.add(UnnamedLifetime.INSTANCE);
        allowed.add(StaticLifetime.INSTANCE);
        return allowed;
    }

    /** Are any lifetimes mentioned in the where clause? If yes, we don't try to reason about elision. */
    private static boolean hasWhereLifetimes(@Nullable RsWhereClause whereClause) {
        if (whereClause == null) return false;
        for (RsWherePred predicate : whereClause.getWherePredList()) {
            if (predicate.getLifetime() != null) return true;

            LifetimesCollector collector = new LifetimesCollector(false);
            RsTypeReference typeRef = predicate.getTypeReference();
            if (typeRef != null) typeRef.accept(collector);
            if (!collector.myLifetimes.isEmpty()) return true;

            RsForLifetimes forLifetimes = predicate.getForLifetimes();
            List<RsLifetimeParameter> boundLifetimeParams = forLifetimes != null ? forLifetimes.getLifetimeParameterList() : Collections.emptyList();
            Set<ReferenceLifetime> allowedLifetimes = allowedLifetimesFrom(boundLifetimeParams);

            RsTypeParamBounds bounds = predicate.getTypeParamBounds();
            if (bounds != null) {
                for (RsPolybound polybound : bounds.getPolyboundList()) {
                    polybound.getBound().accept(collector);
                }
            }
            if (!allowedLifetimes.containsAll(collector.myLifetimes)) return true;
        }
        return false;
    }

    private static boolean hasNamedReferenceLifetime(@NotNull RsBound bound) {
        LifetimesCollector collector = new LifetimesCollector(false);
        bound.accept(collector);
        for (ReferenceLifetime lt : collector.myLifetimes) {
            if (lt instanceof NamedLifetime) return true;
        }
        return false;
    }

    /** Returns true if a lifetime is used in the body */
    private static boolean checkLifetimesUsedInBody(@Nullable RsBlock body) {
        if (body == null) return false;
        BodyLifetimeChecker checker = new BodyLifetimeChecker();
        body.accept(checker);
        return checker.myLifetimesUsedInBody;
    }

    // ---- Reference lifetime types ----

    private static abstract class ReferenceLifetime {
    }

    private static final class UnnamedLifetime extends ReferenceLifetime {
        static final UnnamedLifetime INSTANCE = new UnnamedLifetime();

        @Override
        public boolean equals(Object o) {
            return o instanceof UnnamedLifetime;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    private static final class StaticLifetime extends ReferenceLifetime {
        static final StaticLifetime INSTANCE = new StaticLifetime();

        @Override
        public boolean equals(Object o) {
            return o instanceof StaticLifetime;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    private static final class NamedLifetime extends ReferenceLifetime {
        final String myName;

        NamedLifetime(@NotNull String name) {
            myName = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NamedLifetime)) return false;
            return myName.equals(((NamedLifetime) o).myName);
        }

        @Override
        public int hashCode() {
            return myName.hashCode();
        }
    }

    @NotNull
    private static ReferenceLifetime toReferenceLifetime(@NotNull LifetimeName name) {
        if (name instanceof LifetimeName.Parameter) {
            return new NamedLifetime(((LifetimeName.Parameter) name).getName());
        }
        if (name == LifetimeName.Static.INSTANCE) {
            return StaticLifetime.INSTANCE;
        }
        return UnnamedLifetime.INSTANCE; // Implicit and Underscore
    }

    // ---- LifetimesCollector ----

    private static class LifetimesCollector extends RsRecursiveVisitor {
        boolean myAbort = false;
        boolean myHasLifetimeOutsideRef = false;
        final List<ReferenceLifetime> myLifetimes = new ArrayList<>();
        private final boolean myIsForInputParams;

        LifetimesCollector(boolean isForInputParams) {
            myIsForInputParams = isForInputParams;
        }

        @Override
        public void visitSelfParameter(@NotNull RsSelfParameter selfParameter) {
            RsLifetime lifetime = selfParameter.getLifetime();
            if (lifetime != null) visitLifetime(lifetime);
            if (RsSelfParameterUtil.isRef(selfParameter) && isElided(selfParameter.getLifetime())) {
                record(null);
            }
            RsTypeReference typeRef = selfParameter.getTypeReference();
            if (typeRef != null) visitTypeReference(typeRef);
        }

        @Override
        public void visitRefLikeType(@NotNull RsRefLikeType refLike) {
            if (RsRefLikeTypeUtil.isRef(refLike) && isElided(refLike.getLifetime())) {
                record(null);
            }
            super.visitRefLikeType(refLike);
        }

        @Override
        public void visitTypeReference(@NotNull RsTypeReference ref) {
            Ty type = RsTypesUtil.getRawType(ref);
            if (type instanceof TyTraitObject) {
                TyTraitObject traitObj = (TyTraitObject) type;
                if (traitObj.getRegion() instanceof ReEarlyBound || traitObj.getRegion() instanceof ReStatic) {
                    myAbort = true;
                }
            }
            super.visitTypeReference(ref);
        }

        @Override
        public void visitTraitType(@NotNull RsTraitType trait) {
            for (RsPolybound polybound : trait.getPolyboundList()) {
                RsLifetime lifetime = polybound.getBound().getLifetime();
                if (lifetime != null) record(null);
                if (myIsForInputParams) {
                    myAbort = myAbort || hasNamedReferenceLifetime(polybound.getBound());
                }
            }
            super.visitTraitType(trait);
        }

        @Override
        public void visitPath(@NotNull RsPath path) {
            collectAnonymousLifetimes(path);
            super.visitPath(path);
        }

        @Override
        public void visitLifetime(@NotNull RsLifetime lifetime) {
            myHasLifetimeOutsideRef = myHasLifetimeOutsideRef || !(lifetime.getParent() instanceof RsRefLikeType);
            record(lifetime);
        }

        @Override
        public void visitElement(@NotNull RsElement element) {
            if (myAbort) return;
            if (element instanceof RsItemElement) return; // ignore nested items
            if (processFnPointerOrFnTrait(element)) return;
            PsiElementExtUtil.forEachChild(element, child -> {
                child.accept(this);
            });
        }

        private boolean processFnPointerOrFnTrait(@NotNull RsElement element) {
            boolean isFnPointer = element instanceof RsFnPointerType;
            boolean isFnTrait = element instanceof RsPath && ((RsPath) element).getValueParameterList() != null;
            if (!isFnPointer && !isFnTrait) return false;

            for (RsPath path : RsElementUtil.descendantsOfType(element, RsPath.class)) {
                collectAnonymousLifetimes(path);
                RsTypeArgumentList typeArgList = path.getTypeArgumentList();
                if (typeArgList != null) {
                    for (RsLifetime lt : typeArgList.getLifetimeList()) {
                        record(lt);
                    }
                }
            }
            return true;
        }

        private void record(@Nullable RsLifetime lifetime) {
            LifetimeName typedName = RsLifetimeUtil.getTypedName(lifetime);
            myLifetimes.add(toReferenceLifetime(typedName));
        }

        private void collectAnonymousLifetimes(@NotNull RsPath path) {
            if (!RsPathUtil.getLifetimeArguments(path).isEmpty()) return;
            PsiElement resolved = path.getReference() != null ? path.getReference().resolve() : null;
            if (resolved instanceof RsStructItem || resolved instanceof RsEnumItem
                || resolved instanceof RsTraitItem || resolved instanceof RsTypeAlias) {
                RsGenericDeclaration declaration = (RsGenericDeclaration) resolved;
                int count = declaration.getLifetimeParameters().size();
                for (int i = 0; i < count; i++) {
                    record(null);
                }
            }
        }

        private static boolean isElided(@Nullable RsLifetime lifetime) {
            return lifetime == null || RsLifetimeUtil.getTypedName(lifetime) instanceof LifetimeName.Implicit
                || RsLifetimeUtil.getTypedName(lifetime) instanceof LifetimeName.Underscore;
        }
    }

    // ---- BodyLifetimeChecker ----

    private static class BodyLifetimeChecker extends RsWithMacrosInspectionVisitor {
        boolean myLifetimesUsedInBody = false;

        @Override
        public void visitLifetime(@NotNull RsLifetime lifetime) {
            if (RsLifetimeUtil.getTypedName(lifetime) instanceof LifetimeName.Parameter) {
                myLifetimesUsedInBody = true;
            }
        }

        @Override
        public void visitElement(@NotNull RsElement element) {
            if (myLifetimesUsedInBody || element instanceof RsItemElement) return;
            PsiElementExtUtil.forEachChild(element, child -> {
                child.accept(this);
            });
        }
    }
}
