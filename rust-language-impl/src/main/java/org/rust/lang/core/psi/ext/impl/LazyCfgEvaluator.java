/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.stubs.common.RsAttributeOwnerPsiOrStub;
import org.rust.lang.utils.evaluation.CfgEvaluator;
import org.rust.lang.core.psi.ext.*;

public abstract class LazyCfgEvaluator {

    @Nullable
    public abstract CfgEvaluator createEvaluator(@Nonnull RsAttributeOwnerPsiOrStub<?> element);

    public static final class Lazy extends LazyCfgEvaluator {
        public static final Lazy INSTANCE = new Lazy();

        private Lazy() {
        }

        @Nullable
        @Override
        public CfgEvaluator createEvaluator(@Nonnull RsAttributeOwnerPsiOrStub<?> element) {
            Crate crate = Crate.asNotFake(((RsElement) element).getContainingCrate());
            if (crate == null) return null;
            return CfgEvaluator.forCrate(crate);
        }
    }

    public static final class LazyForCrate extends LazyCfgEvaluator {
        @Nonnull
        private final Crate crate;

        public LazyForCrate(@Nonnull Crate crate) {
            this.crate = crate;
        }

        @Nonnull
        @Override
        public CfgEvaluator createEvaluator(@Nonnull RsAttributeOwnerPsiOrStub<?> element) {
            return CfgEvaluator.forCrate(crate);
        }
    }

    public static final class NonLazy extends LazyCfgEvaluator {
        @Nonnull
        private final CfgEvaluator evaluator;

        public NonLazy(@Nonnull CfgEvaluator evaluator) {
            this.evaluator = evaluator;
        }

        @Nonnull
        @Override
        public CfgEvaluator createEvaluator(@Nonnull RsAttributeOwnerPsiOrStub<?> element) {
            return evaluator;
        }
    }
}
