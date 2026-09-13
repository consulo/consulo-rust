/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.impl.QueryAttributes;
import org.rust.lang.core.psi.ext.impl.RsInnerAttributeOwnerRegistry;


import consulo.language.psi.NavigatablePsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.stubs.common.RsAttributeOwnerPsiOrStub;

import java.util.stream.Stream;

public interface RsDocAndAttributeOwner extends RsElement, NavigatablePsiElement, RsAttributeOwnerPsiOrStub<RsMetaItem> {
    @Override
    @Nonnull
    default Stream<RsMetaItem> getRawMetaItems() {
        return RsInnerAttributeOwnerRegistry.rawMetaItems(this);
    }

    @Nonnull
    default QueryAttributes<RsMetaItem> getQueryAttributes() {
        return new QueryAttributes<>(RsPsiSupport.getInstance().rawMetaItems(this));
    }
}
