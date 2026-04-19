/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion.sort;

import com.intellij.codeInsight.lookup.LookupElement;

/**
 * A weigher for Rust completion variants.
 *
 * @see RsCompletionWeighers#RS_COMPLETION_WEIGHERS
 */
public interface RsCompletionWeigher {
    /**
     * Returned values are sorted in ascending order, i.e.
     * - {@code 0}, {@code 1}, {@code 2},
     * - {@code false}, {@code true},
     * - upper enum variants before bottom ones.
     *
     * Note that any (Boolean, Number or Enum) value returned from {@link #weigh} is automatically added
     * as an "element feature" in ML-Assisted Completion (identified by {@link #getId}), see
     * {@code org.rust.ml.RsElementFeatureProvider} for more details.
     */
    Comparable<?> weigh(LookupElement element);

    /**
     * The id turns into {@code com.intellij.codeInsight.lookup.LookupElementWeigher.myId}, which then turns into
     * {@code com.intellij.codeInsight.lookup.ClassifierFactory.getId} which is used for comparison of
     * {@code com.intellij.codeInsight.completion.CompletionSorter}s.
     *
     * Also, id identifies the value returned from {@link #weigh} when the value is used as an "element feature"
     * in ML-Assisted Completion (see {@code org.rust.ml.RsElementFeatureProvider}).
     */
    String getId();
}
