/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.evaluation;

import consulo.util.dataholder.Key;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import jakarta.annotation.Nonnull;
import org.rust.cargo.CfgOptions;
import org.rust.cargo.project.workspace.FeatureState;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsPsiManagerUtil;
import org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub;
import org.rust.lang.core.stubs.index.RsCfgNotTestIndex;

import java.util.*;

/**
 * See <a href="https://doc.rust-lang.org/reference/conditional-compilation.html">Conditional Compilation</a>
 */
public class CfgEvaluator {
    @Nonnull
    private final CfgOptions myOptions;
    @Nonnull
    private final Map<String, FeatureState> myFeatures;
    @Nonnull
    private final PackageOrigin myOrigin;
    private final boolean myEvaluateUnknownCfgToFalse;
    @Nonnull
    private final ThreeValuedLogic myCfgTestValue;

    public CfgEvaluator(
        @Nonnull CfgOptions options,
        @Nonnull Map<String, FeatureState> features,
        @Nonnull PackageOrigin origin,
        boolean evaluateUnknownCfgToFalse,
        @Nonnull ThreeValuedLogic cfgTestValue
    ) {
        myOptions = options;
        myFeatures = features;
        myOrigin = origin;
        myEvaluateUnknownCfgToFalse = evaluateUnknownCfgToFalse;
        myCfgTestValue = cfgTestValue;
    }

    @Nonnull
    public ThreeValuedLogic evaluate(@Nonnull Iterable<RsMetaItemPsiOrStub> cfgAttributes) {
        return evaluate(CfgPredicate.fromCfgAttributes(cfgAttributes));
    }

    @Nonnull
    public ThreeValuedLogic evaluateCondition(@Nonnull RsMetaItemPsiOrStub predicate) {
        return evaluate(CfgPredicate.fromMetaItem(predicate));
    }

    /**
     * Flatten {@code #[cfg_attr(cond, attr1, attr2, ...)]} attributes into the contained attributes
     * when the condition is not {@link ThreeValuedLogic#False}. Non-{@code cfg_attr} attributes are
     * passed through unchanged. Nested {@code cfg_attr} attributes are expanded recursively.
     * <p>
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public <T extends RsMetaItemPsiOrStub> java.util.stream.Stream<T> expandCfgAttrs(
        @Nonnull java.util.stream.Stream<? extends T> rawMetaItems
    ) {
        return rawMetaItems.flatMap(item -> {
            if ("cfg_attr".equals(item.getName())) {
                java.util.List<? extends RsMetaItemPsiOrStub> args = item.getMetaItemArgsList();
                if (args.isEmpty()) return java.util.stream.Stream.empty();
                RsMetaItemPsiOrStub condition = args.get(0);
                if (evaluateCondition(condition) == ThreeValuedLogic.False) {
                    return java.util.stream.Stream.empty();
                }
                java.util.stream.Stream<T> tail =
                    (java.util.stream.Stream<T>) args.subList(1, args.size()).stream();
                return (java.util.stream.Stream<T>) expandCfgAttrs(tail);
            }
            return java.util.stream.Stream.of((T) item);
        });
    }

    @Nonnull
    private ThreeValuedLogic evaluate(@Nonnull CfgPredicate cfgPredicate) {
        ThreeValuedLogic result = evaluatePredicate(cfgPredicate);

        if (result == ThreeValuedLogic.True) {
            CfgTestmarks.EvaluatesTrue.INSTANCE.hit();
        } else if (result == ThreeValuedLogic.False) {
            CfgTestmarks.EvaluatesFalse.INSTANCE.hit();
        }

        return result;
    }

    @Nonnull
    private ThreeValuedLogic evaluatePredicate(@Nonnull CfgPredicate predicate) {
        if (predicate instanceof CfgPredicate.All) {
            ThreeValuedLogic acc = ThreeValuedLogic.True;
            for (CfgPredicate pred : ((CfgPredicate.All) predicate).myList) {
                acc = acc.and(evaluatePredicate(pred));
            }
            return acc;
        } else if (predicate instanceof CfgPredicate.Any) {
            ThreeValuedLogic acc = ThreeValuedLogic.False;
            for (CfgPredicate pred : ((CfgPredicate.Any) predicate).myList) {
                acc = acc.or(evaluatePredicate(pred));
            }
            return acc;
        } else if (predicate instanceof CfgPredicate.Not) {
            return evaluatePredicate(((CfgPredicate.Not) predicate).mySingle).not();
        } else if (predicate instanceof CfgPredicate.NameOption) {
            return evaluateName(((CfgPredicate.NameOption) predicate).myName);
        } else if (predicate instanceof CfgPredicate.NameValueOption) {
            CfgPredicate.NameValueOption nv = (CfgPredicate.NameValueOption) predicate;
            return evaluateNameValue(nv.myName, nv.myValue);
        } else {
            return ThreeValuedLogic.Unknown;
        }
    }

    @Nonnull
    private ThreeValuedLogic evaluateName(@Nonnull String name) {
        if ("cfg_panic".equals(name)) return ThreeValuedLogic.Unknown;
        if ("test".equals(name)) return myCfgTestValue;
        if ("rustdoc".equals(name) && myOrigin == PackageOrigin.STDLIB) return ThreeValuedLogic.Unknown;

        if (myEvaluateUnknownCfgToFalse) {
            return ThreeValuedLogic.fromBoolean(myOptions.isNameEnabled(name));
        } else {
            if (SUPPORTED_NAME_OPTIONS.contains(name)) {
                return ThreeValuedLogic.fromBoolean(myOptions.isNameEnabled(name));
            }
            return ThreeValuedLogic.Unknown;
        }
    }

    @Nonnull
    private ThreeValuedLogic evaluateNameValue(@Nonnull String name, @Nonnull String value) {
        if ("feature".equals(name)) return evaluateFeature(value);

        if (myEvaluateUnknownCfgToFalse) {
            return ThreeValuedLogic.fromBoolean(myOptions.isNameValueEnabled(name, value));
        } else {
            if (SUPPORTED_NAME_VALUE_OPTIONS.contains(name)) {
                return ThreeValuedLogic.fromBoolean(myOptions.isNameValueEnabled(name, value));
            }
            return ThreeValuedLogic.Unknown;
        }
    }

    @Nonnull
    private ThreeValuedLogic evaluateFeature(@Nonnull String name) {
        if (myOrigin == PackageOrigin.STDLIB) {
            return ThreeValuedLogic.Unknown;
        }

        FeatureState state = myFeatures.get(name);
        if (state == FeatureState.Enabled) return ThreeValuedLogic.True;
        if (state == FeatureState.Disabled) return ThreeValuedLogic.False;
        // state is null
        if (myOptions.isNameValueEnabled("feature", name)) return ThreeValuedLogic.True;
        if (myEvaluateUnknownCfgToFalse) return ThreeValuedLogic.False;
        return ThreeValuedLogic.Unknown;
    }

    // Static constants and factory methods

    private static final Set<String> SUPPORTED_NAME_OPTIONS = new HashSet<>(Arrays.asList(
        "debug_assertions", "unix", "windows", "test", "doc"
    ));

    private static final Set<String> SUPPORTED_NAME_VALUE_OPTIONS = new HashSet<>(Arrays.asList(
        "target_arch", "target_endian", "target_env", "target_family",
        "target_feature", "target_os", "target_pointer_width", "target_vendor"
    ));

    private static final Key<CachedValue<CfgEvaluator>> CRATE_CFG_EVALUATOR_KEY =
        Key.create("CRATE_CFG_EVALUATOR_KEY");

    @Nonnull
    public static CfgEvaluator forCrate(@Nonnull Crate crate) {
        return CachedValuesManager.getManager(crate.getProject()).getCachedValue(
            crate,
            CRATE_CFG_EVALUATOR_KEY,
            () -> CachedValueProvider.Result.create(
                forCrateInner(crate),
                RsPsiManagerUtil.getRustStructureModificationTracker(crate)
            ),
            false
        );
    }

    @Nonnull
    private static CfgEvaluator forCrateInner(@Nonnull Crate crate) {
        ThreeValuedLogic cfgTest;
        switch (crate.getOrigin()) {
            case STDLIB:
            case STDLIB_DEPENDENCY:
                cfgTest = ThreeValuedLogic.False;
                break;
            case DEPENDENCY: {
                boolean hasCfgNotTest = crate.getCargoTarget() != null &&
                    crate.getCargoTarget().getPkg() != null &&
                    RsCfgNotTestIndex.hasCfgNotTest(crate.getProject(), crate.getCargoTarget().getPkg());
                cfgTest = ThreeValuedLogic.fromBoolean(
                    crate.getCargoTarget() != null &&
                        crate.getCargoTarget().getPkg() != null &&
                        !hasCfgNotTest
                );
                break;
            }
            default:
                cfgTest = ThreeValuedLogic.Unknown;
                break;
        }
        return forCrate(crate, crate.getEvaluateUnknownCfgToFalse(), cfgTest);
    }

    @Nonnull
    public static CfgEvaluator forCrate(
        @Nonnull Crate crate,
        boolean evaluateUnknownCfgToFalse,
        @Nonnull ThreeValuedLogic cfgTestValue
    ) {
        return new CfgEvaluator(
            crate.getCfgOptions(),
            crate.getFeatures(),
            crate.getOrigin(),
            evaluateUnknownCfgToFalse,
            cfgTestValue
        );
    }
}
