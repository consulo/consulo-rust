/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsFieldDecl;
import org.rust.lang.core.psi.ext.RsFieldsOwner;
import org.rust.lang.core.psi.ext.impl.RsBindingModeKind;
import org.rust.lang.core.psi.ext.impl.RsFieldsOwnerUtil;
import org.rust.lang.core.psi.ext.impl.RsPatBindingUtil;
import org.rust.lang.core.psi.ext.impl.RsPatFieldKind;
import org.rust.lang.core.psi.ext.impl.RsPatFieldUtil;
import org.rust.lang.core.psi.impl.RsLiteralKind;
import org.rust.lang.core.psi.impl.RsLiteralKindUtil;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtUnknown;
import org.rust.lang.core.types.ty.*;
import org.rust.lang.utils.evaluation.ConstExpr;
import org.rust.openapiext.Testmark;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Walks a pattern against the type it matches and records the type of every binding, following the default
 * binding modes of match ergonomics.
 */
public final class PatternMatchingUtil {
    private PatternMatchingUtil() {
    }

    public static void extractBindings(@Nonnull RsPat pat, @Nonnull RsTypeInferenceWalker walker, @Nonnull Ty type) {
        extractBindings(pat, walker, type, new RsBindingModeKind.BindByValue(Mutability.IMMUTABLE));
    }

    public static void extractBindings(
        @Nonnull RsPat pat,
        @Nonnull RsTypeInferenceWalker walker,
        @Nonnull Ty type,
        @Nonnull RsBindingModeKind defBm
    ) {
        if (pat instanceof RsPatWild) {
            walker.writePatTy(pat, type);
        }
        else if (pat instanceof RsPatConst patConst) {
            RsExpr expr = patConst.getExpr();
            if (expr instanceof RsPathExpr pathExpr) {
                Ty inferred = walker.inferType(expr);
                List<ResolvedPath> resolved = walker.getResolvedPath(pathExpr);
                boolean constant = resolved.size() == 1 && resolved.get(0).getElement() instanceof RsConstant;
                Ty expected = constant ? type : stripReferences(type, defBm).ty;
                walker.coerce(expr, inferred, expected);
                walker.writePatTy(pat, expected);
            }
            else {
                boolean string = expr instanceof RsLitExpr lit && RsLiteralKindUtil.getKind(lit) instanceof RsLiteralKind.StringLiteral;
                Ty expected = string ? type : stripReferences(type, defBm).ty;
                walker.writePatTy(pat, expected);
                walker.inferTypeCoercableTo(expr, expected);
            }
        }
        else if (pat instanceof RsPatRef patRef) {
            RsPat inner = patRef.getPat();
            if (inner != null) {
                extractBindings(inner, walker, type instanceof TyReference reference ? reference.getReferenced() : TyUnknown.INSTANCE);
            }
            walker.writePatTy(pat, type);
        }
        else if (pat instanceof RsPatRange range) {
            Ty expected = stripReferences(type, defBm).ty;
            walker.writePatTy(pat, expected);
            for (RsPatConst bound : range.getPatConstList()) {
                walker.inferTypeCoercableTo(bound.getExpr(), expected);
            }
        }
        else if (pat instanceof RsPatIdent patIdent) {
            RsPatBinding patBinding = patIdent.getPatBinding();
            PsiElement resolved = patBinding.getReference() != null ? patBinding.getReference().resolve() : null;
            Ty bindingType = resolved instanceof RsEnumVariant
                ? stripReferences(type, defBm).ty
                : inferBindingType(patBinding, type, defBm);
            walker.writePatTy(pat, bindingType);
            RsPat subPat = patIdent.getPat();
            if (subPat != null) extractBindings(subPat, walker, type);
        }
        else if (pat instanceof RsPatTup tup) {
            Stripped stripped = stripReferences(type, defBm);
            walker.writePatTy(pat, stripped.ty);
            List<Ty> types = stripped.ty instanceof TyTuple tuple ? tuple.getTypes() : List.of();
            inferTupleFieldsTypes(walker, tup.getPatList(), stripped.bm, types.size(),
                idx -> idx >= 0 && idx < types.size() ? types.get(idx) : TyUnknown.INSTANCE);
        }
        else if (pat instanceof RsPatTupleStruct tupleStruct) {
            Stripped stripped = stripReferences(type, defBm);
            Ty expected = stripped.ty;
            walker.writePatTy(pat, expected);
            RsFieldsOwner item = fieldsOwnerOf(tupleStruct.getPath(), expected);
            if (item == null) return;
            List<? extends RsFieldDecl> tupleFields = RsFieldsOwnerUtil.getPositionalFields(item);
            Substitution subst = expected.getTypeParameterValues();
            inferTupleFieldsTypes(walker, tupleStruct.getPatList(), stripped.bm, tupleFields.size(), idx -> {
                if (idx < 0 || idx >= tupleFields.size()) return TyUnknown.INSTANCE;
                RsTypeReference typeRef = tupleFields.get(idx).getTypeReference();
                return typeRef != null ? substituteAndNormalize(ExtensionsUtil.getRawType(typeRef), subst, walker) : TyUnknown.INSTANCE;
            });
        }
        else if (pat instanceof RsPatStruct patStruct) {
            Stripped stripped = stripReferences(type, defBm);
            Ty expected = stripped.ty;
            walker.writePatTy(pat, expected);
            RsFieldsOwner item = fieldsOwnerOf(patStruct.getPath(), expected);
            if (item == null) return;

            // Field types come from the struct declaration. Asking for the type of the pattern field instead
            // would re-enter inference of the function being inferred right now and recurse forever.
            Map<String, RsFieldDecl> structFields = new HashMap<>();
            for (RsNamedFieldDecl decl : RsFieldsOwnerUtil.getNamedFields(item)) {
                if (decl.getName() != null) structFields.put(decl.getName(), decl);
            }
            List<? extends RsFieldDecl> positional = RsFieldsOwnerUtil.getPositionalFields(item);
            for (int i = 0; i < positional.size(); i++) {
                structFields.put(Integer.toString(i), positional.get(i));
            }
            Substitution subst = expected.getTypeParameterValues();
            for (RsPatField patField : patStruct.getPatFieldList()) {
                RsPatFieldKind kind = RsPatFieldUtil.getKind(patField);
                RsFieldDecl decl = structFields.get(RsPatFieldUtil.getFieldName(kind));
                RsTypeReference typeRef = decl != null ? decl.getTypeReference() : null;
                Ty fieldType = typeRef != null ? substituteAndNormalize(ExtensionsUtil.getRawType(typeRef), subst, walker) : TyUnknown.INSTANCE;

                if (kind instanceof RsPatFieldKind.Full full) {
                    extractBindings(full.getPat(), walker, fieldType, stripped.bm);
                    walker.writePatFieldTy(patField, fieldType);
                }
                else if (kind instanceof RsPatFieldKind.Shorthand shorthand) {
                    Ty bindingType = fieldType;
                    if (fieldType instanceof TyAdt adt && shorthand.isBox()) {
                        List<Ty> args = adt.getTypeArguments();
                        if (args.isEmpty()) return;
                        bindingType = args.get(0);
                    }
                    walker.writePatFieldTy(patField, inferBindingType(shorthand.getBinding(), bindingType, stripped.bm));
                }
            }
        }
        else if (pat instanceof RsPatSlice slice) {
            Stripped stripped = stripReferences(type, defBm);
            walker.writePatTy(pat, stripped.ty);
            inferSlicePatsTypes(walker, slice.getPatList(), stripped.bm, stripped.ty);
        }
        else if (pat instanceof RsPatBox box) {
            Stripped stripped = stripReferences(type, defBm);
            walker.writePatTy(pat, stripped.ty);
            if (stripped.ty instanceof TyAdt adt && TyUtil.isBox(adt) && box.getPat() != null) {
                List<Ty> args = adt.getTypeArguments();
                if (args.isEmpty()) return;
                extractBindings(box.getPat(), walker, args.get(0), stripped.bm);
            }
        }
        else if (pat instanceof RsOrPat orPat) {
            for (RsPat alternative : orPat.getPatList()) {
                extractBindings(alternative, walker, type, defBm);
            }
            walker.writePatTy(pat, type);
        }
    }

    /** The struct or variant a pattern matches: its own path if that resolves, else the matched struct. */
    private static RsFieldsOwner fieldsOwnerOf(@Nonnull RsPath path, @Nonnull Ty expected) {
        PsiElement resolved = path.getReference() != null ? path.getReference().resolve() : null;
        if (resolved instanceof RsFieldsOwner owner) return owner;
        if (expected instanceof TyAdt adt && adt.getItem() instanceof RsStructItem struct) return struct;
        return null;
    }

    @Nonnull
    private static Ty substituteAndNormalize(@Nonnull Ty ty, @Nonnull Substitution subst, @Nonnull RsTypeInferenceWalker walker) {
        Ty substituted = FoldUtil.substituteOrUnknown(ty, subst);
        TyWithObligations<Ty> normalized = walker.getCtx().normalizeAssociatedTypesIn(substituted);
        for (Obligation obligation : normalized.getObligations()) {
            walker.getCtx().fulfill.registerPredicateObligation(obligation);
        }
        return normalized.getValue();
    }

    private static boolean isRest(@Nonnull RsPat pat) {
        return pat instanceof RsPatRest || pat instanceof RsPatIdent ident && ident.getPat() instanceof RsPatRest;
    }

    private static void inferSlicePatsTypes(
        @Nonnull RsTypeInferenceWalker walker,
        @Nonnull List<RsPat> patList,
        @Nonnull RsBindingModeKind bm,
        @Nonnull Ty sliceType
    ) {
        Ty elementType;
        Ty restType;
        if (sliceType instanceof TyArray array) {
            elementType = array.getBase();
            restType = new TyArray(array.getBase(), restSize(array, patList));
        }
        else if (sliceType instanceof TySlice slice) {
            elementType = slice.getElementType();
            restType = sliceType;
        }
        else {
            elementType = TyUnknown.INSTANCE;
            restType = TyUnknown.INSTANCE;
        }
        for (RsPat pat : patList) {
            extractBindings(pat, walker, isRest(pat) ? restType : elementType, bm);
        }
    }

    @Nonnull
    private static Const restSize(@Nonnull TyArray array, @Nonnull List<RsPat> patList) {
        Long arraySize = array.getSize();
        if (arraySize == null) return CtUnknown.INSTANCE;
        long patRestCount = patList.stream().filter(PatternMatchingUtil::isRest).count();
        if (patRestCount != 1) {
            PatternMatchingTestMarks.MultipleRestPats.hit();
            return CtUnknown.INSTANCE;
        }
        long restSize = arraySize - patList.size() + 1;
        if (restSize < 0) {
            PatternMatchingTestMarks.NegativeRestSize.hit();
            return CtUnknown.INSTANCE;
        }
        return ConstExpr.toConst(new ConstExpr.Value.Integer(restSize, TyInteger.USize.INSTANCE));
    }

    /**
     * In correct code a tuple or tuple struct pattern has at most one `..`. Several are tolerated by ignoring the
     * bindings between the first and the last of them, as in `let (x, .., y, .., z) = tuple`.
     */
    private static void inferTupleFieldsTypes(
        @Nonnull RsTypeInferenceWalker walker,
        @Nonnull List<RsPat> patList,
        @Nonnull RsBindingModeKind bm,
        int tupleSize,
        @Nonnull IntFunction<Ty> type
    ) {
        int firstPatRestIndex = Integer.MAX_VALUE;
        int lastPatRestIndex = -1;
        for (int index = 0; index < patList.size(); index++) {
            if (isRest(patList.get(index))) {
                firstPatRestIndex = Math.min(firstPatRestIndex, index);
                lastPatRestIndex = Math.max(lastPatRestIndex, index);
            }
        }
        for (int idx = 0; idx < patList.size(); idx++) {
            Ty fieldType;
            if (idx < firstPatRestIndex) fieldType = type.apply(idx);
            else if (idx > lastPatRestIndex) fieldType = type.apply(tupleSize - (patList.size() - idx));
            else fieldType = TyUnknown.INSTANCE;
            extractBindings(patList.get(idx), walker, fieldType, bm);
        }
    }

    @Nonnull
    private static Ty inferBindingType(@Nonnull RsPatBinding binding, @Nonnull Ty expected, @Nonnull RsBindingModeKind defBm) {
        RsBindingModeKind bm = RsPatBindingUtil.getKind(binding);
        if (bm instanceof RsBindingModeKind.BindByValue byValue && byValue.getMutability() == Mutability.IMMUTABLE) {
            bm = defBm;
        }
        return bm instanceof RsBindingModeKind.BindByReference byRef ? new TyReference(expected, byRef.getMutability()) : expected;
    }

    private record Stripped(@Nonnull Ty ty, @Nonnull RsBindingModeKind bm) {
    }

    /** Peels the references off a matched type, turning the default binding mode into a by-reference one. */
    @Nonnull
    private static Stripped stripReferences(@Nonnull Ty type, @Nonnull RsBindingModeKind defBm) {
        RsBindingModeKind bm = defBm;
        Ty ty = type;
        while (ty instanceof TyReference reference) {
            if (bm instanceof RsBindingModeKind.BindByReference byRef) {
                bm = new RsBindingModeKind.BindByReference(byRef.getMutability() == Mutability.IMMUTABLE ? Mutability.IMMUTABLE : reference.getMutability());
            }
            else {
                bm = new RsBindingModeKind.BindByReference(reference.getMutability());
            }
            ty = reference.getReferenced();
        }
        return new Stripped(ty, bm);
    }

    public static final class PatternMatchingTestMarks {
        public static final Testmark MultipleRestPats = new Testmark();
        public static final Testmark NegativeRestSize = new Testmark();
    }
}
