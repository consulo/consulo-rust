/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsInferenceContextOwner;
import org.rust.lang.core.psi.ext.RsItemElement;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.resolve.ParamEnv;
import consulo.util.lang.Pair;

/**
 * Top-level entry point for type inference.
 */
public final class TypeInference {
    private TypeInference() {
    }

    @Nonnull
    public static RsInferenceResult inferTypesIn(@Nonnull RsInferenceContextOwner element) {
        return inferTypesInWithOptions(element, new TypeInferenceOptions()).getSecond();
    }

    @Nonnull
    public static Pair<RsInferenceContext, RsInferenceResult> inferTypesInWithOptions(
        @Nonnull RsInferenceContextOwner element,
        @Nonnull TypeInferenceOptions options
    ) {
        KnownItems items = RsElementUtil.getKnownItems(element);
        ParamEnv paramEnv = element instanceof RsItemElement
            ? ParamEnv.buildFor((RsItemElement) element) : ParamEnv.EMPTY;
        ImplLookup lookup = new ImplLookup(
            element.getProject(), element.getContainingCrate(), items, paramEnv, element, options
        );
        return new Pair<>(lookup.getCtx(), lookup.getCtx().infer(element));
    }
}
