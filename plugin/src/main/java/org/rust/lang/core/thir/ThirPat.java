/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.utils.checkMatch.CheckMatchException;
import org.rust.lang.core.mir.MirUtils;
import org.rust.lang.core.mir.RsBindingModeWrapper;
import org.rust.lang.core.mir.schemas.MirSpan;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.infer.InferExtUtil;
import org.rust.lang.core.types.ty.Mutability;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.ty.TyTuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// https://github.com/rust-lang/rust/blob/f7b831ac8a897273f78b9f47165cf8e54066ce4b/compiler/rustc_middle/src/thir.rs#L585
/** See also {@link org.rust.ide.utils.checkMatch.PatternKind} */
public abstract class ThirPat {
    @Nonnull
    public final Ty ty;
    @Nonnull
    public final MirSpan source;

    protected ThirPat(@Nonnull Ty ty, @Nonnull MirSpan source) {
        this.ty = ty;
        this.source = source;
    }

    @Nonnull
    public Ty getTy() {
        return ty;
    }

    @Nonnull
    public MirSpan getSource() {
        return source;
    }

    public static class Wild extends ThirPat {
        public Wild(@Nonnull Ty ty, @Nonnull MirSpan source) {
            super(ty, source);
        }
    }

    public static class AscribeUserType extends ThirPat {
        public AscribeUserType(@Nonnull Ty ty, @Nonnull MirSpan source) {
            super(ty, source);
        }
    }

    public static class Binding extends ThirPat {
        @Nonnull public final Mutability mutability;
        @Nonnull public final String name;
        @Nonnull public final ThirBindingMode mode;
        @Nonnull public final LocalVar variable;
        @Nonnull public final Ty varTy;
        @Nullable public final ThirPat subpattern;
        public final boolean isPrimary;

        public Binding(
            @Nonnull Mutability mutability,
            @Nonnull String name,
            @Nonnull ThirBindingMode mode,
            @Nonnull LocalVar variable,
            @Nonnull Ty varTy,
            @Nullable ThirPat subpattern,
            boolean isPrimary,
            @Nonnull Ty ty,
            @Nonnull MirSpan source
        ) {
            super(ty, source);
            this.mutability = mutability;
            this.name = name;
            this.mode = mode;
            this.variable = variable;
            this.varTy = varTy;
            this.subpattern = subpattern;
            this.isPrimary = isPrimary;
        }

        @Nonnull
        public Mutability getMutability() {
            return mutability;
        }

        @Nonnull
        public String getName() {
            return name;
        }

        @Nonnull
        public ThirBindingMode getMode() {
            return mode;
        }

        @Nonnull
        public LocalVar getVariable() {
            return variable;
        }

        @Nonnull
        public Ty getVarTy() {
            return varTy;
        }

        @Nullable
        public ThirPat getSubpattern() {
            return subpattern;
        }

        public boolean getIsPrimary() {
            return isPrimary;
        }
    }

    public static class Variant extends ThirPat {
        @Nonnull public final RsEnumItem item;
        /** MirVariantIndex */
        public final int variantIndex;
        @Nonnull public final List<ThirFieldPat> subpatterns;

        public Variant(
            @Nonnull RsEnumItem item,
            int variantIndex,
            @Nonnull List<ThirFieldPat> subpatterns,
            @Nonnull Ty ty,
            @Nonnull MirSpan source
        ) {
            super(ty, source);
            this.item = item;
            this.variantIndex = variantIndex;
            this.subpatterns = subpatterns;
        }

        @Nonnull
        public RsEnumItem getItem() {
            return item;
        }

        public int getVariantIndex() {
            return variantIndex;
        }

        @Nonnull
        public List<ThirFieldPat> getSubpatterns() {
            return subpatterns;
        }
    }

    /** {@code (...)}, {@code Foo(...)}, {@code Foo{...}}, or {@code Foo}, where {@code Foo} is a variant name from an ADT with a single variant. */
    public static class Leaf extends ThirPat {
        @Nonnull public final List<ThirFieldPat> subpatterns;

        public Leaf(@Nonnull List<ThirFieldPat> subpatterns, @Nonnull Ty ty, @Nonnull MirSpan source) {
            super(ty, source);
            this.subpatterns = subpatterns;
        }

        @Nonnull
        public List<ThirFieldPat> getSubpatterns() {
            return subpatterns;
        }
    }

    public static class Deref extends ThirPat {
        @Nonnull public final ThirPat subpattern;

        public Deref(@Nonnull ThirPat subpattern, @Nonnull Ty ty, @Nonnull MirSpan source) {
            super(ty, source);
            this.subpattern = subpattern;
        }

        @Nonnull
        public ThirPat getSubpattern() {
            return subpattern;
        }
    }

    public static class Const extends ThirPat {
        public Const(@Nonnull Ty ty, @Nonnull MirSpan source) { super(ty, source); }
    }

    public static class Range extends ThirPat {
        public Range(@Nonnull Ty ty, @Nonnull MirSpan source) { super(ty, source); }
    }

    public static class Slice extends ThirPat {
        public Slice(@Nonnull Ty ty, @Nonnull MirSpan source) { super(ty, source); }
    }

    public static class Array extends ThirPat {
        public Array(@Nonnull Ty ty, @Nonnull MirSpan source) { super(ty, source); }
    }

    public static class Or extends ThirPat {
        public Or(@Nonnull Ty ty, @Nonnull MirSpan source) { super(ty, source); }
    }

    // --- Static factory methods (from companion object) ---

    // TODO: adjustments
    // https://github.com/rust-lang/rust/blob/f7b831ac8a897273f78b9f47165cf8e54066ce4b/compiler/rustc_mir_build/src/thir/pattern/mod.rs#L211
    /** See also {@code org.rust.ide.utils.checkMatch.CheckMatchUtil.getKind} */
    @Nonnull
    public static ThirPat from(@Nonnull RsPat pattern) {
        Ty ty = ExtensionsUtil.getType(pattern);
        MirSpan span = MirUtils.asSpan(pattern);
        if (pattern instanceof RsPatWild) {
            return new Wild(ty, span);
        }
        if (pattern instanceof RsPatIdent) {
            RsPatIdent patIdent = (RsPatIdent) pattern;
            if (patIdent.getPat() != null) throw new UnsupportedOperationException("Support `x @ pat`");
            PsiElement resolved = patIdent.getPatBinding().getReference().resolve();
            if (resolved instanceof RsEnumVariant) {
                if (!(ty instanceof TyAdt)) throw new IllegalStateException("Expected TyAdt for RsPatIdent resolved to RsEnumVariant");
                return lowerVariantOrLeaf((RsEnumVariant) resolved, span, (TyAdt) ty, Collections.emptyList());
            } else if (resolved instanceof RsConstant) {
                throw new UnsupportedOperationException("TODO");
            } else {
                return lowerPatIdent(patIdent.getPatBinding(), ty, span);
            }
        }
        if (pattern instanceof RsPatConst) {
            RsPatConst patConst = (RsPatConst) pattern;
            if (ty instanceof TyAdt) {
                RsStructOrEnumItemElement item = ((TyAdt) ty).getItem();
                if (!(item instanceof RsEnumItem)) throw new IllegalStateException("Unresolved constant");
                RsEnumItem enumItem = (RsEnumItem) item;
                RsPath path = ((RsPathExpr) patConst.getExpr()).getPath();
                PsiElement resolved = path.getReference() != null ? path.getReference().resolve() : null;
                if (!(resolved instanceof RsEnumVariant)) {
                    throw new IllegalStateException("Can't resolve " + path.getText());
                }
                RsEnumVariant variant = (RsEnumVariant) resolved;
                Integer variantIndex = MirrorContext.indexOfVariant(enumItem, variant);
                if (variantIndex == null) throw new IllegalStateException("Can't find enum variant");
                return new Variant(enumItem, variantIndex, Collections.emptyList(), ty, span);
            } else {
                throw new UnsupportedOperationException("TODO");
            }
        }
        if (pattern instanceof RsPatRange) {
            throw new UnsupportedOperationException("TODO");
        }
        if (pattern instanceof RsPatRef) {
            return new Deref(from(((RsPatRef) pattern).getPat()), ty, span);
        }
        if (pattern instanceof RsPatSlice) {
            throw new UnsupportedOperationException("TODO");
        }
        if (pattern instanceof RsPatTup) {
            if (!(ty instanceof TyTuple)) throw new IllegalStateException("Unexpected type for tuple pattern");
            List<ThirFieldPat> subpatterns = lowerTupleSubpats(((RsPatTup) pattern).getPatList(), ((TyTuple) ty).getTypes().size());
            return new Leaf(subpatterns, ty, span);
        }
        if (pattern instanceof RsPatTupleStruct) {
            RsPatTupleStruct patTupleStruct = (RsPatTupleStruct) pattern;
            if (!(ty instanceof TyAdt)) throw new IllegalStateException("Tuple struct pattern not applied to an ADT");
            PsiElement resolved = patTupleStruct.getPath().getReference() != null
                ? patTupleStruct.getPath().getReference().resolve() : null;
            if (!(resolved instanceof RsEnumVariant)) throw new IllegalStateException("Unresolved variant");
            RsEnumVariant variant = (RsEnumVariant) resolved;
            List<ThirFieldPat> subpatterns = lowerTupleSubpats(
                patTupleStruct.getPatList(),
                RsFieldsOwnerUtil.getPositionalFields((RsFieldsOwner) variant).size()
            );
            return lowerVariantOrLeaf(variant, span, (TyAdt) ty, subpatterns);
        }
        if (pattern instanceof RsPatStruct) {
            RsPatStruct patStruct = (RsPatStruct) pattern;
            if (!(ty instanceof TyAdt)) throw new IllegalStateException("Struct pattern not applied to an ADT");
            PsiElement resolved = patStruct.getPath().getReference() != null
                ? patStruct.getPath().getReference().resolve() : null;
            if (!(resolved instanceof RsFieldsOwner)) throw new IllegalStateException("Unresolved path for pat struct");
            RsFieldsOwner item = (RsFieldsOwner) resolved;
            List<ThirFieldPat> subpatterns = new ArrayList<>();
            for (RsPatField patField : patStruct.getPatFieldList()) {
                RsPatFieldFull patFieldFull = patField.getPatFieldFull();
                RsPatBinding patBinding = patField.getPatBinding();
                PsiElement pat;
                ThirPat thirPat;
                if (patFieldFull != null) {
                    pat = patFieldFull;
                    thirPat = from(patFieldFull.getPat());
                } else if (patBinding != null) {
                    pat = patBinding;
                    thirPat = lowerPatIdent(patBinding, ExtensionsUtil.getType(patField), MirUtils.asSpan(patField));
                } else {
                    throw new IllegalStateException("Invalid RsPatField");
                }
                PsiElement fieldResolved = ((RsReferenceElement) pat).getReference().resolve();
                if (!(fieldResolved instanceof RsFieldDecl)) throw new IllegalStateException("Unexpected resolve result");
                RsFieldDecl field = (RsFieldDecl) fieldResolved;
                RsFieldsOwner owner = RsFieldDeclUtil.getOwner(field);
                if (owner == null) throw new IllegalStateException("Can't find owner for field");
                Integer fieldIndex = MirrorContext.indexOfField(owner, field);
                if (fieldIndex == null) throw new IllegalStateException("Can't find field");
                subpatterns.add(new ThirFieldPat(fieldIndex, thirPat));
            }
            return lowerVariantOrLeaf(item, span, (TyAdt) ty, subpatterns);
        }
        if (pattern instanceof RsOrPat) {
            throw new UnsupportedOperationException("TODO");
        }
        if (pattern instanceof RsPatMacro) {
            throw new UnsupportedOperationException("TODO");
        }
        throw new UnsupportedOperationException("Not implemented for type " + pattern.getClass().getName());
    }

    @Nonnull
    private static Binding lowerPatIdent(@Nonnull RsPatBinding binding, @Nonnull Ty ty, @Nonnull MirSpan span) {
        RsBindingModeWrapper bindingMode = new RsBindingModeWrapper(binding.getBindingMode());
        ThirBindingMode mode;
        Mutability mutability;
        if (bindingMode.getRef() == null) {
            mode = ThirBindingMode.ByValue.INSTANCE;
            mutability = bindingMode.getMut() == null ? Mutability.IMMUTABLE : Mutability.MUTABLE;
        } else {
            throw new UnsupportedOperationException("TODO");
        }

        String name = binding.getName();
        if (name == null) throw new IllegalStateException("Could not get name of pattern binding");

        return new Binding(
            mutability,
            name,
            mode,
            LocalVar.from(binding), // TODO: this is wrong in case isPrimary = false
            ty,
            null, // TODO
            true, // TODO: can this even be false? didn't find example
            ty,
            span
        );
    }

    @Nonnull
    private static List<ThirFieldPat> lowerTupleSubpats(@Nonnull List<RsPat> pats, int expectedLen) {
        int restCount = 0;
        for (RsPat pat : pats) {
            if (pat instanceof RsPatRest) restCount++;
        }
        if (restCount > 1) {
            throw new IllegalStateException("More then one .. in tuple pattern");
        }
        boolean hasPatRest = false;
        List<ThirFieldPat> result = new ArrayList<>();
        for (int i = 0; i < pats.size(); i++) {
            RsPat pat = pats.get(i);
            if (pat instanceof RsPatRest) {
                hasPatRest = true;
                continue;
            }
            int index = i + (hasPatRest ? expectedLen - pats.size() : 0);
            ThirPat thirPat = from(pat);
            result.add(new ThirFieldPat(index, thirPat));
        }
        return result;
    }

    @Nonnull
    private static ThirPat lowerVariantOrLeaf(
        @Nonnull RsElement item,
        @Nonnull MirSpan span,
        @Nonnull TyAdt ty,
        @Nonnull List<ThirFieldPat> subpatterns
    ) {
        if (item instanceof RsEnumVariant) {
            RsEnumVariant variant = (RsEnumVariant) item;
            RsEnumItem enumItem = RsEnumVariantUtil.getParentEnum(variant);
            Integer variantIndex = MirrorContext.indexOfVariant(enumItem, variant);
            if (variantIndex == null) throw new IllegalStateException("Can't find enum variant");
            return new Variant(enumItem, variantIndex, subpatterns, ty, span);
        } else if (item instanceof RsStructItem) {
            return new Leaf(subpatterns, ty, span);
        } else {
            throw new CheckMatchException("Impossible case " + item);
        }
    }

    @Nonnull
    public static ThirPat from(@Nonnull RsSelfParameter self) {
        boolean isMut = self.getMut() != null && self.getAnd() == null;
        Ty typeOfValue = InferExtUtil.typeOfValue(self);
        return new Binding(
            Mutability.valueOf(isMut),
            "self",
            ThirBindingMode.ByValue.INSTANCE,
            LocalVar.from(self),
            typeOfValue,
            null,
            true,
            typeOfValue,
            MirUtils.asSpan(self)
        );
    }

    /**
     * Returns the simple identifier name if this is a simple binding pattern, null otherwise.
     */
    @Nullable
    public static String getSimpleIdent(@Nonnull ThirPat pat) {
        if (pat instanceof Binding) {
            Binding binding = (Binding) pat;
            if (binding.mode instanceof ThirBindingMode.ByValue && binding.subpattern == null) {
                return binding.name;
            }
        }
        return null;
    }
}
