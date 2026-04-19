/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsFieldLookup;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsMethodCall;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsFieldDecl;
import org.rust.lang.core.resolve.*;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.infer.Autoderef;
import org.rust.lang.core.types.infer.Obligation;
import org.rust.lang.core.types.infer.RsInferenceResult;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.Substitution;

import java.util.*;

public class RsMethodCallReferenceImpl extends RsReferenceBase<RsMethodCall> {

    public RsMethodCallReferenceImpl(@NotNull RsMethodCall element) {
        super(element);
    }

    @NotNull
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
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return element instanceof RsFunction && ((RsFunction) element).isMethod() && super.isReferenceTo(element);
    }

    @NotNull
    public static List<MethodResolveVariant> resolveMethodCallReferenceWithReceiverType(
        @NotNull ImplLookup lookup,
        @NotNull Ty receiverType,
        @NotNull RsMethodCall methodCall
    ) {
        return Processors.collectResolveVariantsAsScopeEntries(methodCall.getReferenceName(), processor ->
            NameResolutionUtil.processMethodCallExprResolveVariants(lookup, receiverType, methodCall, processor)
        );
    }

    @NotNull
    public static List<FieldResolveVariant> resolveFieldLookupReferenceWithReceiverType(
        @NotNull ImplLookup lookup,
        @NotNull Ty receiverType,
        @NotNull RsFieldLookup expr
    ) {
        return Processors.collectResolveVariantsAsScopeEntries(expr.getReferenceName(), processor ->
            NameResolutionUtil.processFieldExprResolveVariants(lookup, receiverType, processor)
        );
    }
}
