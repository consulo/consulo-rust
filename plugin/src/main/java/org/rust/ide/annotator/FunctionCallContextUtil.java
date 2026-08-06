/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsCallExpr;
import org.rust.lang.core.psi.RsMethodCall;
import org.rust.lang.core.psi.RsValueArgumentList;

/**
 * Delegates to RsErrorAnnotator's FunctionCallContext methods.
 */
public final class FunctionCallContextUtil {
    private FunctionCallContextUtil() {
    }

    @Nullable
    public static RsErrorAnnotator.FunctionCallContext getFunctionCallContext(@Nonnull RsValueArgumentList args) {
        return RsErrorAnnotator.getFunctionCallContext(args);
    }

    @Nullable
    public static RsErrorAnnotator.FunctionCallContext getFunctionCallContext(@Nonnull RsCallExpr callExpr) {
        return RsErrorAnnotator.getFunctionCallContext(callExpr);
    }

    @Nullable
    public static RsErrorAnnotator.FunctionCallContext getFunctionCallContext(@Nonnull RsMethodCall methodCall) {
        return RsErrorAnnotator.getFunctionCallContext(methodCall);
    }
}
