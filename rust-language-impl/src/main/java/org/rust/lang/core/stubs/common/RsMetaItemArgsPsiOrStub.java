/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs.common;

import java.util.List;

public interface RsMetaItemArgsPsiOrStub {
    List<? extends RsMetaItemPsiOrStub> getMetaItemList();
}
