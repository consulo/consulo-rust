/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.evaluation;

import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
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
    @NotNull
    private final CfgOptions myOptions;
    @NotNull
    private final Map<String, FeatureState> myFeatures;
    @NotNull
    private final PackageOrigin myOrigin;
    private final boolean myEvaluateUnknownCfgToFalse;
    @NotNull
    private final ThreeValuedLogic myCfgTestValue;

    public CfgEvaluator(
        @NotNull CfgOptions options,
        @NotNull Map<String, FeatureState> features,
        @NotNull PackageOrigin origin,
        boolean evaluateUnknownCfgToFalse,
        @NotNull ThreeValuedLogic cfgTestValue
    ) {
        myOptions = options;
        myFeatures = features;
        myOrigin = origin;
        myEvaluateUnknownCfgToFalse = evaluateUnknownCfgToFalse;
        myCfgTestValue = cfgTestValue;
    }

    @NotNull
    public ThreeValuedLogic evaluate(@NotNull Iterable<RsMetaItemPsiOrStub> cfgAttributes) {
        return evaluate(CfgPredicate.fromCfgAttributes(cfgAttributes));
    }

    @NotNull
    public ThreeValuedLogic evaluateCondition(@NotNull RsMetaItemPsiOrStub predicate) {
        return evaluate(CfgPredicate.fromMetaItem(predicate));
    }

    /**
     * Flatten {@code #[cfg_attr(cond, attr1, attr2, ...)]} attributes into the contained attributes
     * when the condition is not {@link ThreeValuedLogic#False}. Non-{@code cfg_attr} attributes are
     * passed through unchanged. Nested {@code cfg_attr} attributes are expanded recursively.
     * <p>
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T extends RsMetaItemPsiOrStub> java.util.stream.Stream<T> expandCfgAttrs(
        @NotNull java.util.stream.Stream<? extends T> rawMetaItems
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

    @NotNull
    private ThreeValuedLogic evaluate(@NotNull CfgPredicate cfgPredicate) {
        ThreeValuedLogic result = evaluatePredicate(cfgPredicate);

        if (result == ThreeValuedLogic.True) {
            CfgTestmarks.EvaluatesTrue.INSTANCE.hit();
        } else if (result == ThreeValuedLogic.False) {
            CfgTestmarks.EvaluatesFalse.INSTANCE.hit();
        }

        return result;
    }

    @NotNull
    private ThreeValuedLogic evaluatePredicate(@NotNull CfgPredicate predicate) {
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

    @NotNull
    private ThreeValuedLogic evaluateName(@NotNull String name) {
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

    @NotNull
    private ThreeValuedLogic evaluateNameValue(@NotNull String name, @NotNull String value) {
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

    @NotNull
    private ThreeValuedLogic evaluateFeature(@NotNull String name) {
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

    @NotNull
    public static CfgEvaluator forCrate(@NotNull Crate crate) {
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

    @NotNull
    private static CfgEvaluator forCrateInner(@NotNull Crate crate) {
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

    @NotNull
    public static CfgEvaluator forCrate(
        @NotNull Crate crate,
        boolean evaluateUnknownCfgToFalse,
        @NotNull ThreeValuedLogic cfgTestValue
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
