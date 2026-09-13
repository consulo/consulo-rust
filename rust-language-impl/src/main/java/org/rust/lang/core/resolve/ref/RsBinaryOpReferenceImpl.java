/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsBinaryOp;
import org.rust.lang.core.psi.ext.OverloadableBinaryOperator;
import org.rust.lang.core.psi.ext.impl.RsBinaryOpImplUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.NameResolution;

import java.util.Collections;
import java.util.List;

public class RsBinaryOpReferenceImpl extends RsReferenceCached<RsBinaryOp> {

    public RsBinaryOpReferenceImpl(@Nonnull RsBinaryOp element) {
        super(element);
    }

    @Nonnull
    @Override
    protected ResolveCacheDependency getCacheDependency() {
        return ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE;
    }

    @Nonnull
    @Override
    protected List<RsElement> resolveInner() {
        Object operatorType = RsBinaryOpImplUtil.getOperatorType(getElement());
        if (!(operatorType instanceof OverloadableBinaryOperator)) return Collections.emptyList();
        OverloadableBinaryOperator operator = (OverloadableBinaryOperator) operatorType;
        return NameResolution.collectResolveVariants(operator.getFnName(), processor ->
            NameResolution.processBinaryOpVariants(getElement(), operator, processor)
        );
    }
}
