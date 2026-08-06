/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs.common;

import org.rust.lang.core.crate.Crate;

import java.util.stream.Stream;

public interface RsAttributeOwnerPsiOrStub<T extends RsMetaItemPsiOrStub> {
    Stream<T> getRawMetaItems();

    default Stream<T> getRawOuterMetaItems() {
        return Stream.empty();
    }
}
