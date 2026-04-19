/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.stubs.common.RsAttributeOwnerPsiOrStub;
import org.rust.lang.utils.evaluation.CfgEvaluator;

public abstract class LazyCfgEvaluator {

    @Nullable
    public abstract CfgEvaluator createEvaluator(@NotNull RsAttributeOwnerPsiOrStub<?> element);

    public static final class Lazy extends LazyCfgEvaluator {
        public static final Lazy INSTANCE = new Lazy();

        private Lazy() {
        }

        @Nullable
        @Override
        public CfgEvaluator createEvaluator(@NotNull RsAttributeOwnerPsiOrStub<?> element) {
            Crate crate = Crate.asNotFake(((RsElement) element).getContainingCrate());
            if (crate == null) return null;
            return CfgEvaluator.forCrate(crate);
        }
    }

    public static final class LazyForCrate extends LazyCfgEvaluator {
        @NotNull
        private final Crate crate;

        public LazyForCrate(@NotNull Crate crate) {
            this.crate = crate;
        }

        @NotNull
        @Override
        public CfgEvaluator createEvaluator(@NotNull RsAttributeOwnerPsiOrStub<?> element) {
            return CfgEvaluator.forCrate(crate);
        }
    }

    public static final class NonLazy extends LazyCfgEvaluator {
        @NotNull
        private final CfgEvaluator evaluator;

        public NonLazy(@NotNull CfgEvaluator evaluator) {
            this.evaluator = evaluator;
        }

        @NotNull
        @Override
        public CfgEvaluator createEvaluator(@NotNull RsAttributeOwnerPsiOrStub<?> element) {
            return evaluator;
        }
    }
}
