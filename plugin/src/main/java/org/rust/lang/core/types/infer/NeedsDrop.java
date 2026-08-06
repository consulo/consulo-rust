/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.*;
// import removed - use constant directly
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.Substitution;
// import org.rust.lang.core.types.rawType; // wrong reference
import org.rust.lang.core.types.ty.*;
import org.rust.lang.utils.evaluation.ThreeValuedLogic;
public final class NeedsDrop {
    private NeedsDrop() {}

}
