/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.stubs.RsFunctionStub;

/**
 * Bridge class that re-exports constants from {@link RsFunctionKt}.
 * <p>
 * Some callers may reference {@code RsFunctionExtUtil} directly.
 */
public final class RsFunctionExtUtil {
    private RsFunctionExtUtil() {
    }

    public static final StubbedAttributeProperty<RsFunction, RsFunctionStub> IS_PROC_MACRO_DEF_PROP =
        RsFunctionUtil.IS_PROC_MACRO_DEF_PROP;
}
