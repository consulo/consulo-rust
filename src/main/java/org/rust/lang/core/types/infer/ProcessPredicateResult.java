/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public abstract class ProcessPredicateResult {
    private ProcessPredicateResult() {}

    public static final ProcessPredicateResult Err = new ProcessPredicateResult() {
        @Override
        public String toString() { return "Err"; }
    };

    public static final ProcessPredicateResult NoChanges = new ProcessPredicateResult() {
        @Override
        public String toString() { return "NoChanges"; }
    };

    public static final class Ok extends ProcessPredicateResult {
        @NotNull
        private final List<PendingPredicateObligation> myChildren;

        public Ok(@NotNull List<PendingPredicateObligation> children) {
            myChildren = children;
        }

        public Ok(@NotNull PendingPredicateObligation... children) {
            this(Arrays.asList(children));
        }

        @NotNull
        public List<PendingPredicateObligation> getChildren() {
            return myChildren;
        }
    }
}
