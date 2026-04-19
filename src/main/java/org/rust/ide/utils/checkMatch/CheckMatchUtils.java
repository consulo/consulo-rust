/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.checkMatch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.infer.FoldUtil;
import org.rust.lang.core.types.consts.CtValue;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.ty.TyUnknown;
import org.rust.lang.utils.evaluation.ConstExpr;
import org.rust.lang.utils.evaluation.ExpressionEvaluationUtil;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsMatchArmUtil;
import org.rust.lang.core.psi.ext.RsEnumVariantUtil;
import org.rust.lang.utils.evaluation.ConstExprEvaluator;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;

public final class CheckMatchUtils {
    private CheckMatchUtils() {
    }

    @Nullable
    public static List<Pattern> checkExhaustive(@NotNull RsMatchExpr matchExpr) {
        RsExpr expr = matchExpr.getExpr();
        if (expr == null) return null;
        Ty exprType = RsTypesUtil.getType(expr);
        if (FoldUtil.containsTyOfClass(exprType, TyUnknown.class)) return null;
        try {
            return doCheckExhaustive(matchExpr);
        } catch (UnsupportedOperationException e) {
            // TODO case
        } catch (CheckMatchException e) {
            // known unhandled case
        } catch (Constructor.CheckMatchRuntimeException e) {
            // runtime wrapper
        }
        return null;
    }

    @Nullable
    private static List<Pattern> doCheckExhaustive(@NotNull RsMatchExpr match) throws CheckMatchException {
        RsExpr expr = match.getExpr();
        if (expr == null) return null;
        Ty matchedExprType = RsTypesUtil.getType(expr);
        if (FoldUtil.containsTyOfClass(matchedExprType, TyUnknown.class)) return null;
        // match on uninhabited type is exhaustive
        if (!Constructor.isInhabited(matchedExprType)) return null;

        List<List<Pattern>> matrix = calculateMatrix(RsMatchExprUtil.getArms(match));
        if (matrix == null) return null;
        if (!isWellTyped(matrix)) return null;

        Pattern wild = Pattern.wild(matchedExprType);
        UsefulnessResult useful = isUseful(matrix, Collections.singletonList(wild), true,
            RsElementUtil.getCrateRoot((RsElement) match), true);

        if (useful instanceof UsefulnessResult.UsefulWithWitness) {
            List<Witness> witnesses = ((UsefulnessResult.UsefulWithWitness) useful).getWitnesses();
            List<Pattern> result = new ArrayList<>();
            for (Witness w : witnesses) {
                List<Pattern> patterns = w.getPatterns();
                if (patterns.size() == 1) {
                    result.add(patterns.get(0));
                }
            }
            return result.isEmpty() ? null : result;
        }
        return null;
    }

    /**
     * Check if all the patterns have the same type.
     */
    public static boolean isWellTyped(@NotNull List<List<Pattern>> matrix) {
        List<Pattern> flat = matrix.stream().flatMap(Collection::stream).collect(Collectors.toList());
        boolean variantPatternsTypesAreValid = flat.stream().allMatch(p -> {
            if (p.getKind() instanceof PatternKind.Variant) {
                PatternKind.Variant v = (PatternKind.Variant) p.getKind();
                return p.getTy() instanceof TyAdt && v.getItem().equals(((TyAdt) p.getTy()).getItem());
            }
            return true;
        });
        if (!variantPatternsTypesAreValid) return false;

        List<Ty> types = flat.stream().map(Pattern::getTy).collect(Collectors.toList());
        if (types.isEmpty()) return true;
        long distinctCount = types.stream().distinct().count();
        return distinctCount == 1;
    }

    @Nullable
    public static Ty getFirstColumnType(@NotNull List<List<Pattern>> matrix) throws CheckMatchException {
        List<Pattern> firstColumn = getFirstColumn(matrix);
        List<Ty> firstColumnTypes = firstColumn.stream().map(Pattern::getTy).collect(Collectors.toList());
        if (firstColumnTypes.isEmpty()) return null;
        long distinctCount = firstColumnTypes.stream().distinct().count();
        if (distinctCount != 1) {
            throw new CheckMatchException("Ambiguous type of the first column");
        }
        return firstColumnTypes.get(0);
    }

    @NotNull
    public static List<Pattern> getFirstColumn(@NotNull List<List<Pattern>> matrix) {
        List<Pattern> result = new ArrayList<>();
        for (List<Pattern> row : matrix) {
            if (!row.isEmpty()) {
                result.add(row.get(0));
            }
        }
        return result;
    }

    /**
     * Calculates the pattern matrix by splitting or-patterns across different rows.
     */
    @Nullable
    public static List<List<Pattern>> calculateMatrix(@NotNull List<? extends RsMatchArm> arms) {
        List<List<Pattern>> result = new ArrayList<>();
        for (RsMatchArm arm : arms) {
            if (arm.getMatchArmGuard() != null) continue;
            for (RsPat pat : RsMatchArmUtil.getPatList(arm)) {
                try {
                    result.add(Collections.singletonList(lowerPat(pat)));
                } catch (CheckMatchException | UnsupportedOperationException e) {
                    return null;
                }
            }
        }
        return result;
    }

    @NotNull
    static Pattern lowerPat(@NotNull RsPat pat) throws CheckMatchException {
        return new Pattern(RsTypesUtil.getType(pat), getPatKind(pat));
    }

    @NotNull
    private static PatternKind getPatKind(@NotNull RsPat pat) throws CheckMatchException {
        if (pat instanceof RsPatIdent) {
            RsPatIdent patIdent = (RsPatIdent) pat;
            if (patIdent.getPat() != null) {
                throw new UnsupportedOperationException("Support `x @ pat`");
            }
            RsPatBinding binding = patIdent.getPatBinding();
            com.intellij.psi.PsiElement resolved = binding.getReference().resolve();
            if (resolved instanceof RsEnumVariant) {
                RsEnumVariant variant = (RsEnumVariant) resolved;
                return new PatternKind.Variant(RsEnumVariantUtil.getParentEnum(variant), variant, Collections.emptyList());
            }
            if (resolved instanceof RsConstant) {
                RsConstant constant = (RsConstant) resolved;
                RsExpr constExpr = constant.getExpr();
                if (constExpr == null) {
                    throw new CheckMatchException("Can't evaluate constant " + constant.getText());
                }
                ConstExpr.Value<?> value = getExprValue(constExpr);
                if (value == null) {
                    throw new CheckMatchException("Can't evaluate constant " + constant.getText());
                }
                return new PatternKind.Const(value);
            }
            return new PatternKind.Binding(RsTypesUtil.getType(binding), binding.getName() != null ? binding.getName() : "");
        }

        if (pat instanceof RsPatWild) {
            return PatternKind.Wild;
        }

        if (pat instanceof RsPatTup) {
            List<Pattern> subPatterns = new ArrayList<>();
            for (RsPat p : ((RsPatTup) pat).getPatList()) {
                subPatterns.add(lowerPat(p));
            }
            return new PatternKind.Leaf(subPatterns);
        }

        if (pat instanceof RsPatStruct) {
            RsPatStruct patStruct = (RsPatStruct) pat;
            com.intellij.psi.PsiElement resolved = patStruct.getPath().getReference().resolve();
            if (!(resolved instanceof RsFieldsOwner)) {
                throw new CheckMatchException("Can't resolve " + patStruct.getPath().getText());
            }
            RsFieldsOwner item = (RsFieldsOwner) resolved;

            List<Pattern> subPatterns = new ArrayList<>();
            Map<String, RsPatField> nameToPatField = new HashMap<>();
            for (RsPatField patField : patStruct.getPatFieldList()) {
                nameToPatField.put(RsPatFieldUtil.getFieldName(RsPatFieldUtil.getKind(patField)), patField);
            }

            for (RsFieldDecl field : RsFieldsOwnerUtil.getNamedFields(item)) {
                RsPatField patField = nameToPatField.get(field.getName());
                subPatterns.add(createPatternForField(patField, field));
            }

            List<? extends RsFieldDecl> positionalFields = RsFieldsOwnerUtil.getPositionalFields(item);
            for (int index = 0; index < positionalFields.size(); index++) {
                List<RsPatField> patFields = patStruct.getPatFieldList();
                RsPatField patField = index < patFields.size() ? patFields.get(index) : null;
                subPatterns.add(createPatternForField(patField, positionalFields.get(index)));
            }

            return getLeafOrVariant(item, subPatterns);
        }

        if (pat instanceof RsPatTupleStruct) {
            RsPatTupleStruct patTupleStruct = (RsPatTupleStruct) pat;
            com.intellij.psi.PsiElement resolved = patTupleStruct.getPath().getReference().resolve();
            if (resolved == null) {
                throw new CheckMatchException("Can't resolve " + patTupleStruct.getPath().getText());
            }
            List<Pattern> subPatterns = new ArrayList<>();
            for (RsPat p : patTupleStruct.getPatList()) {
                subPatterns.add(lowerPat(p));
            }
            return getLeafOrVariant((RsElement) resolved, subPatterns);
        }

        if (pat instanceof RsPatConst) {
            RsExpr expr = ((RsPatConst) pat).getExpr();
            Ty ty = RsTypesUtil.getType(expr);
            if (ty instanceof TyAdt) {
                TyAdt adt = (TyAdt) ty;
                if (adt.getItem() instanceof RsEnumItem) {
                    RsPath path = ((RsPathExpr) expr).getPath();
                    com.intellij.psi.PsiElement resolved = path.getReference().resolve();
                    if (!(resolved instanceof RsEnumVariant)) {
                        throw new CheckMatchException("Can't resolve " + path.getText());
                    }
                    RsEnumVariant variant = (RsEnumVariant) resolved;
                    return new PatternKind.Variant((RsEnumItem) adt.getItem(), variant, Collections.emptyList());
                } else {
                    throw new CheckMatchException("Unresolved constant");
                }
            } else {
                ConstExpr.Value<?> value = getExprValue(expr);
                if (value == null) {
                    throw new CheckMatchException("Can't evaluate constant " + expr.getText());
                }
                return new PatternKind.Const(value);
            }
        }

        if (pat instanceof RsPatRange) {
            RsPatRange patRange = (RsPatRange) pat;
            List<RsPatConst> patConsts = patRange.getPatConstList();
            RsPatConst lPatConst = patConsts.size() > 0 ? patConsts.get(0) : null;
            RsPatConst rPatConst = patConsts.size() > 1 ? patConsts.get(1) : null;
            if (lPatConst == null || lPatConst.getExpr() == null) {
                throw new CheckMatchException("Incomplete range");
            }
            if (rPatConst == null || rPatConst.getExpr() == null) {
                throw new CheckMatchException("Incomplete range");
            }
            ConstExpr.Value<?> lc = getExprValue(lPatConst.getExpr());
            ConstExpr.Value<?> rc = getExprValue(rPatConst.getExpr());
            if (lc == null) throw new CheckMatchException("Incomplete range");
            if (rc == null) throw new CheckMatchException("Incomplete range");
            return new PatternKind.Range(lc, rc, RsPatRangeUtil.isInclusive(patRange));
        }

        if (pat instanceof RsPatRef) {
            return new PatternKind.Deref(lowerPat(((RsPatRef) pat).getPat()));
        }

        if (pat instanceof RsPatMacro) {
            throw new UnsupportedOperationException("Pat macro");
        }
        if (pat instanceof RsPatSlice) {
            throw new UnsupportedOperationException("Pat slice");
        }
        throw new UnsupportedOperationException("Unknown pat: " + pat.getClass());
    }

    @NotNull
    private static Pattern createPatternForField(@Nullable RsPatField patField, @NotNull RsFieldDecl field) throws CheckMatchException {
        if (patField != null) {
            RsPatFieldFull full = patField.getPatFieldFull();
            if (full != null && full.getPat() != null) {
                return lowerPat(full.getPat());
            }
            RsPatBinding binding = patField.getPatBinding();
            if (binding == null) {
                throw new CheckMatchException("Invalid RsPatField");
            }
            Ty bindingType = RsTypesUtil.getType(binding);
            return new Pattern(bindingType, new PatternKind.Binding(bindingType, binding.getName() != null ? binding.getName() : ""));
        } else {
            org.rust.lang.core.psi.RsTypeReference typeRef = field.getTypeReference();
            if (typeRef == null) {
                throw new CheckMatchException("Field type = null");
            }
            Ty fieldType = RsTypesUtil.getNormType(typeRef);
            return new Pattern(fieldType, PatternKind.Wild);
        }
    }

    @NotNull
    private static PatternKind getLeafOrVariant(@NotNull RsElement item, @NotNull List<Pattern> subPatterns) throws CheckMatchException {
        if (item instanceof RsEnumVariant) {
            RsEnumVariant variant = (RsEnumVariant) item;
            return new PatternKind.Variant(RsEnumVariantUtil.getParentEnum(variant), variant, subPatterns);
        }
        if (item instanceof RsStructItem) {
            return new PatternKind.Leaf(subPatterns);
        }
        throw new CheckMatchException("Impossible case " + item);
    }

    @Nullable
    private static ConstExpr.Value<?> getExprValue(@NotNull RsExpr expr) {
        Object result = ConstExprEvaluator.evaluate(expr);
        if (result instanceof CtValue) {
            return ((CtValue) result).getExpr();
        }
        return null;
    }

    /**
     * Main usefulness algorithm.
     * See detailed description in rust/src/librustc_mir_build/thir/pattern/_match.rs.
     * Original algorithm from INRIA: https://moscova.inria.fr/~maranget/papers/warn/warn004.html
     */
    @NotNull
    public static UsefulnessResult isUseful(
        @NotNull List<List<Pattern>> matrix,
        @NotNull List<Pattern> patterns,
        boolean withWitness,
        @Nullable RsMod crateRoot,
        boolean isTopLevel
    ) {
        if (patterns.isEmpty()) {
            if (matrix.isEmpty()) {
                return withWitness ? UsefulnessResult.UsefulWithWitness.empty() : UsefulnessResult.USEFUL;
            }
            return UsefulnessResult.USELESS;
        }

        Pattern pattern = patterns.get(0);
        Ty type;
        try {
            Ty firstColumnType = getFirstColumnType(matrix);
            type = firstColumnType != null ? firstColumnType : pattern.getErgonomicType();
        } catch (CheckMatchException e) {
            type = pattern.getErgonomicType();
        }
        List<Constructor> constructors = pattern.getConstructors();

        if (constructors != null) {
            return expandConstructors(matrix, patterns, constructors, type, withWitness, crateRoot);
        }

        // Otherwise, pattern is wildcard (or binding which is basically the same).
        List<Pattern> firstColumn = getFirstColumn(matrix);
        List<Constructor> usedConstructors = new ArrayList<>();
        for (Pattern p : firstColumn) {
            List<Constructor> pConstructors = p.getConstructors();
            if (pConstructors != null) {
                usedConstructors.addAll(pConstructors);
            }
        }
        List<Constructor> allConstructors = Constructor.allConstructors(type);
        List<Constructor> missingConstructors = new ArrayList<>(allConstructors);
        missingConstructors.removeAll(usedConstructors);

        boolean isPrivatelyEmpty = allConstructors.isEmpty();
        boolean isDeclaredNonExhaustive = type instanceof TyAdt
            && RsElementUtil.getQueryAttributes(((TyAdt) type).getItem()).hasAtomAttribute("non_exhaustive");
        boolean isInDifferentCrate = type instanceof TyAdt
            && !Objects.equals(RsElementUtil.getCrateRoot((RsElement) ((TyAdt) type).getItem()), crateRoot);

        boolean isNonExhaustive = isPrivatelyEmpty || (isDeclaredNonExhaustive && isInDifferentCrate);

        if (missingConstructors.isEmpty() && !isNonExhaustive) {
            return expandConstructors(matrix, patterns, allConstructors, type, withWitness, crateRoot);
        }

        // Filter rows starting with wildcard or binding
        List<List<Pattern>> wildcardSubmatrix = new ArrayList<>();
        for (List<Pattern> row : matrix) {
            if (!row.isEmpty()) {
                PatternKind rowKind = row.get(0).getKind();
                if (rowKind == PatternKind.Wild || rowKind instanceof PatternKind.Binding) {
                    wildcardSubmatrix.add(row.subList(1, row.size()));
                }
            }
        }
        List<Pattern> remainingPatterns = patterns.subList(1, patterns.size());

        UsefulnessResult res = isUseful(wildcardSubmatrix, remainingPatterns, withWitness, crateRoot, false);

        if (res instanceof UsefulnessResult.UsefulWithWitness) {
            UsefulnessResult.UsefulWithWitness usefulRes = (UsefulnessResult.UsefulWithWitness) res;
            boolean reportConstructors = isTopLevel && !isIntegral(type);
            List<Witness> newWitness;
            if (!reportConstructors && (isNonExhaustive || usedConstructors.isEmpty())) {
                newWitness = new ArrayList<>();
                for (Witness witness : usefulRes.getWitnesses()) {
                    witness.getPatterns().add(Pattern.wild(type));
                    newWitness.add(witness);
                }
            } else {
                newWitness = new ArrayList<>();
                for (Witness witness : usefulRes.getWitnesses()) {
                    for (Constructor mc : missingConstructors) {
                        newWitness.add(witness.cloneWitness().pushWildConstructor(mc, type));
                    }
                }
            }
            return new UsefulnessResult.UsefulWithWitness(newWitness);
        }

        return res;
    }

    @NotNull
    private static UsefulnessResult expandConstructors(
        @NotNull List<List<Pattern>> matrix,
        @NotNull List<Pattern> patterns,
        @NotNull List<Constructor> constructors,
        @NotNull Ty type,
        boolean withWitness,
        @Nullable RsMod crateRoot
    ) {
        for (Constructor constructor : constructors) {
            UsefulnessResult result = isUsefulSpecialized(matrix, patterns, constructor, type, withWitness, crateRoot);
            if (result.isUseful()) {
                return result;
            }
        }
        return UsefulnessResult.USELESS;
    }

    @NotNull
    private static UsefulnessResult isUsefulSpecialized(
        @NotNull List<List<Pattern>> matrix,
        @NotNull List<Pattern> patterns,
        @NotNull Constructor constructor,
        @NotNull Ty type,
        boolean withWitness,
        @Nullable RsMod crateRoot
    ) {
        List<Pattern> newPatterns = specializeRow(patterns, constructor, type);
        if (newPatterns == null) return UsefulnessResult.USELESS;

        List<List<Pattern>> newMatrix = new ArrayList<>();
        for (List<Pattern> row : matrix) {
            List<Pattern> specialized = specializeRow(row, constructor, type);
            if (specialized != null) {
                newMatrix.add(specialized);
            }
        }

        UsefulnessResult useful = isUseful(newMatrix, newPatterns, withWitness, crateRoot, false);
        if (useful instanceof UsefulnessResult.UsefulWithWitness) {
            UsefulnessResult.UsefulWithWitness uw = (UsefulnessResult.UsefulWithWitness) useful;
            List<Witness> mapped = uw.getWitnesses().stream()
                .map(w -> w.applyConstructor(constructor, type))
                .collect(Collectors.toList());
            return new UsefulnessResult.UsefulWithWitness(mapped);
        }
        return useful;
    }

    @Nullable
    private static List<Pattern> specializeRow(
        @NotNull List<Pattern> row,
        @NotNull Constructor constructor,
        @NotNull Ty type
    ) {
        if (row.isEmpty()) return Collections.emptyList();

        Pattern firstPattern = row.get(0);
        List<Pattern> wildPatterns = constructor.subTypes(type).stream()
            .map(Pattern::wild)
            .collect(Collectors.toCollection(ArrayList::new));

        List<Pattern> head;
        PatternKind kind = firstPattern.getKind();

        if (kind instanceof PatternKind.Variant) {
            List<Constructor> firstConstructors = firstPattern.getConstructors();
            if (firstConstructors != null && !firstConstructors.isEmpty() && constructor.equals(firstConstructors.get(0))) {
                fillWithSubPatterns(wildPatterns, ((PatternKind.Variant) kind).getSubPatterns());
                head = wildPatterns;
            } else {
                head = null;
            }
        } else if (kind instanceof PatternKind.Leaf) {
            fillWithSubPatterns(wildPatterns, ((PatternKind.Leaf) kind).getSubPatterns());
            head = wildPatterns;
        } else if (kind instanceof PatternKind.Deref) {
            head = Collections.singletonList(((PatternKind.Deref) kind).getSubPattern());
        } else if (kind instanceof PatternKind.Const) {
            ConstExpr.Value<?> value = ((PatternKind.Const) kind).getValue();
            if (constructor instanceof Constructor.Slice) {
                throw new UnsupportedOperationException("TODO: Slice specialization");
            }
            if (constructor.coveredByRange(value, value, true)) {
                head = Collections.emptyList();
            } else {
                head = null;
            }
        } else if (kind instanceof PatternKind.Range) {
            PatternKind.Range range = (PatternKind.Range) kind;
            if (constructor.coveredByRange(range.getLc(), range.getRc(), range.isInclusive())) {
                head = Collections.emptyList();
            } else {
                head = null;
            }
        } else if (kind instanceof PatternKind.Slice || kind instanceof PatternKind.Array) {
            throw new UnsupportedOperationException("TODO: Slice/Array specialization");
        } else {
            // Wild or Binding
            head = wildPatterns;
        }

        if (head == null) return null;

        List<Pattern> result = new ArrayList<>(head);
        result.addAll(row.subList(1, row.size()));
        return result;
    }

    private static void fillWithSubPatterns(@NotNull List<Pattern> target, @NotNull List<Pattern> subPatterns) {
        for (int index = 0; index < subPatterns.size(); index++) {
            while (target.size() <= index) {
                target.add(Pattern.wild());
            }
            target.set(index, subPatterns.get(index));
        }
    }

    private static boolean isIntegral(@NotNull Ty type) {
        return type instanceof org.rust.lang.core.types.ty.TyInteger
            || type instanceof org.rust.lang.core.types.ty.TyChar;
    }
}
