/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion.sort;

import com.intellij.codeInsight.lookup.LookupElement;
import org.rust.lang.core.completion.RsLookupElement;
import org.rust.lang.core.completion.RsLookupElementProperties;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * A list of weighers that sort completion variants in the Rust plugin.
 */
public final class RsCompletionWeighers {
    private RsCompletionWeighers() {
    }

    public static final List<Object> RS_COMPLETION_WEIGHERS = Collections.unmodifiableList(Arrays.asList(
        "priority",

        preferTrue(RsLookupElementProperties::isFullLineCompletion, "rust-impl-member-full-line-completion"),
        preferUpperVariant(RsLookupElementProperties::getKeywordKind, "rust-prefer-keywords"),
        preferTrue(RsLookupElementProperties::isSelfTypeCompatible, "rust-prefer-compatible-self-type"),
        preferTrue(RsLookupElementProperties::isReturnTypeConformsToExpectedType, "rust-prefer-matching-expected-type"),
        preferTrue(RsLookupElementProperties::isLocal, "rust-prefer-locals"),
        preferUpperVariant(RsLookupElementProperties::getElementKind, "rust-prefer-by-kind"),
        preferTrue(RsLookupElementProperties::isInherentImplMember, "rust-prefer-inherent-impl-member"),

        "prefix",
        "stats",
        "proximity"
    ));

    private static RsCompletionWeigher preferTrue(
        Function<RsLookupElementProperties, Boolean> property,
        String id
    ) {
        return new RsCompletionWeigher() {
            @Override
            public Comparable<?> weigh(LookupElement element) {
                if (element instanceof RsLookupElement) {
                    return !property.apply(((RsLookupElement) element).getProps());
                }
                return true;
            }

            @Override
            public String getId() {
                return id;
            }
        };
    }

    private static RsCompletionWeigher preferUpperVariant(
        Function<RsLookupElementProperties, Enum<?>> property,
        String id
    ) {
        return new RsCompletionWeigher() {
            @Override
            public Comparable<?> weigh(LookupElement element) {
                if (element instanceof RsLookupElement) {
                    return property.apply(((RsLookupElement) element).getProps()).ordinal();
                }
                return Integer.MAX_VALUE;
            }

            @Override
            public String getId() {
                return id;
            }
        };
    }
}
