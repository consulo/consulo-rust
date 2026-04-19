/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.checkMatch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.TypeUtil;
import org.rust.lang.core.types.consts.CtValue;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.ty.TyUnknown;
import org.rust.lang.utils.evaluation.ConstExpr;
import org.rust.lang.utils.evaluation.ExpressionEvaluationUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsMatchArmUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.infer.FoldUtil;

/**
 * Utilities for exhaustiveness checking of match expressions.
 */
public final class CheckMatchUtil {

    private CheckMatchUtil() {
    }

    @Nullable
    public static List<Pattern> checkExhaustive(@NotNull RsMatchExpr matchExpr) {
        RsExpr expr = matchExpr.getExpr();
        if (expr == null) return null;
        Ty exprType = RsTypesUtil.getType(expr);
        if (FoldUtil.containsTyOfClass(exprType, TyUnknown.class)) return null;
        try {
            return doCheckExhaustive(matchExpr);
        } catch (UnsupportedOperationException ignored) {
        } catch (CheckMatchException ignored) {
        }
        return null;
    }

    @Nullable
    private static List<Pattern> doCheckExhaustive(@NotNull RsMatchExpr match) {
        RsExpr expr = match.getExpr();
        if (expr == null) return null;
        Ty matchedExprType = RsTypesUtil.getType(expr);
        if (FoldUtil.containsTyOfClass(matchedExprType, TyUnknown.class)) return null;
        if (!Constructor.isInhabited(matchedExprType)) return null;

        List<RsMatchArm> arms = match.getMatchBody() != null ? match.getMatchBody().getMatchArmList() : Collections.emptyList();
        List<RsMatchArm> filteredArms = arms.stream()
            .filter(arm -> arm.getMatchArmGuard() == null)
            .collect(Collectors.toList());

        List<List<Pattern>> matrix = calculateMatrix(filteredArms);
        if (!isWellTyped(matrix)) return null;

        Pattern wild = Pattern.wild(matchedExprType);
        UsefulnessResult useful = CheckMatchUtils.isUseful(matrix, Collections.singletonList(wild), true, RsElementUtil.getCrateRoot((RsElement) match), true);

        if (useful instanceof UsefulnessResult.UsefulWithWitness) {
            List<Witness> witnesses = ((UsefulnessResult.UsefulWithWitness) useful).getWitnesses();
            List<Pattern> result = new ArrayList<>();
            for (Witness witness : witnesses) {
                List<Pattern> patterns = witness.getPatterns();
                if (patterns.size() == 1) {
                    result.add(patterns.get(0));
                }
            }
            return result;
        }
        return null;
    }

    @NotNull
    public static List<List<Pattern>> calculateMatrix(@NotNull List<RsMatchArm> arms) {
        List<List<Pattern>> matrix = new ArrayList<>();
        for (RsMatchArm arm : arms) {
            for (RsPat pat : RsMatchArmUtil.getPatList(arm)) {
                matrix.add(Collections.singletonList(CheckMatchUtils.lowerPat(pat)));
            }
        }
        return matrix;
    }

    public static boolean isWellTyped(@NotNull List<List<Pattern>> matrix) {
        for (List<Pattern> row : matrix) {
            for (Pattern pattern : row) {
                PatternKind kind = pattern.getKind();
                if (kind instanceof PatternKind.Variant) {
                    if (!(pattern.getTy() instanceof TyAdt)) return false;
                    if (((PatternKind.Variant) kind).getItem() != ((TyAdt) pattern.getTy()).getItem()) return false;
                }
            }
        }
        List<Ty> types = matrix.stream()
            .flatMap(List::stream)
            .map(Pattern::getTy)
            .collect(Collectors.toList());
        if (types.isEmpty()) return true;
        return types.stream().distinct().count() == 1;
    }

    @Nullable
    public static Ty getFirstColumnType(@NotNull List<List<Pattern>> matrix) {
        List<Ty> firstColumnTypes = matrix.stream()
            .filter(row -> !row.isEmpty())
            .map(row -> row.get(0).getTy())
            .collect(Collectors.toList());
        if (firstColumnTypes.isEmpty()) return null;
        long distinctCount = firstColumnTypes.stream().distinct().count();
        if (distinctCount != 1) {
            throw new CheckMatchException("Ambiguous type of the first column");
        }
        return firstColumnTypes.get(0);
    }

    @NotNull
    public static List<Pattern> getFirstColumn(@NotNull List<List<Pattern>> matrix) {
        return matrix.stream()
            .filter(row -> !row.isEmpty())
            .map(row -> row.get(0))
            .collect(Collectors.toList());
    }
}
