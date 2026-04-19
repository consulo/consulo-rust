/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.infer.Adjustment;
import org.rust.lang.core.types.infer.RsInferenceData;
import org.rust.lang.core.types.regions.ReStatic;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.ty.*;
import org.rust.stdext.StdextUtil;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

// ---- Categorization ----

public final class MemoryCategorization {
    private MemoryCategorization() {
    }

    // ---- Categorization sealed class ----
    public static abstract class Categorization {
        private Categorization() {
        }

        /** Temporary value */
        public static class Rvalue extends Categorization {
            @NotNull
            public final Region region;

            public Rvalue(@NotNull Region region) {
                this.region = region;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Rvalue)) return false;
                return region.equals(((Rvalue) o).region);
            }

            @Override
            public int hashCode() {
                return region.hashCode();
            }
        }

        /** Static value */
        public static final Categorization StaticItem = new Categorization() {
        };

        /** Local variable */
        public static class Local extends Categorization {
            @NotNull
            public final RsElement declaration;

            public Local(@NotNull RsElement declaration) {
                this.declaration = declaration;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Local)) return false;
                return declaration.equals(((Local) o).declaration);
            }

            @Override
            public int hashCode() {
                return declaration.hashCode();
            }
        }

        /** Dereference of a pointer */
        public static class Deref extends Categorization {
            @NotNull
            public final Cmt cmt;
            @NotNull
            public final PointerKind pointerKind;

            public Deref(@NotNull Cmt cmt, @NotNull PointerKind pointerKind) {
                this.cmt = cmt;
                this.pointerKind = pointerKind;
            }

            @NotNull
            public Cmt unwrapDerefs() {
                if (cmt.category instanceof Deref) {
                    return ((Deref) cmt.category).unwrapDerefs();
                }
                return cmt;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Deref)) return false;
                Deref deref = (Deref) o;
                return cmt.equals(deref.cmt) && pointerKind.equals(deref.pointerKind);
            }

            @Override
            public int hashCode() {
                return Objects.hash(cmt, pointerKind);
            }
        }

        /** Something reachable from the base without a pointer dereference (e.g. field) */
        public static abstract class Interior extends Categorization {
            @NotNull
            public abstract Cmt getCmt();

            public static class Field extends Interior {
                @NotNull
                private final Cmt cmt;
                @Nullable
                public final String name;

                public Field(@NotNull Cmt cmt, @Nullable String name) {
                    this.cmt = cmt;
                    this.name = name;
                }

                @NotNull
                @Override
                public Cmt getCmt() {
                    return cmt;
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (!(o instanceof Field)) return false;
                    Field field = (Field) o;
                    return cmt.equals(field.cmt) && Objects.equals(name, field.name);
                }

                @Override
                public int hashCode() {
                    return Objects.hash(cmt, name);
                }
            }

            public static class Index extends Interior {
                @NotNull
                private final Cmt cmt;

                public Index(@NotNull Cmt cmt) {
                    this.cmt = cmt;
                }

                @NotNull
                @Override
                public Cmt getCmt() {
                    return cmt;
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (!(o instanceof Index)) return false;
                    return cmt.equals(((Index) o).cmt);
                }

                @Override
                public int hashCode() {
                    return cmt.hashCode();
                }
            }

            public static class Pattern extends Interior {
                @NotNull
                private final Cmt cmt;

                public Pattern(@NotNull Cmt cmt) {
                    this.cmt = cmt;
                }

                @NotNull
                @Override
                public Cmt getCmt() {
                    return cmt;
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (!(o instanceof Pattern)) return false;
                    return cmt.equals(((Pattern) o).cmt);
                }

                @Override
                public int hashCode() {
                    return cmt.hashCode();
                }
            }
        }

        /** Selects a particular enum variant */
        public static class Downcast extends Categorization {
            @NotNull
            public final Cmt cmt;
            @NotNull
            public final RsElement element;

            public Downcast(@NotNull Cmt cmt, @NotNull RsElement element) {
                this.cmt = cmt;
                this.element = element;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Downcast)) return false;
                Downcast d = (Downcast) o;
                return cmt.equals(d.cmt) && element.equals(d.element);
            }

            @Override
            public int hashCode() {
                return Objects.hash(cmt, element);
            }
        }
    }

    // ---- BorrowKind ----
    public static abstract class BorrowKind {
        private BorrowKind() {
        }

        public static final BorrowKind ImmutableBorrow = new BorrowKind() {
        };
        public static final BorrowKind MutableBorrow = new BorrowKind() {
        };

        @NotNull
        public static BorrowKind from(@NotNull Mutability mutability) {
            return mutability == Mutability.IMMUTABLE ? ImmutableBorrow : MutableBorrow;
        }

        public static boolean isCompatible(@NotNull BorrowKind first, @NotNull BorrowKind second) {
            return first == ImmutableBorrow && second == ImmutableBorrow;
        }
    }

    // ---- PointerKind ----
    public static abstract class PointerKind {
        private PointerKind() {
        }

        public static final PointerKind Unique = new PointerKind() {
        };

        public static class BorrowedPointer extends PointerKind {
            @NotNull
            public final BorrowKind borrowKind;
            @NotNull
            public final Region region;

            public BorrowedPointer(@NotNull BorrowKind borrowKind, @NotNull Region region) {
                this.borrowKind = borrowKind;
                this.region = region;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof BorrowedPointer)) return false;
                BorrowedPointer that = (BorrowedPointer) o;
                return borrowKind == that.borrowKind && region.equals(that.region);
            }

            @Override
            public int hashCode() {
                return Objects.hash(borrowKind, region);
            }
        }

        public static class UnsafePointer extends PointerKind {
            @NotNull
            public final Mutability mutability;

            public UnsafePointer(@NotNull Mutability mutability) {
                this.mutability = mutability;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof UnsafePointer)) return false;
                return mutability == ((UnsafePointer) o).mutability;
            }

            @Override
            public int hashCode() {
                return mutability.hashCode();
            }
        }
    }

    // ---- ImmutabilityBlame ----
    public static abstract class ImmutabilityBlame {
        private ImmutabilityBlame() {
        }

        public static class LocalDeref extends ImmutabilityBlame {
            @NotNull
            public final RsElement element;

            public LocalDeref(@NotNull RsElement element) {
                this.element = element;
            }
        }

        public static final ImmutabilityBlame AdtFieldDeref = new ImmutabilityBlame() {
        };

        public static class ImmutableLocal extends ImmutabilityBlame {
            @NotNull
            public final RsElement element;

            public ImmutableLocal(@NotNull RsElement element) {
                this.element = element;
            }
        }
    }

    // ---- Aliasability ----
    public static abstract class Aliasability {
        private Aliasability() {
        }

        public static class FreelyAliasable extends Aliasability {
            @NotNull
            public final AliasableReason reason;

            public FreelyAliasable(@NotNull AliasableReason reason) {
                this.reason = reason;
            }
        }

        public static final Aliasability NonAliasable = new Aliasability() {
        };
    }

    public enum AliasableReason {
        Borrowed, Static, StaticMut
    }

    // ---- MutabilityCategory ----
    public enum MutabilityCategory {
        Immutable, Declared, Inherited;

        @NotNull
        public static MutabilityCategory from(@NotNull Mutability mutability) {
            return mutability == Mutability.IMMUTABLE ? Immutable : Declared;
        }

        @NotNull
        public static MutabilityCategory from(@NotNull BorrowKind borrowKind) {
            return borrowKind == BorrowKind.ImmutableBorrow ? Immutable : Declared;
        }

        @NotNull
        public static MutabilityCategory from(@NotNull MutabilityCategory baseMut, @NotNull PointerKind pointerKind) {
            if (pointerKind == PointerKind.Unique) return baseMut.inherit();
            if (pointerKind instanceof PointerKind.BorrowedPointer) {
                return from(((PointerKind.BorrowedPointer) pointerKind).borrowKind);
            }
            if (pointerKind instanceof PointerKind.UnsafePointer) {
                return from(((PointerKind.UnsafePointer) pointerKind).mutability);
            }
            return Immutable;
        }

        @NotNull
        public MutabilityCategory inherit() {
            return this == Immutable ? Immutable : Inherited;
        }

        public boolean isMutable() {
            return this != Immutable;
        }
    }

    // ---- Cmt ----
    public static class Cmt {
        @NotNull
        public final RsElement element;
        @Nullable
        public final Categorization category;
        @NotNull
        public final MutabilityCategory mutabilityCategory;
        @NotNull
        public final Ty ty;

        public Cmt(@NotNull RsElement element, @Nullable Categorization category,
                    @NotNull MutabilityCategory mutabilityCategory, @NotNull Ty ty) {
            this.element = element;
            this.category = category;
            this.mutabilityCategory = mutabilityCategory;
            this.ty = ty;
        }

        public Cmt(@NotNull RsElement element, @NotNull Ty ty) {
            this(element, null, MutabilityCategory.from(Mutability.DEFAULT_MUTABILITY), ty);
        }

        public boolean isMutable() {
            return mutabilityCategory.isMutable();
        }
    }

    // ---- MemoryCategorizationContext ----
    public static class MemoryCategorizationContext {
        @NotNull
        public final ImplLookup lookup;
        @NotNull
        public final RsInferenceData inference;

        public MemoryCategorizationContext(@NotNull ImplLookup lookup, @NotNull RsInferenceData inference) {
            this.lookup = lookup;
            this.inference = inference;
        }

        @NotNull
        public Cmt processExpr(@NotNull RsExpr expr) {
            List<Adjustment> adjustments = inference.getExprAdjustments(expr);
            List<Adjustment> reversed = new java.util.ArrayList<>(adjustments);
            java.util.Collections.reverse(reversed);
            return processExprAdjustedWith(expr, reversed.iterator());
        }

        @NotNull
        private Cmt processExprAdjustedWith(@NotNull RsExpr expr, @NotNull Iterator<Adjustment> adjustments) {
            Adjustment adjustment = StdextUtil.nextOrNull(adjustments);
            if (adjustment == null) {
                return processExprUnadjusted(expr);
            }
            if (adjustment instanceof Adjustment.Deref) {
                Adjustment.Deref deref = (Adjustment.Deref) adjustment;
                Cmt cmt;
                if (deref.getOverloaded() != null) {
                    TyReference ref = new TyReference(deref.getTarget(), deref.getOverloaded());
                    cmt = processRvalue(expr, ref);
                } else {
                    cmt = processExprAdjustedWith(expr, adjustments);
                }
                return processDeref(expr, cmt);
            }
            if (adjustment instanceof Adjustment.BorrowReference) {
                Adjustment.BorrowReference borrow = (Adjustment.BorrowReference) adjustment;
                Ty target = borrow.getTarget();
                Cmt unadjustedCmt = processExprUnadjusted(expr);
                if (target instanceof TyReference && ((TyReference) target).getMutability().isMut() && !unadjustedCmt.isMutable()) {
                    return processExprUnadjusted(expr);
                }
                return processRvalue(expr, target);
            }
            return processRvalue(expr, adjustment.getTarget());
        }

        @NotNull
        private Cmt processExprUnadjusted(@NotNull RsExpr expr) {
            if (expr instanceof RsUnaryExpr) return processUnaryExpr((RsUnaryExpr) expr);
            if (expr instanceof RsDotExpr) return processDotExpr((RsDotExpr) expr);
            if (expr instanceof RsIndexExpr) return processIndexExpr((RsIndexExpr) expr);
            if (expr instanceof RsPathExpr) return processPathExpr((RsPathExpr) expr);
            if (expr instanceof RsParenExpr) return processParenExpr((RsParenExpr) expr);
            return processRvalue(expr);
        }

        @NotNull
        private Cmt processUnaryExpr(@NotNull RsUnaryExpr unaryExpr) {
            if (!RsUnaryExprExtUtil.isDereference(unaryExpr)) return processRvalue(unaryExpr);
            RsExpr base = unaryExpr.getExpr();
            if (base == null) return new Cmt(unaryExpr, inference.getExprType(unaryExpr));
            if (inference.isOverloadedOperator(unaryExpr)) {
                return processOverloadedPlace(unaryExpr, base);
            }
            Cmt baseCmt = processExpr(base);
            return processDeref(unaryExpr, baseCmt);
        }

        @NotNull
        private Cmt processDotExpr(@NotNull RsDotExpr dotExpr) {
            if (dotExpr.getMethodCall() != null) return processRvalue(dotExpr);
            Ty type = inference.getExprType(dotExpr);
            RsExpr base = dotExpr.getExpr();
            Cmt baseCmt = processExpr(base);
            RsFieldLookup fieldLookup = dotExpr.getFieldLookup();
            String fieldName = null;
            if (fieldLookup != null) {
                if (fieldLookup.getIdentifier() != null) {
                    fieldName = fieldLookup.getIdentifier().getText();
                } else if (fieldLookup.getIntegerLiteral() != null) {
                    fieldName = fieldLookup.getIntegerLiteral().getText();
                }
            }
            return cmtOfField(dotExpr, baseCmt, fieldName, type);
        }

        @NotNull
        private Cmt processIndexExpr(@NotNull RsIndexExpr indexExpr) {
            if (inference.isOverloadedOperator(indexExpr)) {
                return processOverloadedPlace(indexExpr, RsIndexExprUtil.getContainerExpr(indexExpr));
            }
            Ty type = inference.getExprType(indexExpr);
            Cmt baseCmt = processExpr(RsIndexExprUtil.getContainerExpr(indexExpr));
            return new Cmt(indexExpr, new Categorization.Interior.Index(baseCmt), baseCmt.mutabilityCategory.inherit(), type);
        }

        @NotNull
        private Cmt processOverloadedPlace(@NotNull RsExpr expr, @NotNull RsExpr base) {
            Ty placeTy = inference.getExprType(expr);
            Ty baseTy = inference.getExprTypeAdjusted(base);
            if (!(baseTy instanceof TyReference)) return new Cmt(expr, placeTy);
            TyReference refTy = new TyReference(placeTy, ((TyReference) baseTy).getMutability(), ((TyReference) baseTy).getRegion());
            return processDeref(expr, processRvalue(expr, refTy));
        }

        @NotNull
        private Cmt processPathExpr(@NotNull RsPathExpr pathExpr) {
            Ty type = inference.getExprType(pathExpr);
            List<? extends org.rust.lang.core.types.infer.ResolvedPath> resolvedPaths = inference.getResolvedPath(pathExpr);
            RsElement declaration = null;
            if (resolvedPaths.size() == 1) {
                declaration = resolvedPaths.get(0).getElement();
            }
            if (declaration == null) return new Cmt(pathExpr, type);

            if (declaration instanceof RsConstant) {
                RsConstant constant = (RsConstant) declaration;
                if (!RsConstantUtil.isConst(constant)) {
                    return new Cmt(pathExpr, Categorization.StaticItem, MutabilityCategory.from(RsConstantUtil.getMutability(constant)), type);
                }
                return processRvalue(pathExpr);
            }
            if (declaration instanceof RsEnumVariant || declaration instanceof RsStructItem || declaration instanceof RsFunction) {
                return processRvalue(pathExpr);
            }
            if (declaration instanceof RsPatBinding) {
                return new Cmt(pathExpr, new Categorization.Local(declaration), MutabilityCategory.from(RsPatBindingUtil.getMutability((RsPatBinding) declaration)), type);
            }
            if (declaration instanceof RsSelfParameter) {
                return new Cmt(pathExpr, new Categorization.Local(declaration), MutabilityCategory.from(RsSelfParameterExtUtil.getMutability((RsSelfParameter) declaration)), type);
            }
            return new Cmt(pathExpr, type);
        }

        @NotNull
        private Cmt processParenExpr(@NotNull RsParenExpr parenExpr) {
            RsExpr wrapped = parenExpr.getExpr();
            return wrapped != null ? processExpr(wrapped) : processRvalue(parenExpr);
        }

        @NotNull
        private Cmt processDeref(@NotNull RsExpr expr, @NotNull Cmt baseCmt) {
            Ty baseType = baseCmt.ty;
            com.intellij.openapi.util.Pair<Ty, Mutability> result = org.rust.lang.core.types.ty.TyUtil.builtinDeref(baseType, lookup.getItems());
            Ty derefType = result != null ? result.getFirst() : TyUnknown.INSTANCE;
            Mutability derefMut = result != null ? result.getSecond() : Mutability.DEFAULT_MUTABILITY;

            PointerKind pointerKind;
            if (baseType instanceof TyAdt) {
                pointerKind = PointerKind.Unique;
            } else if (baseType instanceof TyReference) {
                pointerKind = new PointerKind.BorrowedPointer(BorrowKind.from(((TyReference) baseType).getMutability()), ((TyReference) baseType).getRegion());
            } else if (baseType instanceof TyPointer) {
                pointerKind = new PointerKind.UnsafePointer(((TyPointer) baseType).getMutability());
            } else {
                pointerKind = new PointerKind.UnsafePointer(derefMut);
            }

            return new Cmt(expr, new Categorization.Deref(baseCmt, pointerKind),
                MutabilityCategory.from(baseCmt.mutabilityCategory, pointerKind), derefType);
        }

        @NotNull
        private Cmt processRvalue(@NotNull RsExpr expr) {
            return processRvalue(expr, inference.getExprType(expr));
        }

        @NotNull
        private Cmt processRvalue(@NotNull RsExpr expr, @NotNull Ty ty) {
            return new Cmt(expr, new Categorization.Rvalue(ReStatic.INSTANCE), MutabilityCategory.Declared, ty);
        }

        @NotNull
        public Cmt processRvalue(@NotNull RsElement element, @NotNull Region tempScope, @NotNull Ty ty) {
            return new Cmt(element, new Categorization.Rvalue(tempScope), MutabilityCategory.Declared, ty);
        }

        public void walkPat(@NotNull Cmt cmt, @NotNull RsPat pat, @NotNull WalkPatCallback callback) {
            if (pat instanceof RsPatIdent) {
                RsPatIdent patIdent = (RsPatIdent) pat;
                RsPatBinding binding = patIdent.getPatBinding();
                if (binding.getReference().resolve() == null || !RsElementUtil.isConstantLike(binding.getReference().resolve())) {
                    callback.accept(cmt, pat, binding);
                }
                if (!(patIdent.getPatBinding().getReference().resolve() instanceof RsEnumVariant)) {
                    RsPat subPat = patIdent.getPat();
                    if (subPat != null) {
                        walkPat(cmt, subPat, callback);
                    }
                }
            } else if (pat instanceof RsPatTupleStruct) {
                processTuplePats(cmt, pat, ((RsPatTupleStruct) pat).getPatList(), callback);
            } else if (pat instanceof RsPatTup) {
                processTuplePats(cmt, pat, ((RsPatTup) pat).getPatList(), callback);
            } else if (pat instanceof RsPatStruct) {
                RsPatStruct patStruct = (RsPatStruct) pat;
                for (RsPatField patField : patStruct.getPatFieldList()) {
                    RsPatBinding binding = patField.getPatBinding();
                    if (binding != null) {
                        String fieldName = binding.getReferenceName();
                        Ty fieldType = inference.getBindingType(binding);
                        Cmt fieldCmt = cmtOfField(pat, cmt, fieldName, fieldType);
                        callback.accept(fieldCmt, pat, binding);
                    } else {
                        RsPatFieldFull patFieldFull = patField.getPatFieldFull();
                        if (patFieldFull == null) continue;
                        String fieldName = patFieldFull.getReferenceName();
                        RsPat fieldPat = patFieldFull.getPat();
                        Ty fieldType = inference.getPatType(fieldPat);
                        Cmt fieldCmt = cmtOfField(pat, cmt, fieldName, fieldType);
                        walkPat(fieldCmt, fieldPat, callback);
                    }
                }
            } else if (pat instanceof RsPatSlice) {
                Cmt elementCmt = cmtOfSliceElement(pat, cmt);
                for (RsPat subPat : ((RsPatSlice) pat).getPatList()) {
                    walkPat(elementCmt, subPat, callback);
                }
            }
        }

        private void processTuplePats(@NotNull Cmt cmt, @NotNull RsPat pat, @NotNull List<RsPat> pats, @NotNull WalkPatCallback callback) {
            int index = 0;
            for (RsPat subPat : pats) {
                RsPatBinding subBinding = RsElementExtUtil.descendantsOfTypeFirst(subPat, RsPatBinding.class);
                if (subBinding == null) { index++; continue; }
                Ty subType = inference.getBindingType(subBinding);
                Cmt subCmt = new Cmt(pat, new Categorization.Interior.Field(cmt, String.valueOf(index)), cmt.mutabilityCategory.inherit(), subType);
                walkPat(subCmt, subPat, callback);
                index++;
            }
        }

        @NotNull
        private Cmt cmtOfField(@NotNull RsElement element, @NotNull Cmt baseCmt, @Nullable String fieldName, @NotNull Ty fieldType) {
            return new Cmt(element, new Categorization.Interior.Field(baseCmt, fieldName), baseCmt.mutabilityCategory.inherit(), fieldType);
        }

        @NotNull
        private Cmt cmtOfSliceElement(@NotNull RsElement element, @NotNull Cmt baseCmt) {
            return new Cmt(element, new Categorization.Interior.Pattern(baseCmt), baseCmt.mutabilityCategory.inherit(), baseCmt.ty);
        }
    }

    @FunctionalInterface
    public interface WalkPatCallback {
        void accept(@NotNull Cmt cmt, @NotNull RsPat pat, @NotNull RsPatBinding binding);
    }
}
