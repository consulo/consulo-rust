/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.NavigatablePsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.stubs.common.RsAttributeOwnerPsiOrStub;

import java.util.stream.Stream;

public interface RsDocAndAttributeOwner extends RsElement, NavigatablePsiElement, RsAttributeOwnerPsiOrStub<RsMetaItem> {
    @Override
    @NotNull
    default Stream<RsMetaItem> getRawMetaItems() {
        return RsInnerAttributeOwnerRegistry.rawMetaItems(this);
    }

    @NotNull
    default QueryAttributes<RsMetaItem> getQueryAttributes() {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(this);
    }
}
