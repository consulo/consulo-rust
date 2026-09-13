/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsFieldLookup;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsMethodCall;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.*;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.infer.RsInferenceResult;
import org.rust.lang.core.types.ty.Ty;

import java.util.*;

public class RsMethodCallReferenceImpl extends RsReferenceBase<RsMethodCall> {

    public RsMethodCallReferenceImpl(@Nonnull RsMethodCall element) {
        super(element);
    }

    @Nonnull
    @Override
    public List<RsElement> multiResolve() {
        RsInferenceResult inference = ExtensionsUtil.getInference(getElement());
        if (inference != null) {
            List<MethodResolveVariant> resolved = inference.getResolvedMethod(getElement());
            if (resolved != null) {
                List<RsElement> result = new ArrayList<>(resolved.size());
                for (MethodResolveVariant variant : resolved) {
                    result.add(variant.getElement());
                }
                return result;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isReferenceTo(@Nonnull PsiElement element) {
        return element instanceof RsFunction && ((RsFunction) element).isMethod() && super.isReferenceTo(element);
    }

    @Nonnull
    public static List<MethodResolveVariant> resolveMethodCallReferenceWithReceiverType(
        @Nonnull ImplLookup lookup,
        @Nonnull Ty receiverType,
        @Nonnull RsMethodCall methodCall
    ) {
        return Processors.collectResolveVariantsAsScopeEntries(methodCall.getReferenceName(), processor ->
            NameResolutionUtil.processMethodCallExprResolveVariants(lookup, receiverType, methodCall, processor)
        );
    }

    @Nonnull
    public static List<FieldResolveVariant> resolveFieldLookupReferenceWithReceiverType(
        @Nonnull ImplLookup lookup,
        @Nonnull Ty receiverType,
        @Nonnull RsFieldLookup expr
    ) {
        return Processors.collectResolveVariantsAsScopeEntries(expr.getReferenceName(), processor ->
            NameResolutionUtil.processFieldExprResolveVariants(lookup, receiverType, processor)
        );
    }
}
