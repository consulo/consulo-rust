/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.ext.RsBindingModeKind.BindByReference;
import org.rust.lang.core.psi.ext.RsBindingModeKind.BindByValue;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtUnknown;
// import org.rust.lang.core.types.rawType; // wrong reference
import org.rust.lang.core.types.ty.*;
import org.rust.lang.core.types.ty.Mutability;
import org.rust.lang.utils.evaluation.ConstExpr;
import org.rust.openapiext.Testmark;
public final class PatternMatching {
    private PatternMatching() {}

}
