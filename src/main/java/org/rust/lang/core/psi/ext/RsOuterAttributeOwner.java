/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsOuterAttr;

import java.util.List;
import java.util.stream.Stream;

/**
 * An element with attached outer attributes and documentation comments.
 * Such elements should use left edge binder to properly wrap preceding comments.
 *
 * Fun fact: in Rust, documentation comments are a syntactic sugar for attribute syntax.
 *
 * <pre>
 * /// docs
 * fn foo() {}
 * </pre>
 *
 * is equivalent to
 *
 * <pre>
 * #[doc="docs"]
 * fn foo() {}
 * </pre>
 */
public interface RsOuterAttributeOwner extends RsDocAndAttributeOwner {
    @NotNull
    default List<RsOuterAttr> getOuterAttrList() {
        return PsiElementUtil.stubChildrenOfType(this, RsOuterAttr.class);
    }

    @NotNull
    @Override
    default Stream<RsMetaItem> getRawOuterMetaItems() {
        return getOuterAttrList().stream().map(RsOuterAttr::getMetaItem);
    }
}
