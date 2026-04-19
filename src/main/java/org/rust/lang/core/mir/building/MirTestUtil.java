/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.MirBuilder;
import org.rust.lang.core.mir.schemas.*;
import org.rust.lang.core.mir.schemas.MirMatch.MirCandidate;
import org.rust.lang.core.mir.schemas.MirMatch.MirMatchPair;
import org.rust.lang.core.mir.schemas.impls.MirBasicBlockImpl;
import org.rust.lang.core.mir.schemas.impls.MirSwitchTargetsImpl;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.ext.RsEnumItemImplMixin;
import org.rust.lang.core.psi.ext.RsEnumItemUtil;
import org.rust.lang.core.thir.ThirFieldPat;
import org.rust.lang.core.thir.ThirPat;
import org.rust.lang.core.thir.ThirUtilUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyBool;
import org.rust.lang.core.types.ty.TyChar;
import org.rust.lang.core.types.ty.TyInteger;
import org.rust.lang.core.types.ty.TyUtil;
import org.rust.openapiext.TestAssertUtil;

import java.util.*;
import java.util.function.Supplier;

public final class MirTestUtil {
    private MirTestUtil() {
    }

    public static boolean addVariantsToSwitch(
        @NotNull PlaceBuilder testPlace,
        @NotNull MirCandidate candidate,
        @NotNull BitSet variants
    ) {
        MirMatchPair matchPair = null;
        for (MirMatchPair mp : candidate.getMatchPairs()) {
            if (mp.getPlace().equals(testPlace)) {
                matchPair = mp;
                break;
            }
        }
        if (matchPair == null) return false;
        ThirPat pattern = matchPair.getPattern();
        if (pattern instanceof ThirPat.Variant) {
            variants.set(((ThirPat.Variant) pattern).getVariantIndex());
            return true;
        }
        return false;
    }

    public static void performTest(
        @NotNull MirBuilder builder,
        @NotNull MirSpan matchStartSpan,
        @NotNull MirSpan scrutineeSpan,
        @NotNull MirBasicBlockImpl block,
        @NotNull PlaceBuilder placeBuilder,
        @NotNull MirTest test,
        @NotNull Supplier<List<MirBasicBlockImpl>> makeTargetBlocks
    ) {
        MirPlace place = placeBuilder.toPlace();

        MirSourceInfo sourceInfo = builder.sourceInfo(test.getSpan());
        if (test instanceof MirTest.Switch) {
            MirTest.Switch switchTest = (MirTest.Switch) test;
            List<MirBasicBlockImpl> targetBlocks = makeTargetBlocks.get();
            int numEnumVariants = RsEnumItemUtil.getVariants(switchTest.getItem()).size();
            TestAssertUtil.testAssert(() -> targetBlocks.size() == numEnumVariants + 1);
            MirBasicBlockImpl otherwiseBlock = targetBlocks.get(targetBlocks.size() - 1);

            List<com.intellij.openapi.util.Pair<Long, MirBasicBlockImpl>> pairs = new ArrayList<>();
            List<com.intellij.openapi.util.Pair<Integer, Long>> discriminants = ThirUtilUtil.discriminants(switchTest.getItem());
            for (com.intellij.openapi.util.Pair<Integer, Long> entry : discriminants) {
                int index = entry.getFirst();
                long discriminant = entry.getSecond();
                if (switchTest.getVariants().get(index)) {
                    pairs.add(new com.intellij.openapi.util.Pair<>(discriminant, targetBlocks.get(index)));
                }
            }

            MirSwitchTargetsImpl switchTargets = MirSwitchTargetsImpl.create(pairs, otherwiseBlock);
            Ty discrTy = TyInteger.ISize.INSTANCE; // TODO
            MirPlace discr = builder.temp(discrTy, test.getSpan());
            block.pushAssign(discr, new MirRvalue.Discriminant(place), builder.sourceInfo(scrutineeSpan));
            block.terminateWithSwitchInt(new MirOperand.Move(discr), switchTargets, builder.sourceInfo(matchStartSpan));
        } else if (test instanceof MirTest.SwitchInt) {
            throw new UnsupportedOperationException("TODO");
        } else if (test instanceof MirTest.Eq) {
            throw new UnsupportedOperationException("TODO");
        } else if (test instanceof MirTest.Range) {
            throw new UnsupportedOperationException("TODO");
        } else if (test instanceof MirTest.Len) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    @Nullable
    public static Integer sortCandidate(
        @NotNull PlaceBuilder testPlace,
        @NotNull MirTest test,
        @NotNull MirCandidate candidate
    ) {
        int matchPairIndex = -1;
        MirMatchPair matchPair = null;
        List<MirMatchPair> matchPairs = candidate.getMatchPairs();
        for (int i = 0; i < matchPairs.size(); i++) {
            if (matchPairs.get(i).getPlace().equals(testPlace)) {
                matchPairIndex = i;
                matchPair = matchPairs.get(i);
                break;
            }
        }
        if (matchPair == null) return null;

        ThirPat pattern = matchPair.getPattern();

        if (test instanceof MirTest.Switch && pattern instanceof ThirPat.Variant) {
            MirTest.Switch switchTest = (MirTest.Switch) test;
            ThirPat.Variant variantPat = (ThirPat.Variant) pattern;
            TestAssertUtil.testAssert(() -> variantPat.getItem() == switchTest.getItem());
            candidateAfterVariantSwitch(
                matchPairIndex,
                variantPat.getItem(),
                variantPat.getVariantIndex(),
                variantPat.getSubpatterns(),
                candidate
            );
            return variantPat.getVariantIndex();
        }
        if (test instanceof MirTest.Switch) {
            return null;
        }

        if (test instanceof MirTest.SwitchInt && pattern instanceof ThirPat.Const && isSwitchType(((ThirPat.Const) pattern).getTy())) {
            throw new UnsupportedOperationException("TODO");
        }
        if (test instanceof MirTest.SwitchInt && pattern instanceof ThirPat.Range) {
            throw new UnsupportedOperationException("TODO");
        }
        if (test instanceof MirTest.SwitchInt) {
            return null;
        }

        if (test instanceof MirTest.Len && pattern instanceof ThirPat.Slice) {
            throw new UnsupportedOperationException("TODO");
        }

        if (test instanceof MirTest.Range && pattern instanceof ThirPat.Range) {
            throw new UnsupportedOperationException("TODO");
        }
        if (test instanceof MirTest.Range && pattern instanceof ThirPat.Const) {
            throw new UnsupportedOperationException("TODO");
        }
        if (test instanceof MirTest.Range) {
            return null;
        }

        if (test instanceof MirTest.Eq || test instanceof MirTest.Len) {
            throw new UnsupportedOperationException("TODO");
        }

        throw new IllegalStateException("unreachable");
    }

    private static void candidateAfterVariantSwitch(
        int matchPairIndex,
        @NotNull RsEnumItem item,
        int variantIndex,
        @NotNull List<ThirFieldPat> subpatterns,
        @NotNull MirCandidate candidate
    ) {
        MirMatchPair matchPair = candidate.getMatchPairs().remove(matchPairIndex);

        PlaceBuilder downcastPlace = matchPair.getPlace().downcast(item, variantIndex);
        List<MirMatchPair> consequentMatchPairs = new ArrayList<>();
        for (ThirFieldPat subpattern : subpatterns) {
            // e.g., `(x as Variant).0`
            PlaceBuilder place = downcastPlace.cloneProject(
                new MirProjectionElem.Field(subpattern.getField(), subpattern.getPattern().getTy())
            );
            // e.g., `(x as Variant).0 @ P1`
            consequentMatchPairs.add(MirMatchPair.create(place, subpattern.getPattern()));
        }
        candidate.getMatchPairs().addAll(consequentMatchPairs);
    }

    private static boolean isSwitchType(@NotNull Ty ty) {
        return TyUtil.isIntegral(ty) || ty instanceof TyChar || ty instanceof TyBool;
    }
}
